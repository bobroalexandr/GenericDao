package alex.bobro.genericdao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.entities.FieldType;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.util.CollectionUtils;
import alex.bobro.genericdao.util.OutValue;

/**
 * Created by alex on 25.11.14.
 */
public class GenericDaoHelper {

    public static <T> Object getValueForField(@NotNull Class<T> genericDaoClass, @NotNull Object object, @NotNull Field field) throws InvocationTargetException, IllegalAccessException {
        Object value;
        Method getter = null;

        try {
            getter = genericDaoClass.getMethod(getGetterName(field));
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
        }

        if (getter != null && field.getType().equals(getter.getReturnType())) {
            value = getter.invoke(object);

        } else {
            if (!field.isAccessible())
                field.setAccessible(true);

            value = field.get(object);
        }

        return value;
    }

    public static <T> void setValueForField(@NotNull Class<T> genericDaoClass, @NotNull Object object, @NotNull Field field, Object value) throws InvocationTargetException, IllegalAccessException {
        if(!field.getDeclaringClass().isAssignableFrom(object.getClass())) {
            field = Scheme.getFieldByNameFrom(object.getClass(), field.getName());
        }
        Method fieldSetter = null;

        try {
            fieldSetter = genericDaoClass.getMethod(getSetterName(field), field.getType());
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
        }

        if (fieldSetter != null) {
            fieldSetter.invoke(object, value);

        } else {
            if (!field.isAccessible())
                field.setAccessible(true);

            field.set(object, value);
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
        Scheme scheme = Scheme.getSchemeInstance(value.getClass());

        String[] keyFields = new String[]{scheme.getKeyField()};
        if (keyFields.length == 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        try {
            for (int i = 0; i < keyFields.length; i++) {
                String keyField = keyFields[i];
                if (i != 0) {
                    builder.append(SEPARATOR);
                }
                builder.append(getValueForField(value.getClass(), value, scheme.getAnnotatedFields().get(keyField).getConnectedField()));

            }
        } catch (Exception e) {
            Log.e("test!","scheme = " + scheme.getName());
            e.printStackTrace();
        }


        return builder.toString();
    }

    public static String[] fromKeyValue(String keyValue) {
        return keyValue.split(SEPARATOR);
    }


    @SuppressWarnings("unchecked")
    public static <DbEntity> ContentValues toCv(@NotNull DbEntity dbEntity, Map<String, String> additionalCv, ArrayList<GenericContentProviderOperation> operations, RequestParameters requestParameters) {
        if(requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }

        Class objectClass = dbEntity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);

        ContentValues cv = new ContentValues();
        cv.put(Scheme.COLUMN_OBJECT_CLASS_NAME, dbEntity.getClass().getName());
        if (additionalCv != null) {
            for (String key : additionalCv.keySet()) {
                cv.put(key, additionalCv.get(key));
            }
        }

        for (String fieldKey : scheme.getAnnotatedFields().keySet()) {
            Column column = scheme.getAnnotatedFields().get(fieldKey);
            if(!CollectionUtils.contains(scheme.getAllFields().get(objectClass), column.getConnectedField(), Scheme.fieldNameComparator)) {
                continue;
            }
            if (RelationType.ONE_TO_MANY.equals(column.getRelationType()) || RelationType.MANY_TO_MANY.equals(column.getRelationType())) {
                continue;
            }

            if (Arrays.asList(scheme.getKeyField()).contains(column.getName()) && scheme.isAutoincrement())
                continue;

            try {
                operations.addAll(checkValueAndPutIntoCV(cv, column, scheme, dbEntity, requestParameters));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return cv;
    }

    @SuppressWarnings("unchecked")
    public static <DbEntity> DbEntity fromCursor(Cursor cursor, Class<DbEntity> dbEntityClass) {
        return fromCursor(cursor, dbEntityClass, new OutValue<>(0));
    }


    @SuppressWarnings("unchecked")
    public static <DbEntity> DbEntity fromCursor(Cursor cursor, Class<DbEntity> dbEntityClass, OutValue<Integer> objectIndex) {
        if (cursor == null)
            return null;

        List<String> columns = Arrays.asList(cursor.getColumnNames());

        try {
            int objectStartIndex = objectIndex.value;
            int objectFinishIndex = indexOf(columns, objectIndex.value + 1, Scheme.COLUMN_OBJECT_CLASS_NAME);
            if(objectFinishIndex == -1)
                objectFinishIndex = columns.size();

            String className = cursor.getString(objectIndex.value);
            if(TextUtils.isEmpty(className))
                return null;

            Class objectClass = Class.forName(className);
            objectIndex.value = objectFinishIndex;

            Scheme scheme = Scheme.getSchemeInstance(objectClass);
            DbEntity entity = dbEntityClass.cast(objectClass.newInstance());

            for (int i = objectStartIndex + 1; i < objectFinishIndex; i++) {
                Column column = scheme.getAnnotatedFields().get(columns.get(i));
                if(column == null)
                    continue;

                if (cursor.isNull(i)) {
                    if(RelationType.MANY_TO_ONE.equals(column.getRelationType())) {
                        Scheme manyToOneScheme = Scheme.getSchemeInstance(column.getConnectedField().getType());
                        for(int k = 0; k < manyToOneScheme.getManyToOneFields().size() + 1; k++) {
                            objectIndex.value = indexOf(columns, objectIndex.value + 1, Scheme.COLUMN_OBJECT_CLASS_NAME);
                        }
                    }
                    continue;
                }

                if (RelationType.ONE_TO_MANY.equals(column.getRelationType()) || RelationType.MANY_TO_MANY.equals(column.getRelationType()))
                    continue;

                try {
                    Object valueForField = GenericDaoHelper.getValueForFieldFromCursor(column, cursor, RelationType.MANY_TO_ONE.equals(column.getRelationType()) ? objectIndex : new OutValue<>(i));
                    GenericDaoHelper.setValueForField(objectClass, entity, column.getConnectedField(), valueForField);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            return entity;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

    private static ArrayList<GenericContentProviderOperation> checkValueAndPutIntoCV(@NotNull ContentValues cv, @NotNull Column column, @NotNull Scheme scheme, Object object, RequestParameters requestParameters) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ArrayList<GenericContentProviderOperation> contentProviderOperationList = new ArrayList<>();
        if (object == null)
            return contentProviderOperationList;

        Field field = column.getConnectedField();
        if(!field.getDeclaringClass().isAssignableFrom(object.getClass())) {
            field = Scheme.getFieldByNameFrom(object.getClass(), field.getName());
        }
        Object fieldValue = GenericDaoHelper.getValueForField(object.getClass(), object, field);

        if (fieldValue == null && scheme.getKeyField() != null && Arrays.asList(scheme.getKeyField()).contains(field.getName()))
            fieldValue = "";

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

            case OBJECT:
                if(RelationType.MANY_TO_ONE.equals(column.getRelationType())) {
                    cv.put(column.getName(), GenericDaoHelper.toKeyValue(fieldValue));
                    if(!RequestParameters.SavingMode.JUST_PARENT.equals(requestParameters.getSavingMode())) {
                        contentProviderOperationList.addAll(GenericDao.getInstance().getContentProviderOperationBatch(fieldValue, null, null, null));
                    }
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
                break;
        }

        return contentProviderOperationList;
    }


    public static Object getValueForFieldFromCursor(Column column, Cursor cursor, OutValue<Integer> objectIndex) {
        Field field = column.getConnectedField();
        FieldType fieldType = FieldType.findByTypeClass(field.getType());

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

            case OBJECT:
                if (List.class.isAssignableFrom(field.getType())) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    FieldType type = FieldType.findByTypeClass((Class<?>) listType.getActualTypeArguments()[0]);

                    switch (type) {
                        case STRING:
                            valueForField = Arrays.asList(cursor.getString(index).split(","));
                            break;
                    }

                } else {
                    valueForField = GenericDaoHelper.fromCursor(cursor, field.getType(), objectIndex);
                }
                break;
        }

        return valueForField;
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

}
