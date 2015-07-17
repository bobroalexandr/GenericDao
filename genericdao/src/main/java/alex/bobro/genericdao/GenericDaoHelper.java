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
import java.util.concurrent.TimeUnit;

import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.entities.FieldType;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.util.CollectionUtils;
import alex.bobro.genericdao.util.OutValue;
import alex.bobro.genericdao.util.QueryBuilder;
import alex.bobro.genericdao.util.Reflection;
import alex.bobro.genericdao.util.TimeLogger;

@SuppressWarnings({"unused"})
public class GenericDaoHelper {

    public static Scheme getSchemeInstanceOrThrow(Class clazz) {
        Scheme scheme = Scheme.getSchemeInstance(clazz);
        if (scheme == null) throw new Error("Scheme hasn't been initialized yet!");

        return scheme;
    }

    @SuppressWarnings("unchecked")
    public static Object getValueForField(@NotNull Scheme scheme, @NotNull Class genericDaoClass, @NotNull Object object, @NotNull Field field) {
        Reflection.Accessor fieldAccessor = scheme.getAccessorsMap().get(genericDaoClass).get(field.getName());
        return fieldAccessor.get(object);
    }

    @SuppressWarnings("unchecked")
    public static void setValueForField(@NotNull Scheme scheme, @NotNull Class genericDaoClass, @NotNull Object object, @NotNull Field field, Object value) {
        Reflection.Accessor fieldAccessor = scheme.getAccessorsMap().get(genericDaoClass).get(field.getName());
        fieldAccessor.set(object, value);
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
        if (TextUtils.isEmpty(scheme.getKeyField())) return null;


        String[] keyFields = new String[]{scheme.getKeyField()};
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < keyFields.length; i++) {
            String keyField = keyFields[i];
            if (i != 0) {
                builder.append(SEPARATOR);
            }
            builder.append(getValueForField(scheme, value.getClass(), value, scheme.getAnnotatedFields().get(keyField).getConnectedField()));
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
        return fromCursor(scheme, cursor, dbEntityClass, new OutValue<>(0));
    }


    @SuppressWarnings("unchecked")
    public static <DbEntity> DbEntity fromCursor(Scheme scheme, Cursor cursor, Class<DbEntity> dbEntityClass, OutValue<Integer> objectIndex) {
        if (cursor == null || objectIndex.value >= cursor.getColumnCount())
            return null;

//        TimeLogger.startLogging(GenericDaoHelper.class, TimeUnit.NANOSECONDS);
        String className = cursor.getString(objectIndex.value);
        if (TextUtils.isEmpty(className))
            return null;

//        TimeLogger.logTime(GenericDaoHelper.class, "step 1");
        Reflection.Creator creator = scheme.getCreatorMap().get(className);
        Object object = creator.newInstanceFor();
        DbEntity entity = dbEntityClass.cast(object);
//        TimeLogger.logTime(GenericDaoHelper.class, "step 2");
        if (entity != null)
            fillEntityWithValues(entity, creator.getConstructor().getDeclaringClass(), cursor, scheme, objectIndex);
//        TimeLogger.logTime(GenericDaoHelper.class, "step 3");
//        TimeLogger.stopLogging(GenericDaoHelper.class);
        return entity;
    }

    private static <DbEntity> void fillEntityWithValues(DbEntity entity, Class<DbEntity> objectClass, Cursor cursor, Scheme scheme, OutValue<Integer> objectIndex) {
        List<String> columns = Arrays.asList(cursor.getColumnNames());
        int objectStartIndex = objectIndex.value;
        int objectFinishIndex = indexOf(columns, objectIndex.value + 1, Scheme.COLUMN_OBJECT_CLASS_NAME);
        if (objectFinishIndex == -1)
            objectFinishIndex = columns.size();

        objectIndex.value = objectFinishIndex;

        for (int i = objectStartIndex + 1; i < objectFinishIndex; i++) {
//            TimeLogger.startLogging(TimeLogger.class, TimeUnit.NANOSECONDS);
            if (i >= columns.size()) return;
            Column column = scheme.getAnnotatedFields().get(columns.get(i));
            if (column == null) continue;
            if (scheme.getAccessorsMap().get(objectClass).get(column.getConnectedField().getName()) == null) {
                continue;
            }

            if (cursor.isNull(i)) {
                if (RelationType.MANY_TO_ONE.equals(column.getRelationType())) {
                    Scheme manyToOneScheme = column.getScheme();
                    for (int k = 0; k < manyToOneScheme.getManyToOneFields().size() + 1; k++) {
                        objectIndex.value = indexOf(columns, objectIndex.value + 1, Scheme.COLUMN_OBJECT_CLASS_NAME);
                    }
                }
                continue;
            }

            if (RelationType.ONE_TO_MANY.equals(column.getRelationType()) || RelationType.MANY_TO_MANY.equals(column.getRelationType()))
                continue;

//            TimeLogger.logTime(TimeLogger.class, "peref");
            Object valueForField = GenericDaoHelper.getValueForFieldFromCursor(column, cursor, RelationType.MANY_TO_ONE.equals(column.getRelationType()) ? objectIndex : new OutValue<>(i));
//            TimeLogger.logTime(TimeLogger.class, "get");
            GenericDaoHelper.setValueForField(scheme, objectClass, entity, column.getConnectedField(), valueForField);
//            TimeLogger.logTime(TimeLogger.class, "set");
//            TimeLogger.stopLogging(TimeLogger.class);
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

        Object fieldValue = GenericDaoHelper.getValueForField(scheme, object.getClass(), object, field);
        if (fieldValue == null)
            return contentProviderOperationList;

        FieldType fieldType = FieldType.getTypeByClass(field.getType());
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
                cv.put(name, ((Date) fieldValue).getTime());
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
            if (requestParameters.isManyToOneGotWithParent())
                contentProviderOperationList.addAll(getContentProviderOperationBatch(fieldScheme, fieldValue, null, null, requestParameters));
            cv.put(column.getName(), GenericDaoHelper.toKeyValue(column.getParentScheme(), fieldValue));
        } else if (fieldValue instanceof List) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            FieldType type = FieldType.getTypeByClass((Class<?>) listType.getActualTypeArguments()[0]);

            switch (type) {
                case STRING:
                    List<String> list = (List<String>) fieldValue;
                    cv.put(name, GenericDaoHelper.collectionToString(list));
                    break;
            }

        }
    }


    public static Object getValueForFieldFromCursor(Column column, Cursor cursor, OutValue<Integer> objectIndex) {
        Field field = column.getConnectedField();
        FieldType fieldType = FieldType.getTypeByClass(field.getType());

        Object valueForField = null;
        int index = objectIndex.value;
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
                valueForField = getValueFromFieldForObject(column, cursor, index, objectIndex);
                break;
        }

        return valueForField;
    }

    private static Object getValueFromFieldForObject(Column column, Cursor cursor, int index, OutValue<Integer> objectIndex) {
        Field field = column.getConnectedField();
        if (List.class.isAssignableFrom(field.getType())) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            FieldType type = FieldType.getTypeByClass((Class<?>) listType.getActualTypeArguments()[0]);

            switch (type) {
                case STRING:
                    return Arrays.asList(cursor.getString(index).split(","));
            }

        } else {
            return GenericDaoHelper.fromCursor(column.getScheme(), cursor, field.getType(), objectIndex);
        }

        return null;
    }

    static <DbEntity> ArrayList<GenericContentProviderOperation> getContentProviderOperationBatch(Scheme scheme, DbEntity dbEntity, ContentValues contentValues, QueryParameters insertParams, RequestParameters requestParameters) {
        if (requestParameters == null) requestParameters = new RequestParameters.Builder().build();
        if (contentValues == null) contentValues = new ContentValues();

        ArrayList<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();

        Object keyValue = GenericDaoHelper.toKeyValue(scheme, dbEntity);
        if (keyValue == null && scheme.hasNestedObjects())
            throw new Error("Key value can't be null for object with connections");

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
            Object value = GenericDaoHelper.getValueForField(scheme, objectClass, dbEntity, oneToManyColumn.getConnectedField());

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
            Object value = GenericDaoHelper.getValueForField(scheme, objectClass, dbEntity, oneToManyColumn.getConnectedField());

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
            Object value = GenericDaoHelper.getValueForField(scheme, objectClass, dbEntity, manyToManyColumn.getConnectedField());

            if (value == null)
                continue;

            Scheme manyToManyScheme = manyToManyColumn.getScheme();
            List list = (List) value;
            for (Object object : list) {
                //noinspection unchecked
                contentProviderOperations.addAll(getContentProviderOperationBatch(manyToManyScheme, object, null, null, nestedParameters));

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

        if (!requestParameters.isManyToOneGotWithParent())
            fillBatchWithManyToOneFields(dbEntity, scheme, objectClass, keyValue, nestedParameters, contentProviderOperations);
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

}
