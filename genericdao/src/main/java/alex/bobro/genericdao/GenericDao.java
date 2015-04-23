package alex.bobro.genericdao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Pair;

import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.util.CollectionUtils;

import alex.bobro.genericdao.util.SingletonHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class GenericDao<DbHelper extends GenericContentProvider> {

    public static final long FAILED = -1;
    public static final long SUCCESS = 1;

    private DbHelper dbHelper;

    private GenericDao(@NotNull DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    private static final SingletonHelper<GenericDao> SINGLETON
            = new SingletonHelper<>(GenericDao.class, GenericContentProvider.class);


    public static void init(@NotNull GenericContentProvider helper) {
        SINGLETON.initialize(null, helper);
    }


    public static GenericDao getInstance() {
        return SINGLETON.obtain(null);
    }

    public DbHelper getDbHelper() {
        return dbHelper;
    }

    public <DbEntity> boolean saveCollection(Collection<DbEntity> entities) {
        return saveCollection(entities, null);
    }


    /**
     * Save all entities into db and return true if success or save nothing and return false if failure.
     *
     * @param entities The list of entities to save into db
     * @return true if success or false if failure
     */
    public <DbEntity> boolean saveCollection(Collection<DbEntity> entities, RequestParameters requestParameters) {
        if(requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }

        if (entities == null || entities.isEmpty())
            return false;

        RequestParameters.NotificationMode notificationMode = requestParameters.getNotificationMode();
        try {
            ArrayList<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();
            Scheme scheme = null;
            for (DbEntity entity : entities) {
                if (scheme == null)
                    scheme = Scheme.getSchemeInstance(entity.getClass());
                ArrayList<GenericContentProviderOperation> operations = getContentProviderOperationBatch(entity, null, null, requestParameters);

                if(RequestParameters.NotificationMode.AFTER_ALL.equals(notificationMode)) {
                    contentProviderOperations.addAll(operations);
                } else {
                    dbHelper.applyBatch(operations);
                    Object keyValue = GenericDaoHelper.toKeyValue(entity);
                    dbHelper.notifyChange(scheme, keyValue);
                }
            }

            if(RequestParameters.NotificationMode.AFTER_ALL.equals(requestParameters.getNotificationMode())) {
                dbHelper.applyBatch(contentProviderOperations);
                dbHelper.notifyChange(scheme);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public <DbEntity> long save(DbEntity dbEntity) {
        return save(dbEntity, null, null, null);
    }


    protected <DbEntity> ArrayList<GenericContentProviderOperation> getContentProviderOperationBatch(DbEntity dbEntity, HashMap<String, String> additionalCV, QueryParameters insertParams, RequestParameters requestParameters) {
        if(requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }
        ArrayList<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();

        Class objectClass = dbEntity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);
        Object keyValue = GenericDaoHelper.toKeyValue(dbEntity);
        if(keyValue == null) {
            return null;
        }

        try {
            GenericContentProviderOperation.Builder builder = GenericContentProviderOperation.newInsert();
            builder.withContentValues(GenericDaoHelper.toCv(requestParameters.isDeep(), dbEntity, additionalCV))
                    .withQueryParameters(insertParams)
                    .withTable(scheme.getName());
            contentProviderOperations.add(builder.build());

            if(requestParameters.isDeep()) {
                contentProviderOperations.addAll(getNestedContentProviderOperationBatch(dbEntity));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contentProviderOperations;
    }

    public <DbEntity> List<GenericContentProviderOperation> getNestedContentProviderOperationBatch(DbEntity dbEntity) {
        List<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>();

        Class objectClass = dbEntity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);
        try {
            String keyValue = GenericDaoHelper.toKeyValue(dbEntity);
            if (TextUtils.isEmpty(keyValue))
                return contentProviderOperations;

            for (String oneToManyField : scheme.getOneToManyFields()) {
                Column oneToManyColumn = scheme.getAnnotatedFields().get(oneToManyField);
                if(!scheme.getAllFields().get(objectClass).contains(oneToManyColumn.getConnectedField())) {
                    continue;
                }
                Object value = GenericDaoHelper.getValueForField(objectClass, dbEntity, oneToManyColumn.getConnectedField());
                if (value == null)
                    continue;

                HashMap<String, String> additionalCv = null;
                if (!scheme.getAnnotatedFields().containsKey(GenericDaoHelper.getColumnNameFromTable(scheme.getName()))) {
                    additionalCv = new HashMap<>();
                    additionalCv.put(GenericDaoHelper.getColumnNameFromTable(scheme.getName()), keyValue);
                }

                List list = (List) value;
                for (Object object : list) {
                    contentProviderOperations.addAll(GenericDao.getInstance().getContentProviderOperationBatch(object, additionalCv, null, null));
                }
            }

            for (String manyToManyField : scheme.getManyToManyFields()) {
                Column manyToManyColumn = scheme.getAnnotatedFields().get(manyToManyField);
                if(!scheme.getAllFields().get(objectClass).contains(manyToManyColumn.getConnectedField())) {
                    continue;
                }
                Object value = GenericDaoHelper.getValueForField(objectClass, dbEntity, manyToManyColumn.getConnectedField());
                if (value != null) {
                    List list = (List) value;
                    for (Object object : list) {
                        contentProviderOperations.addAll(GenericDao.getInstance().getContentProviderOperationBatch(object, null, null, null));

                        ContentValues manyToManyCV = new ContentValues();
                        manyToManyCV.put(GenericDaoHelper.getColumnNameFromTable(scheme.getName()), keyValue);
                        manyToManyCV.put(GenericDaoHelper.getColumnNameFromTable(Scheme.getToManyClassName(manyToManyColumn)), GenericDaoHelper.toKeyValue(object));

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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return contentProviderOperations;
    }

    /**
     * Save entity into database if not exists or update it otherwise.
     *
     * @param dbEntity entity to save into db
     * @return id of saved object if save, state UPDATED if updated, state FAILURE if fault
     */
    protected <DbEntity> long save(DbEntity dbEntity, HashMap<String, String> additionalCV, QueryParameters insertParams, RequestParameters requestParameters) {
        if (dbEntity == null)
            return FAILED;

        long id = FAILED;

        Class objectClass = dbEntity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);
        Object keyValue = GenericDaoHelper.toKeyValue(dbEntity);
        if(keyValue == null) {
            return id;
        }

        try {
            ArrayList<GenericContentProviderOperation> operations = getContentProviderOperationBatch(dbEntity, additionalCV, insertParams, requestParameters);
            dbHelper.applyBatch(operations);
            dbHelper.notifyChange(scheme, keyValue);
            id = SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return id;
    }



    private void notifyDeleted(Scheme scheme) {
        dbHelper.notifyChange(scheme);
        for (Scheme foreignScheme : scheme.getForeignSchemes()) {
            dbHelper.notifyChange(foreignScheme);
        }
    }

    public boolean delete(Class entityClass, @NotNull String[] keyValues) {
        Scheme scheme = Scheme.getSchemeInstance(entityClass);
        if (scheme.getKeyField() == null) {
            throw new IllegalArgumentException("You should mark some fields as key to use this method!");
        }

        String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
        int result = dbHelper.delete(scheme.getName(), where, keyValues);
        if(result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }



    public boolean delete(Class entityClass, QueryParameters deleteParams, String whereClause, String... whereArgs) {
        Scheme scheme = Scheme.getSchemeInstance(entityClass);
        int result = dbHelper.delete(scheme.getName(), whereClause, whereArgs, deleteParams);

        if(result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public boolean delete(Class entityClass) {
        Scheme scheme = Scheme.getSchemeInstance(entityClass);
        int result = dbHelper.delete(scheme.getName(), null, null);

        if(result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public boolean delete(Class entityClass, String whereClause, String... whereArgs) {
        Scheme scheme = Scheme.getSchemeInstance(entityClass);
        int result = dbHelper.delete(scheme.getName(), whereClause, whereArgs);

        if(result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public <DbEntity> boolean delete(DbEntity dbEntity) {
        if (dbEntity == null)
            return false;

        Class objectClass = dbEntity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);

        try {
            if (scheme.getKeyField() != null) {
                String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
                Object fieldValue = GenericDaoHelper.getValueForField(objectClass, dbEntity, scheme.getAnnotatedFields().get(scheme.getKeyField()).getConnectedField());
                String[] values = new String[]{String.valueOf(fieldValue)};

                int result = dbHelper.delete(scheme.getName(), where, values);
                if(result > 0) {
                    notifyDeleted(scheme);
                }

                return result > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public <DbEntity> boolean delete(Collection<DbEntity> dbEntities) {
        if (dbEntities == null)
            return false;

        Class objectClass = GenericDaoHelper.getParametrizedType(dbEntities.getClass());
        Scheme scheme = Scheme.getSchemeInstance(objectClass);

        try {
            if (scheme.getKeyField() != null) {
                String where = GenericDaoHelper.arrayToInWhereString(scheme.getKeyFieldFullName());
                List<String> fieldValues = new ArrayList<>();

                for (DbEntity entity : dbEntities) {
                    Object fieldValue = GenericDaoHelper.getValueForField(objectClass, entity, scheme.getAnnotatedFields().get(scheme.getKeyField()).getConnectedField());
                    fieldValues.add(fieldValue.toString());
                }

                String[] values = new String[]{GenericDaoHelper.arrayToQueryString(fieldValues.toArray(new String[fieldValues.size()]))};

                int result = dbHelper.delete(scheme.getName(), where, values);
                if(result > 0) {
                    notifyDeleted(scheme);
                }

                return result > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Pair<String, String[]> getSelectionPairFromHashMap(HashMap<String, String> queryMap) {
        if (queryMap == null || queryMap.isEmpty())
            return null;

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        Iterator<String> keysIterator = queryMap.keySet().iterator();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            String value = queryMap.get(key);

            if (value == null)
                continue;

            if (keysIterator.hasNext())
                selection.append(key).append("=? AND ");
            else
                selection.append(key).append("=?");

            selectionArgs.add(value);
        }

        return new Pair<>(selection.toString(), selectionArgs.toArray(new String[selectionArgs.size()]));
    }

    public int getCount(Class entityClass, @NotNull String where, String... args) {
        Cursor cursor = dbHelper.query(Scheme.getSchemeInstance(entityClass).getName(), new String[]{"count(*)"}, where, args, null, null, null);

        cursor.moveToFirst();
        try {
            if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
                return cursor.getInt(0);
            } else {
                return 0;

            }
        } finally {
            cursor.close();
        }
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass, HashMap<String, String> queryMap) {
        return getObjects(true, entityClass, queryMap);
    }

    public <DbEntity> List<DbEntity> getObjects(boolean deepFilling, Class<DbEntity> entityClass, HashMap<String, String> queryMap) {
        if (queryMap == null)
            return null;

        Pair<String, String[]> selectionPair = getSelectionPairFromHashMap(queryMap);
        return getObjects(deepFilling, entityClass, selectionPair.first, selectionPair.second, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(boolean deepFilling, Class<DbEntity> entityClass) {
        return getObjects(deepFilling, entityClass, null, null, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass) {
        return getObjects(true, entityClass);
    }

    public <DbEntity> List<DbEntity> getObjects(boolean deepFilling, Class<DbEntity> entityClass, String where, String... args) {
        return getObjects(deepFilling, entityClass, where, args, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass, String where, String... args) {
        return getObjects(true, entityClass, where, args);
    }

    public <DbEntity> List<DbEntity> getObjects(boolean deepFilling, Class<DbEntity> entityClass, String where, String[] args, String groupBy, String having, String orderBy, String limit) {

        Scheme scheme = Scheme.getSchemeInstance(entityClass);
        ArrayList<DbEntity> objects = new ArrayList<>();

        final String tableName = scheme.getName();
        if (tableName != null) {
            Cursor cursor = dbHelper.query(scheme.getName(), null, where, args, groupBy, having, orderBy, limit);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    objects = new ArrayList<>();
                    do {
                        DbEntity entity = GenericDaoHelper.fromCursor(cursor, entityClass);
                        objects.add(entity);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        if (deepFilling) {
            for (DbEntity entity : objects) {
                fillEntityWithNestedObjects(entity, true);
            }
        }

        return objects;
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass, String where, String[] args, String groupBy, String having, String orderBy, String limit) {
        return getObjects(true, entityClass, where, args, groupBy, having, orderBy, limit);
    }

    public <DbEntity> DbEntity getObjectById(boolean deepFilling, Class<DbEntity> entityClass, @NotNull String... keyValues) {
        Scheme scheme = Scheme.getSchemeInstance(entityClass);

        try {
            DbEntity entity = null;

            if (!TextUtils.isEmpty(scheme.getKeyField())) {

                String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
                Cursor cursor = dbHelper.query(scheme.getName(), null, where, keyValues, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        entity = GenericDaoHelper.fromCursor(cursor, entityClass);
                    }
                    cursor.close();
                }

                if (deepFilling && entity != null) {
                    fillEntityWithNestedObjects(entity, true);
                }
            }


            return entity;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return null;
    }

    public <DbEntity> DbEntity getObjectById(Class<DbEntity> entityClass, @NotNull String... keyValues) {
        return getObjectById(true, entityClass, keyValues);
    }


    public <DbEntity> void fillEntityWithNestedObjects(DbEntity entity, boolean deepFilling) {
        String keyValue = GenericDaoHelper.toKeyValue(entity);
        if (TextUtils.isEmpty(keyValue))
            return;

        Class objectClass = entity.getClass();
        Scheme scheme = Scheme.getSchemeInstance(objectClass);

        String columnName = GenericDaoHelper.getColumnNameFromTable(scheme.getName());

        for (String manyToManyField : scheme.getManyToManyFields()) {
            Column manyToManyColumn = scheme.getAnnotatedFields().get(manyToManyField);
            if(!CollectionUtils.contains(scheme.getAllFields().get(objectClass), manyToManyColumn.getConnectedField(), Scheme.fieldNameComparator)) {
                continue;
            }
            String manyToManyTable = scheme.getManyToManyTableName(manyToManyColumn);
            Class relatedColumnClass = Scheme.getToManyClass(manyToManyColumn);
            String relatedColumn = GenericDaoHelper.getColumnNameFromTable(Scheme.getToManyClassName(manyToManyColumn));

            Cursor relationsCursor = dbHelper.query(manyToManyTable, new String[]{relatedColumn}, columnName + "=?", new String[]{keyValue}, null, null, null, null, null);

            if (relationsCursor != null && relationsCursor.moveToNext()) {
                List valuesList = new ArrayList();

                do {
                    String keyArray = relationsCursor.getString(relationsCursor.getColumnIndex(relatedColumn));

                    Object value = GenericDao.getInstance().getObjectById(deepFilling, relatedColumnClass, GenericDaoHelper.fromKeyValue(keyArray));
                    valuesList.add(value);
                } while (relationsCursor.moveToNext());
                relationsCursor.close();

                try {
                    GenericDaoHelper.setValueForField(objectClass, entity, manyToManyColumn.getConnectedField(), valuesList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (String oneToManyField : scheme.getOneToManyFields()) {
            Column oneToManyColumn = scheme.getAnnotatedFields().get(oneToManyField);
            if(!CollectionUtils.contains(scheme.getAllFields().get(objectClass), oneToManyColumn.getConnectedField(), Scheme.fieldNameComparator)) {
                continue;
            }
            Scheme oneToManyScheme = Scheme.getSchemeInstance(Scheme.getToManyClass(oneToManyColumn));
            Collection objectsCollection = GenericDao.getInstance().getObjects(deepFilling, scheme.getToManyClass(oneToManyColumn), oneToManyScheme.getName() + "." + columnName + "=?", keyValue);

            try {
                GenericDaoHelper.setValueForField(objectClass, entity, oneToManyColumn.getConnectedField(), new ArrayList(objectsCollection));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static class DaoException extends Exception {
        public DaoException(String message) {
            super(message);
        }
    }

}
