package alex.bobro.genericdao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.entities.FieldType;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.util.CollectionUtils;
import alex.bobro.genericdao.util.OutValue;
import alex.bobro.genericdao.util.QueryBuilder;
import alex.bobro.genericdao.util.Reflection;

@SuppressWarnings({"unused"})
public class GenericDaoHelper {

    public static Scheme getSchemeInstanceOrThrow(Class clazz) {
        Scheme scheme = Scheme.getSchemeInstance(clazz);
        if (scheme == null) throw new Error("Scheme hasn't been initialized yet!");

        return scheme;
    }

    @SuppressWarnings("unchecked")
    public static Object getValueForField(@NotNull Class genericDaoClass, @NotNull Object object, @NotNull Field field) {
        Object value;
        Reflection.Invoker getterInvoker = Reflection.invoker(false, getGetterName(field), genericDaoClass);

        if (getterInvoker != null && field.getType().equals(getterInvoker.getMethod().getReturnType())) {
            value = getterInvoker.invokeFor(object);

        } else {
            Reflection.Accessor fieldAccessor = Reflection.accessor(field);
            value = fieldAccessor.get(object);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static void setValueForField(@NotNull Class genericDaoClass, @NotNull Object object, @NotNull Field field, Object value) {
        if (!field.getDeclaringClass().isAssignableFrom(object.getClass())) {
            field = Scheme.getFieldByNameFrom(object.getClass(), field.getName());
        }
        Reflection.Invoker setterInvoker = Reflection.invoker(false, getSetterName(field), genericDaoClass, field.getType());

        if (setterInvoker != null) {
            setterInvoker.invokeFor(object, value);

        } else {
            Reflection.Accessor fieldAccessor = Reflection.accessor(field);
            fieldAccessor.set(object, value);
        }
    }


    private static String getGetterName(@NotNull Field field) {
        if (!field.getType().equals(boolean.class))
            return "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        else {
            if (field.getName().startsWith("is"))
                return field.getName();
            else
                return "is" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        }
    }

    private static String getSetterName(@NotNull Field field) {
        if (field.getType().equals(boolean.class) && field.getName().startsWith("is"))
            return field.getName().replace("is", "set");
        else
            return "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);

    }

    public static String arrayToQueryString(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (String s : array) {
            builder.append("'").append(s).append("'")
                    .append(",");
        }

        return builder.toString().substring(0, builder.length() - 1);
    }

    public static String arrayToString(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                builder.append(",");
            }
            builder.append(array[i]);
        }

        return builder.toString();
    }

    public static String collectionToString(Collection<String> collection) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String s : collection) {
            if (i != 0) {
                builder.append(",");
            }
            builder.append(s);
            i++;
        }

        return builder.toString();
    }

    public static String arrayToWhereString(@NotNull String... array) {
        if (array.length == 0)
            return null;

        StringBuilder builder = new StringBuilder(array[0]).append("=?");
        for (int i = 1; i < array.length; i++) {
            builder.append(" AND ")
                    .append(array[i])
                    .append("=?");
        }

        return builder.toString();
    }

    public static String arrayToInWhereString(@NotNull String... array) {
        if (array.length == 0)
            return null;

        StringBuilder builder = new StringBuilder(array[0]).append("IN (?)");
        for (int i = 1; i < array.length; i++) {
            builder.append(" AND ")
                    .append(array[i])
                    .append("IN (?)");
        }

        return builder.toString();
    }

    private static final String SEPARATOR = "$$$";

    public static String toKeyValue(Object value) {
        Scheme scheme = getSchemeInstanceOrThrow(value.getClass());
        return toKeyValue(scheme, value);
    }

    public static String toKeyValue(Scheme scheme, Object value) {
        if(TextUtils.isEmpty(scheme.getKeyField())) return null;


        String[] keyFields = new String[]{scheme.getKeyField()};
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < keyFields.length; i++) {
            String keyField = keyFields[i];
            if (i != 0) {
                builder.append(SEPARATOR);
            }
            builder.append(getValueForField(value.getClass(), value, scheme.getAnnotatedFields().get(keyField).getConnectedField()));
        }

        return builder.toString();
    }

    public static String[] fromKeyValue(String keyValue) {
        return keyValue.split(SEPARATOR);
    }


    @SuppressWarnings("unchecked")
    public static <DbEntity> void fillCvFromEntity(Scheme scheme, ContentValues contentValues, @NotNull DbEntity dbEntity, ArrayList<GenericContentProviderOperation> operations, RequestParameters requestParameters) {
        if (requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }

        Class objectClass = dbEntity.getClass();
        contentValues.put(Scheme.COLUMN_OBJECT_CLASS_NAME, objectClass.getName());

        for (String fieldKey : scheme.getAnnotatedFields().keySet()) {
            Column column = scheme.getAnnotatedFields().get(fieldKey);
            if (scheme.getAllFields().size() > 1 && !CollectionUtils.contains(scheme.getAllFields().get(objectClass), column.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }
            if (RelationType.ONE_TO_MANY.equals(column.getRelationType()) || RelationType.MANY_TO_MANY.equals(column.getRelationType())) {
                continue;
            }

            if (TextUtils.equals(scheme.getKeyField(), column.getName()) && scheme.isAutoincrement())
                continue;

            try {
                operations.addAll(checkValueAndPutIntoCV(contentValues, column, scheme, dbEntity, requestParameters));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <DbEntity> DbEntity fromCursor(Scheme scheme, Cursor cursor, Class<DbEntity> dbEntityClass) {
        return fromCursor(scheme, cursor, dbEntityClass, null);
    }


    @SuppressWarnings("unchecked")
    public static <DbEntity> DbEntity fromCursor(Scheme scheme, Cursor cursor, Class<DbEntity> dbEntityClass, Column parentColumn) {
        if (cursor == null)
            return null;

        String className = cursor.getString(cursor.getColumnIndex(GenericDaoHelper.getColumnNameFrom(Scheme.COLUMN_OBJECT_CLASS_NAME, parentColumn)));
        if (TextUtils.isEmpty(className))
            return null;

        Class objectClass;
        try {
            objectClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }

        Reflection.Creator creator = Reflection.creator(objectClass);
        DbEntity entity = (DbEntity) creator.newInstanceFor();
        if (entity != null) fillEntityWithValues(entity, objectClass, cursor, scheme, parentColumn);
        return entity;
    }

    private static <DbEntity> void fillEntityWithValues(DbEntity entity, Class<DbEntity> objectClass, Cursor cursor, Scheme scheme, Column parentColumn) {
        for (Column column : scheme.getAnnotatedFields().values()) {
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), column.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }

            int index = cursor.getColumnIndex(GenericDaoHelper.getColumnNameFrom(column.getName(), parentColumn));
            if (cursor.isNull(index)) {
                continue;
            }

            if (RelationType.ONE_TO_MANY.equals(column.getRelationType()) || RelationType.MANY_TO_MANY.equals(column.getRelationType()))
                continue;


            Object valueForField = GenericDaoHelper.getValueForFieldFromCursor(column, cursor, index, RelationType.MANY_TO_ONE.equals(column.getRelationType()) ? column : null);
            GenericDaoHelper.setValueForField(objectClass, entity, column.getConnectedField(), valueForField);
        }
    }


    public static <T> int indexOf(List<T> list, int startIndex, Object object) {
        int s = list.size();
        if (object != null) {
            for (int i = startIndex; i < s; i++) {
                if (object.equals(list.get(i))) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex; i < s; i++) {
                if (list.get(i) == null) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings({"all", "unchecked"})
    private static ArrayList<GenericContentProviderOperation> checkValueAndPutIntoCV(@NotNull ContentValues cv, @NotNull Column column, @NotNull Scheme scheme, Object object, RequestParameters requestParameters) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ArrayList<GenericContentProviderOperation> contentProviderOperationList = new ArrayList<>();
        if (object == null)
            return contentProviderOperationList;

        Field field = column.getConnectedField();
        if (scheme.getAllFields().size() > 1 && !field.getDeclaringClass().isAssignableFrom(object.getClass())) {
            field = Scheme.getFieldByNameFrom(object.getClass(), field.getName());
        }

        Object fieldValue = GenericDaoHelper.getValueForField(object.getClass(), object, field);
        if (fieldValue == null)
            return contentProviderOperationList;

        FieldType fieldType = FieldType.findByTypeClass(field.getType());
        if (fieldType == null)
            return contentProviderOperationList;

        String name = (TextUtils.isEmpty(column.getName())) ? field.getName() : column.getName();

        switch (fieldType) {
            case STRING:
                cv.put(name, (String) fieldValue);
                break;

            case LONG:
                cv.put(name, (Long) fieldValue);
                break;

            case INTEGER:
                cv.put(name, (Integer) fieldValue);
                break;

            case SHORT:
                cv.put(name, (Short) fieldValue);
                break;

            case BYTE:
                cv.put(name, (Byte) fieldValue);
                break;

            case DOUBLE:
                cv.put(name, (Double) fieldValue);
                break;

            case FLOAT:
                cv.put(name, (Float) fieldValue);
                break;

            case BOOLEAN:
                cv.put(name, (Boolean) fieldValue);
                break;

            case BLOB:
                cv.put(name, (byte[]) fieldValue);
                break;

            case STRING_ARRAY:
                cv.put(name, GenericDaoHelper.arrayToString((String[]) fieldValue));
                break;

            case DATE:
                cv.put(name, ((Date)fieldValue).getTime());
                break;

            case OBJECT:
                putObjectIntoCv(column, cv, fieldValue, requestParameters, field, name, contentProviderOperationList);
                break;
        }

        return contentProviderOperationList;
    }

    @SuppressWarnings("unchecked")
    private static void putObjectIntoCv(Column column, ContentValues cv, Object fieldValue, RequestParameters requestParameters, Field field, String name,
                                        List<GenericContentProviderOperation> contentProviderOperationList) {
        if (RelationType.MANY_TO_ONE.equals(column.getRelationType())) {
            Scheme fieldScheme = Scheme.getSchemeInstance(fieldValue.getClass());
            if(requestParameters.isManyToOneGotWithParent()) contentProviderOperationList.addAll(getContentProviderOperationBatch(fieldScheme, fieldValue, null, null, requestParameters));
            cv.put(column.getName(), GenericDaoHelper.toKeyValue(column.getParentScheme(), fieldValue));
        } else if (fieldValue instanceof List) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            FieldType type = FieldType.findByTypeClass((Class<?>) listType.getActualTypeArguments()[0]);

            switch (type) {
                case STRING:
                    List<String> list = (List<String>) fieldValue;
                    cv.put(name, GenericDaoHelper.collectionToString(list));
                    break;
            }

        }
    }


    public static Object getValueForFieldFromCursor(Column column, Cursor cursor, int index,  Column parentColumn) {
        Field field = column.getConnectedField();
        FieldType fieldType = FieldType.findByTypeClass(field.getType());

        Object valueForField = null;
        switch (fieldType) {
            case STRING:
                valueForField = cursor.getString(index);
                break;

            case LONG:
                valueForField = cursor.getLong(index);
                break;

            case INTEGER:
                valueForField = cursor.getInt(index);
                break;

            case SHORT:
                valueForField = cursor.getShort(index);
                break;

            case BYTE:
                valueForField = ((Short) cursor.getShort(index)).byteValue();
                break;

            case DOUBLE:
                valueForField = cursor.getDouble(index);
                break;

            case FLOAT:
                valueForField = cursor.getFloat(index);
                break;

            case BOOLEAN:
                valueForField = cursor.getInt(index) == 1;
                break;

            case BLOB:
                valueForField = cursor.getBlob(index);
                break;

            case STRING_ARRAY:
                valueForField = cursor.getString(index).split(",");
                break;

            case DATE:
                valueForField = new Date(cursor.getLong(index));
                break;

            case OBJECT:
                valueForField = getValueFromFieldForObject(column, cursor, index, parentColumn);
                break;
        }

        return valueForField;
    }

    private static Object getValueFromFieldForObject(Column column, Cursor cursor, int index, Column parentColumn) {
        Field field = column.getConnectedField();
        if (List.class.isAssignableFrom(field.getType())) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            FieldType type = FieldType.findByTypeClass((Class<?>) listType.getActualTypeArguments()[0]);

            switch (type) {
                case STRING:
                    return Arrays.asList(cursor.getString(index).split(","));
            }

        } else {
            return GenericDaoHelper.fromCursor(column.getScheme(), cursor, field.getType(), parentColumn);
        }

        return null;
    }

    static <DbEntity> ArrayList<GenericContentProviderOperation> getContentProviderOperationBatch(Scheme scheme, DbEntity dbEntity, ContentValues contentValues, QueryParameters insertParams, RequestParameters requestParameters) {
        if (requestParameters == null) requestParameters = new RequestParameters.Builder().build();
        if (contentValues == null) contentValues = new ContentValues();

        ArrayList<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();

        Object keyValue = GenericDaoHelper.toKeyValue(scheme, dbEntity);
        if (keyValue == null && scheme.hasNestedObjects()) throw new Error("Key value can't be null for object with connections");

        if (!RequestParameters.RequestMode.JUST_NESTED.equals(requestParameters.getRequestMode())) {
            GenericContentProviderOperation.Builder builder = GenericContentProviderOperation.newInsert();
            GenericDaoHelper.fillCvFromEntity(scheme, contentValues, dbEntity, contentProviderOperations, requestParameters);
            builder.withContentValues(contentValues)
                    .withQueryParameters(insertParams)
                    .withTable(scheme.getName());
            contentProviderOperations.add(builder.build());
        }

        if (!RequestParameters.RequestMode.JUST_PARENT.equals(requestParameters.getRequestMode()) && scheme.hasNestedObjects()) {
            contentProviderOperations.addAll(getNestedContentProviderOperationBatch(scheme, dbEntity, requestParameters));
        }
        return contentProviderOperations;
    }

    static <DbEntity> void fillBatchWithOneToManyFields(DbEntity dbEntity, Scheme scheme, Class<DbEntity> objectClass, String keyValue, RequestParameters nestedParameters,
                                                        List<GenericContentProviderOperation> contentProviderOperations) {
        for (String oneToManyField : scheme.getOneToManyFields()) {
            Column oneToManyColumn = scheme.getAnnotatedFields().get(oneToManyField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), oneToManyColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }
            Object value = GenericDaoHelper.getValueForField(objectClass, dbEntity, oneToManyColumn.getConnectedField());

            if (value == null)
                continue;

            Scheme oneToManyScheme = oneToManyColumn.getScheme();
            //noinspection unchecked
            List list = new ArrayList((List) value);
            for (Object object : list) {
                ContentValues contentValues = new ContentValues();
                if (!scheme.getAnnotatedFields().containsKey(GenericDaoHelper.getColumnNameFromTable(scheme.getName()))) {
                    contentValues.put(GenericDaoHelper.getColumnNameFromTable(scheme.getName()), keyValue);
                }
                //noinspection unchecked
                contentProviderOperations.addAll(getContentProviderOperationBatch(oneToManyScheme, object, contentValues, null, nestedParameters));
            }
        }
    }

    static <DbEntity> void fillBatchWithManyToOneFields(DbEntity dbEntity, Scheme scheme, Class<DbEntity> objectClass, String keyValue, RequestParameters nestedParameters,
                                                        List<GenericContentProviderOperation> contentProviderOperations) {
        for (String manyToOneField : scheme.getManyToOneFields()) {
            Column oneToManyColumn = scheme.getAnnotatedFields().get(manyToOneField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), oneToManyColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }
            Object value = GenericDaoHelper.getValueForField(objectClass, dbEntity, oneToManyColumn.getConnectedField());

            if (value == null)
                continue;

            Scheme manyToOneScheme = oneToManyColumn.getScheme();
            contentProviderOperations.addAll(getContentProviderOperationBatch(manyToOneScheme, value, null, null, null));
        }
    }

    static <DbEntity> void fillBatchWithManyToManyFields(DbEntity dbEntity, Scheme scheme, Class<DbEntity> objectClass, String keyValue, RequestParameters nestedParameters,
                                                         List<GenericContentProviderOperation> contentProviderOperations) {
        for (String manyToManyField : scheme.getManyToManyFields()) {
            Column manyToManyColumn = scheme.getAnnotatedFields().get(manyToManyField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), manyToManyColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }
            Object value = GenericDaoHelper.getValueForField(objectClass, dbEntity, manyToManyColumn.getConnectedField());

            if (value == null)
                continue;

            Scheme manyToManyScheme = manyToManyColumn.getScheme();
            List list = (List) value;
            for (Object object : list) {
                //noinspection unchecked
                contentProviderOperations.addAll(getContentProviderOperationBatch(manyToManyScheme,object, null, null, nestedParameters));

                ContentValues manyToManyCV = new ContentValues();
                manyToManyCV.put(GenericDaoHelper.getColumnNameFromTable(scheme.getName()), keyValue);
                manyToManyCV.put(GenericDaoHelper.getColumnNameFromTable(Scheme.getToManyClassName(manyToManyColumn)), GenericDaoHelper.toKeyValue(manyToManyScheme, object));

                QueryParameters.Builder queryBuilder = new QueryParameters.Builder();
                queryBuilder.addParameter(GenericDaoContentProvider.CONFLICT_ALGORITHM, String.valueOf(SQLiteDatabase.CONFLICT_IGNORE));

                GenericContentProviderOperation.Builder builder = GenericContentProviderOperation.newInsert();
                builder.withContentValues(manyToManyCV)
                        .withQueryParameters(queryBuilder.build())
                        .withTable(scheme.getManyToManyTableName(manyToManyColumn));
                contentProviderOperations.add(builder.build());
            }
        }
    }

    static <DbEntity> List<GenericContentProviderOperation> getNestedContentProviderOperationBatch(Scheme scheme, DbEntity dbEntity, RequestParameters requestParameters) {
        List<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();
        if (dbEntity == null) return contentProviderOperations;

        Class objectClass = dbEntity.getClass();
        RequestParameters nestedParameters = new RequestParameters.Builder().withRequestMode(RequestParameters.RequestMode.FULL).build();

        String keyValue = GenericDaoHelper.toKeyValue(scheme, dbEntity);
        if (TextUtils.isEmpty(keyValue))
            return contentProviderOperations;

        if(!requestParameters.isManyToOneGotWithParent()) fillBatchWithManyToOneFields(dbEntity, scheme, objectClass, keyValue, nestedParameters, contentProviderOperations);
        fillBatchWithOneToManyFields(dbEntity, scheme, objectClass, keyValue, nestedParameters, contentProviderOperations);
        fillBatchWithManyToManyFields(dbEntity, scheme, objectClass, keyValue, nestedParameters, contentProviderOperations);

        return contentProviderOperations;
    }

    public static String getColumnNameFromTable(String table) {
        return table + "_id";
    }

    public static Class getParametrizedType(Class classWithParameter) {
        ParameterizedType parameterizedType = (ParameterizedType) classWithParameter.getGenericSuperclass();
        return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }

    public static boolean isColumnExists(SQLiteDatabase database, String tableName, String columnName) {
        Cursor cursor = database.rawQuery(String.format("SELECT * FROM %s LIMIT 0", tableName), null);
        if (cursor == null)
            return false;

        try {
            return cursor.getColumnIndex(columnName) != -1;
        } finally {
            cursor.close();
        }
    }

    public static List<String> getColumnsFromTable(SQLiteDatabase database, String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = database.rawQuery(String.format("PRAGMA table_info(%s)", tableName), null);

        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1));
        }
        cursor.close();

        return columns;
    }

    public static boolean isTableExists(SQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", tableName), null);
        if (cursor == null)
            return false;

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static String generateInsertQuery(String tableName, ContentValues contentValues) {
        QueryBuilder stringBuilder = new QueryBuilder("INSERT INTO ")
                .appendQuoted(tableName).appendSpace().append("(");

        List<String> columnNames = new ArrayList<>();
        List<String> bindings = new ArrayList<>();
        for (String key : contentValues.keySet()) {
            columnNames.add(key);
            bindings.add("?");
        }

        stringBuilder.appendQuotedList(columnNames).append(") VALUES (");
        stringBuilder.appendList(bindings).append(")");
        return stringBuilder.toString();
    }

    public static final String COLUMN_NAME_SEPARATOR = "_";

    public static String getColumnNameFrom(String name, Column parentColumn) {
        return parentColumn == null ? name : parentColumn.getName() + COLUMN_NAME_SEPARATOR + name;
    }

    public static String getColumnNameFrom(String name, Column parentColumn, String separator) {
        return parentColumn == null ? name : parentColumn.getName() + separator + name;
    }

}
