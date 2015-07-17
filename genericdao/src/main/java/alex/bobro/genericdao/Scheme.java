package alex.bobro.genericdao;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;
import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.entities.FieldType;
import alex.bobro.genericdao.entities.ForeignKeyActions;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.entities.SQLiteType;
import alex.bobro.genericdao.util.CollectionUtils;
import alex.bobro.genericdao.util.OutValue;
import alex.bobro.genericdao.util.Reflection;

public class Scheme {

    private static final String COLUMN_ID_NAME = "_id";
    public static final String COLUMN_OBJECT_CLASS_NAME = "object_class_name";

    public static final Column COLUMN_OBJECT_CLASS = new Column(COLUMN_OBJECT_CLASS_NAME, SQLiteType.TEXT, null, RelationType.NONE, ForeignKeyActions.CASCADE);
//    public static final Column _ID = new Column(COLUMN_ID_NAME, SQLiteType.INTEGER, "PRIMARY KEY AUTOINCREMENT");

    private static class SchemeInstancesHolder {
        public static final Map<String, Scheme> INSTANCES
                = Collections.synchronizedMap(new WeakHashMap<String, Scheme>());
    }

    public static synchronized Scheme getSchemeInstance(@NotNull Class<?> genericDaoClass) {
        if (genericDaoClass.isAnnotationPresent(TableAnnotation.class)) {
            TableAnnotation annotation = genericDaoClass.getAnnotation(TableAnnotation.class);
            return getSchemeInstance(annotation.tableName());
        }

        return null;
    }

    public static synchronized Scheme getSchemeInstance(@NotNull String tableName) {
        return SchemeInstancesHolder.INSTANCES.get(tableName);
    }

    public static Map<String, Scheme> getInstances() {
        return SchemeInstancesHolder.INSTANCES;
    }

    public static synchronized void init(Class... classes) {
        for (Class objectClass : classes) {
            if(objectClass.isAnnotationPresent(TableAnnotation.class)) {
                TableAnnotation annotation = (TableAnnotation) objectClass.getAnnotation(TableAnnotation.class);
                String tableName = annotation.tableName();

                Scheme scheme = SchemeInstancesHolder.INSTANCES.get(tableName);
                if(scheme == null) {
                    SchemeInstancesHolder.INSTANCES.put(tableName, scheme = new Scheme(objectClass));
                }
                scheme.addAllFieldsFrom(objectClass);
                scheme.addClass(objectClass);
            }
        }

        for (String key : SchemeInstancesHolder.INSTANCES.keySet()) {
            SchemeInstancesHolder.INSTANCES.get(key).fillColumnsMap();
        }
    }


    final String name;
    final Set<Class> modelClasses;
    private Map<Class, List<Field>> allFields;
    private Map<String, Column> annotatedFields;

    private String keyField;
    private boolean autoincrement;

    private List<String> manyToManyFields;
    private List<String> oneToManyFields;
    private List<String> manyToOneFields;

    private Set<Scheme> foreignSchemes;

    private Map<String, Reflection.Creator> creatorMap;
    private Map<Class, HashMap<String,Reflection.Accessor>> accessorsMap;
    private Map<Class, FieldType> fieldTypeMap;

    private Scheme(Class<?> modelClass) throws ClassCastException, IllegalArgumentException {
        modelClasses = new HashSet<>();
        TableAnnotation table = modelClass.getAnnotation(TableAnnotation.class);
        if (table == null) {
            throw new IllegalArgumentException("Scheme candidate class must use Table annotation");
        }

        manyToManyFields = new ArrayList<>();
        oneToManyFields = new ArrayList<>();
        manyToOneFields = new ArrayList<>();

        foreignSchemes = new HashSet<>();

        annotatedFields = new LinkedHashMap<>();
        allFields = new HashMap<>();

        creatorMap = new HashMap<>();
        accessorsMap = new HashMap<>();
        fieldTypeMap = new HashMap<>();

        autoincrement = table.autoincrement();
        keyField = table.keyField();
        name = table.tableName();
    }


    public String getName() {
        return name;
    }

    public boolean isAutoincrement() {
        return autoincrement;
    }

    public String getKeyField() {
        return keyField;
    }

    public String getKeyFieldFullName() {
        return getAnnotatedFields().get(keyField).getFullName();
    }

    public String getKeyFieldName() {
        return getAnnotatedFields().get(keyField).getName();
    }

    public Map<String, Column> getAnnotatedFields() {
        return annotatedFields;
    }

    public Map<Class, List<Field>> getAllFields() {
        return allFields;
    }

    public List<String> getManyToManyFields() {
        return manyToManyFields;
    }

    public List<String> getOneToManyFields() {
        return oneToManyFields;
    }

    public List<String> getManyToOneFields() {
        return manyToOneFields;
    }

    public Set<Scheme> getForeignSchemes() {
        return foreignSchemes;
    }

    public Set<Class> getModelClasses() {
        return modelClasses;
    }

    public boolean hasNestedObjects() {
        return !getManyToManyFields().isEmpty() || !getOneToManyFields().isEmpty() || !getManyToOneFields().isEmpty();
    }

    private void fillColumnsMap() {
        List<Field> fieldList = new ArrayList<>();
        for (Class key : allFields.keySet()) {
            for (Field field : allFields.get(key)) {
                if(!CollectionUtils.contains(fieldList, field, FIELD_NAME_COMPARATOR)) {
                    fieldList.add(field);
                }
            }
        }

        Collections.sort(fieldList, new FieldsComparator());

        for (Field field : fieldList) {
            if (field.isAnnotationPresent(FieldAnnotation.class)) {
                FieldAnnotation fieldAnnotation = field.getAnnotation(FieldAnnotation.class);
                Scheme connectedScheme = null;
                if (RelationType.MANY_TO_MANY.equals(fieldAnnotation.relation())) {
                    connectedScheme = GenericDaoHelper.getSchemeInstanceOrThrow(Scheme.getToManyClass(field));
                    manyToManyFields.add(field.getName());
                } else if (RelationType.ONE_TO_MANY.equals(fieldAnnotation.relation())) {
                    connectedScheme = GenericDaoHelper.getSchemeInstanceOrThrow(Scheme.getToManyClass(field));
                    oneToManyFields.add(field.getName());
                } else if (RelationType.MANY_TO_ONE.equals(fieldAnnotation.relation())) {
                    connectedScheme = GenericDaoHelper.getSchemeInstanceOrThrow(field.getType());
                    manyToOneFields.add(field.getName());
                    if(!ForeignKeyActions.NO_ACTION.equals(fieldAnnotation.foreignKeyAction())) {
                        connectedScheme.foreignSchemes.add(this);
                    }
                }
                Column column = new Column(fieldAnnotation, field, this, connectedScheme);
                annotatedFields.put(column.getName(), column);
            }
        }
    }

    private void addClass(Class clazz) {
        modelClasses.add(clazz);
        creatorMap.put(clazz.getName(), Reflection.creator(clazz));
        fieldTypeMap.put(clazz, FieldType.findByTypeClass(clazz));
    }

    public FieldType findByTypeClass(Class clazz) {
        return fieldTypeMap.get(clazz);
    }

    private void addAllFieldsFrom(Class<?> genericDaoClass) {
        HashMap<String, Reflection.Accessor> accessorHashMap = new HashMap<>();
        List<Field> classFields = new ArrayList<>();
        Class current = genericDaoClass;
        while (!current.equals(Object.class)) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if(field.isAnnotationPresent(FieldAnnotation.class)) {
                    classFields.add(field);
                    accessorHashMap.put(field.getName(), Reflection.accessor(field));
                }
            }
            current = current.getSuperclass();
        }

        allFields.put(genericDaoClass, classFields);
        accessorsMap.put(genericDaoClass, accessorHashMap);
    }

    public static Field getFieldByNameFrom(Class<?> genericDaoClass, String name) {
        Class current = genericDaoClass;
        while (!current.equals(Object.class)) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if(field.getName().equals(name))
                    return field;
            }
            current = current.getSuperclass();
        }

        throw new Error("field not found");
    }

    public static final Comparator<Field> FIELD_NAME_COMPARATOR = new Comparator<Field>() {
        @Override
        public int compare(Field lhs, Field rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    };


    private static final String TABLE_SUFFIX = "_table";
    private static final String FOREIGN_KEY_SUFFIX = "_fk";

    private static final String PRIMARY_KEY_TEMPLATE = "CONSTRAINT pk PRIMARY KEY (%s)";
    private static final String FOREIGN_KEY_TEMPLATE = "CONSTRAINT %1$s" + FOREIGN_KEY_SUFFIX + " FOREIGN KEY (%1$s) REFERENCES %2$s(%3$s) ON DELETE %4$s ";
    private static final String MANY_TO_MANY_TABLE_TEMPLATE = "create table if not exists %1$s (%2$s TEXT, %3$s TEXT, CONSTRAINT pk PRIMARY KEY (%2$s,%3$s) " +
            ", CONSTRAINT %2$s" + FOREIGN_KEY_SUFFIX + " FOREIGN KEY (%2$s) REFERENCES %4$s(%5$s) ON DELETE CASCADE" +
            ", CONSTRAINT %3$s" + FOREIGN_KEY_SUFFIX + " FOREIGN KEY (%3$s) REFERENCES %6$s(%7$s) ON DELETE CASCADE);";
    private static final String ALTER_TABLE_TEMPLATE = "ALTER TABLE %s ADD %s";
    private static final String JOIN_TEMPLATE = " LEFT JOIN %1$s ON %2$s = %3$s";

    public void createTables(SQLiteDatabase database) {
        String tableName = getName();
        Map<String, Column> columns = new LinkedHashMap<>();
        columns.put(COLUMN_OBJECT_CLASS_NAME, COLUMN_OBJECT_CLASS);
        columns.putAll(annotatedFields);

        if (!GenericDaoHelper.isTableExists(database, tableName)) {
            String createTableRequest = "create table if not exists %s (%s);";
            StringBuilder queryBuilder = new StringBuilder();

//        if (!columns.containsKey(COLUMN_ID_NAME) && keyField.length == 0)
//            columns.put(COLUMN_ID_NAME, _ID);

            for (String fieldKey : columns.keySet()) {
                Column column = columns.get(fieldKey);

                if (queryBuilder.length() > 0) {
                    queryBuilder.append(",");
                }
                queryBuilder.append(column.getColumnsSql());
            }

            Column column = columns.get(keyField);
            if (column != null) {
                queryBuilder.append(", ")
                        .append(String.format(PRIMARY_KEY_TEMPLATE, column.getName()));
            }

            for (String manyToOneField : getManyToOneFields()) {
                Column manyToOneColumn = getAnnotatedFields().get(manyToOneField);
                if(ForeignKeyActions.NO_ACTION.equals(manyToOneColumn.getForeignKeyActions()))
                    continue;

                Scheme manyToOneClassScheme = Scheme.getSchemeInstance(manyToOneColumn.getConnectedField().getType());
                assert manyToOneClassScheme != null;

                queryBuilder.append(", ")
                        .append(String.format(FOREIGN_KEY_TEMPLATE, manyToOneColumn.getName(), manyToOneClassScheme.getName(), manyToOneClassScheme.getKeyField(),
                                manyToOneColumn.getForeignKeyActions().getName()));
            }

            database.execSQL(String.format(createTableRequest, tableName, queryBuilder.toString()));
        } else {
            for (String fieldKey : columns.keySet()) {
                Column column = columns.get(fieldKey);

                if (!GenericDaoHelper.getColumnsFromTable(database, tableName).contains(column.getName()))
                    database.execSQL(String.format(ALTER_TABLE_TEMPLATE, tableName, column.getColumnsSql()));
            }
        }


        for (String manyToManyField : getManyToManyFields()) {
            Column manyToManyColumn = getAnnotatedFields().get(manyToManyField);
            Scheme manyToManyClassScheme = Scheme.getSchemeInstance(Scheme.getToManyClass(manyToManyColumn));
            assert manyToManyClassScheme != null;

            String column1Name = GenericDaoHelper.getColumnNameFromTable(getName());
            String column2Name = GenericDaoHelper.getColumnNameFromTable(getToManyClassName(manyToManyColumn));

            database.execSQL(String.format(MANY_TO_MANY_TABLE_TEMPLATE, getManyToManyTableName(manyToManyColumn), column1Name, column2Name, getName(), getKeyField(),
                    manyToManyClassScheme.getName(), manyToManyClassScheme.getKeyField()));
        }

        for (String oneToManyField : getOneToManyFields()) {
            Column oneToManyColumn = getAnnotatedFields().get(oneToManyField);

            Scheme oneToManyClassScheme = Scheme.getSchemeInstance(getToManyClass(oneToManyColumn));
            if(oneToManyClassScheme == null)
                throw new Error("class = " + getToManyClass(oneToManyColumn));
            String columnIdName = GenericDaoHelper.getColumnNameFromTable(getName());
            if (oneToManyClassScheme.getAnnotatedFields().containsKey(columnIdName))
                continue;

            String oneToManyTableName = oneToManyClassScheme.getName();

            if (!GenericDaoHelper.isTableExists(database, oneToManyTableName))
                oneToManyClassScheme.createTables(database);
            if (!GenericDaoHelper.getColumnsFromTable(database, oneToManyTableName).contains(columnIdName)) {
                database.execSQL(String.format(ALTER_TABLE_TEMPLATE, oneToManyTableName, columnIdName + " TEXT"));
            }
        }

    }

    public String getManyToManyTableName(Column manyToManyColumn) {
        String className = getName();
        String manyToManyClassName = getToManyClassName(manyToManyColumn);
        boolean isClassNameFirst = className.compareTo(manyToManyClassName) > 0;

        return isClassNameFirst ? (className + "_" + manyToManyClassName) : (manyToManyClassName + "_" + className);
    }

    public static Class getToManyClass(Field field) {
        Class<?> manyToManyFieldClass = field.getType();
        if (List.class.isAssignableFrom(manyToManyFieldClass)) {
            manyToManyFieldClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        }

        return manyToManyFieldClass;
    }

    public static Class getToManyClass(Column manyToManyColumn) {
        return getToManyClass(manyToManyColumn.getConnectedField());
    }

    public static String getToManyClassName(Column manyToManyColumn) {
        return getToManyClass(manyToManyColumn).getSimpleName().toLowerCase();
    }

    public Uri getUri(Context context) {
        UriHelper.Builder builder = new UriHelper.Builder(context);
        return builder.addTable(getName()).build();
    }

    public String createJoinClause(String tableAsName, Column parentColumn, OutValue<Integer> initNumber) {
        String newName = getName() + (parentColumn == null ? "" : initNumber.value++);
        String initial = parentColumn == null ? getName() + " AS " + newName :
                String.format(JOIN_TEMPLATE, getName() + " AS " + newName, tableAsName + "." + parentColumn.getName(), newName + "." + getKeyFieldName());
        StringBuilder stringBuilder = new StringBuilder(initial);
        for (String fieldName : getManyToOneFields()) {
            Column manyToOneColumn = getAnnotatedFields().get(fieldName);
            if (parentColumn != null && !CollectionUtils.contains(getAllFields().get(parentColumn.getConnectedField().getType()), manyToOneColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }

            Scheme manyToOneColumnScheme = Scheme.getSchemeInstance(manyToOneColumn.getConnectedField().getType());
            if(this.equals(manyToOneColumnScheme) && initNumber.value > 0) continue;

            stringBuilder.append(manyToOneColumnScheme.createJoinClause(newName, manyToOneColumn, initNumber));
        }

        return stringBuilder.toString();
    }

    public String[] getProjection() {
        List<String> projection = new ArrayList<>(getColumnsListFromScheme());
        for (String fieldName : getManyToOneFields()) {
            Column manyToOneColumn = getAnnotatedFields().get(fieldName);
            Scheme manyToOneColumnScheme = Scheme.getSchemeInstance(manyToOneColumn.getConnectedField().getType());

            projection.addAll(manyToOneColumnScheme.getColumnsListFromScheme());
        }

        return projection.toArray(new String[projection.size()]);
    }

    public List<String> getColumnsListFromScheme() {
        List<String> columns = new ArrayList<>();
        for (Column column : getAnnotatedFields().values()) {
            columns.add(column.getFullName());
        }

        return columns;
    }

    public List<Uri> getForeignNotifyUri(Context context) {
        List<Uri> uris = new ArrayList<>();
        for (String field : getManyToOneFields()) {
            Column manyToOneColumn = getAnnotatedFields().get(field);
            Scheme manyToOneClassScheme = Scheme.getSchemeInstance(manyToOneColumn.getConnectedField().getType());
            assert manyToOneClassScheme != null;

            uris.add(manyToOneClassScheme.getUri(context));
        }
        return uris;
    }

    public Map<String, Reflection.Creator> getCreatorMap() {
        return creatorMap;
    }

    public Map<Class, HashMap<String, Reflection.Accessor>> getAccessorsMap() {
        return accessorsMap;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Scheme && TextUtils.equals(getName(), ((Scheme) o).getName());
    }

    private static class FieldsComparator implements Comparator<Field> {

        @Override
        public int compare(Field lhs, Field rhs) {
            if (lhs.isAnnotationPresent(FieldAnnotation.class) && rhs.isAnnotationPresent(FieldAnnotation.class)) {
                FieldAnnotation leftField = lhs.getAnnotation(FieldAnnotation.class);
                FieldAnnotation rightField = rhs.getAnnotation(FieldAnnotation.class);

                Integer leftFieldRelationIndex = leftField.relation().ordinal();
                Integer rightFieldRelationIndex = rightField.relation().ordinal();
                return leftFieldRelationIndex.compareTo(rightFieldRelationIndex);
            }
            return 0;
        }
    }
}
