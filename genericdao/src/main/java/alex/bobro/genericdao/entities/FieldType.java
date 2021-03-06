package alex.bobro.genericdao.entities;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public enum FieldType {
    INTEGER(SQLiteType.INTEGER, Integer.class, int.class),
    STRING(SQLiteType.TEXT, String.class),
    BOOLEAN(SQLiteType.INTEGER, Boolean.class, boolean.class),
    LONG(SQLiteType.INTEGER, Long.class, long.class),
    BYTE(SQLiteType.INTEGER, Byte.class, byte.class),
    SHORT(SQLiteType.INTEGER, Short.class, short.class),
    FLOAT(SQLiteType.REAL, Float.class, float.class),
    DOUBLE(SQLiteType.REAL, Double.class, double.class),
    BLOB(SQLiteType.BLOB, Byte[].class, byte[].class),
    STRING_ARRAY(SQLiteType.TEXT, String[].class),
    DATE(SQLiteType.INTEGER, Date.class),
    OBJECT(SQLiteType.TEXT);

    private final Class[] cls;
    private final SQLiteType sqliteType;

    private FieldType(SQLiteType sqliteType, Class... cls) {
        this.sqliteType = sqliteType;
        this.cls = cls;
    }

    public Class[] getTypeClass() {
        return cls;
    }

    public SQLiteType getSqliteType() {
        return sqliteType;
    }

    private static Map<Class, FieldType> fieldTypeMap = new HashMap<>();

    static {
        for (FieldType b : FieldType.values()) {
            for (Class clazz : b.cls) {
                fieldTypeMap.put(clazz, b);
            }
        }
    }

    public static FieldType getTypeByClass(Class cls) {
        FieldType fieldType = fieldTypeMap.get(cls);
        return fieldType == null ? OBJECT : fieldType;
    }

    public static FieldType findByTypeClass(Class cls) {
        return findBy(classPredicate, cls);
    }

    public static FieldType findByTypeName(String clsName) {
        return findBy(classNamePredicate, clsName);
    }

    private static <T> FieldType findBy(FieldTypeClassPredicate<T> predicate, T condition) {
        if (condition != null) {
            for (FieldType b : FieldType.values()) {
                Class[] typeClass = b.getTypeClass();
                for (int i = 0; i < typeClass.length; i++) {
                    Class typeClazz = typeClass[i];
                    if (predicate.isConditionEquals(typeClazz, condition)) {
                        return b;
                    }
                }
            }
        }
        return OBJECT;
    }

    static FieldTypeClassPredicate<String> classNamePredicate = new FieldTypeClassPredicate<String>() {
        @Override
        public boolean isConditionEquals(Class<?> cls, String condition) {
            return condition.equals(cls.getSimpleName());
        }
    };

    static FieldTypeClassPredicate<Class> classPredicate = new FieldTypeClassPredicate<Class>() {
        @Override
        public boolean isConditionEquals(Class<?> cls, Class condition) {
            return cls.isAssignableFrom(condition);
        }
    };

    interface FieldTypeClassPredicate<T> {
        boolean isConditionEquals(Class<?> cls, T condition);
    }
}
