package alex.bobro.genericdao;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import alex.bobro.genericdao.entities.Column;
import alex.bobro.genericdao.util.CollectionUtils;

@SuppressWarnings("unused")
public final class GenericDao<DbHelper extends GenericContentProvider> {

    public static final long FAILED = -1;
    public static final long SUCCESS = 1;

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int THREAD_TIMEOUT_DURATION = 5;
    private static final TimeUnit THREAD_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private DbHelper dbHelper;
    private Executor executor;

    private GenericDao(@NotNull DbHelper dbHelper, @NotNull Executor executor) {
        this.dbHelper = dbHelper;
        this.executor = executor;
    }

    private static GenericDao instance;

    public synchronized static void init(@NotNull GenericContentProvider helper) {
        instance = new GenericDao<>(helper, new ThreadPoolExecutor(
                NUMBER_OF_CORES, NUMBER_OF_CORES * 2, THREAD_TIMEOUT_DURATION, THREAD_TIMEOUT_UNIT, new LinkedBlockingQueue<Runnable>()));
    }

    public synchronized static void init(@NotNull GenericContentProvider helper, @NotNull Executor executor) {
        instance = new GenericDao<>(helper, executor);
    }


    public static synchronized GenericDao getInstance() {
        if (instance == null) {
            throw new Error("GenericDao hasn't been initialized yet!");
        }

        return instance;
    }

    public DbHelper getDbHelper() {
        return dbHelper;
    }

    public <DbEntity> boolean saveCollection(Collection<DbEntity> entities) {
        return saveCollection(entities, null, null);
    }

    public <DbEntity> void saveCollectionAsync(final Collection<DbEntity> entities) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveCollection(entities);
            }
        }).start();
    }

    public <DbEntity> void saveCollectionAsync(final Collection<DbEntity> entities, final RequestParameters requestParameters, final QueryParameters insertParams) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveCollection(entities, requestParameters, insertParams);
            }
        }).start();
    }

    /**
     * Save all entities into db and return true if success or save nothing and return false if failure.
     *
     * @param entities The list of entities to save into db
     * @return true if success or false if failure
     */
    public <DbEntity> boolean saveCollection(Collection<DbEntity> entities, RequestParameters requestParameters, QueryParameters insertParams) {
        if (requestParameters == null)  requestParameters = new RequestParameters.Builder().build();
        if (insertParams == null) insertParams = new QueryParameters.Builder().build();

        if (entities == null || entities.isEmpty())
            return false;

        RequestParameters.NotificationMode notificationMode = requestParameters.getNotificationMode();
        boolean isAfterAll = RequestParameters.NotificationMode.AFTER_ALL.equals(notificationMode);
        try {
            ArrayList<GenericContentProviderOperation> contentProviderOperations = new ArrayList<>(entities.size());
            Scheme scheme = null;
            for (DbEntity entity : entities) {
                if (scheme == null)
                    scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entity.getClass());
                ArrayList<GenericContentProviderOperation> operations = GenericDaoHelper.getContentProviderOperationBatch(scheme, entity, null, insertParams, requestParameters);

                if (isAfterAll) {
                    contentProviderOperations.addAll(operations);
                } else {
                    bulkInsertGenericOperations(operations, insertParams);
                    Object keyValue = GenericDaoHelper.toKeyValue(scheme, entity);
                    dbHelper.notifyChange(scheme, keyValue);
                }
            }

            if (RequestParameters.NotificationMode.AFTER_ALL.equals(requestParameters.getNotificationMode())) {
                bulkInsertGenericOperations(contentProviderOperations, insertParams);
                dbHelper.notifyChange(scheme);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void bulkInsertGenericOperations(ArrayList<GenericContentProviderOperation> contentProviderOperations, QueryParameters queryParameters) {
        Map<String, Pair<QueryParameters,List<ContentValues>>> map = groupContentProviderBatches(contentProviderOperations);
        for (String key : map.keySet()) {
            Pair<QueryParameters,List<ContentValues>> pair = map.get(key);
            dbHelper.bulkInsert(key, pair.second.toArray(new ContentValues[pair.second.size()]), pair.first);
        }
    }

    private Map<String, Pair<QueryParameters,List<ContentValues>>> groupContentProviderBatches(List<GenericContentProviderOperation> operations) {
        Map<String, Pair<QueryParameters,List<ContentValues>>> map = new LinkedHashMap<>();
        for (GenericContentProviderOperation operation : operations) {
            String table = operation.getTable();
            Pair<QueryParameters,List<ContentValues>> pair = map.get(table);
            if(pair == null) {
                List<ContentValues> list = new ArrayList<>();
                pair = new Pair<>(operation.getQueryParameters(), list);
                map.put(table, pair);
            }

            pair.second.add(operation.getContentValues());
        }
        return map;
    }

    public <DbEntity> long save(DbEntity dbEntity) {
        return save(dbEntity, null, null, null);
    }

    public <DbEntity> void saveAsync(final DbEntity dbEntity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                save(dbEntity);
            }
        }).start();
    }

    public <DbEntity> void saveAsync(final DbEntity dbEntity, final ContentValues contentValues, final QueryParameters insertParams, final RequestParameters requestParameters) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                save(dbEntity, contentValues, insertParams, requestParameters);
            }
        }).start();
    }

    /**
     * Save entity into database if not exists or update it otherwise.
     *
     * @param dbEntity entity to save into db
     * @return id of saved object if save, state UPDATED if updated, state FAILURE if fault
     */
    public <DbEntity> long save(DbEntity dbEntity, ContentValues contentValues, QueryParameters insertParams, RequestParameters requestParameters) {
        if (dbEntity == null)
            return FAILED;

        Class objectClass = dbEntity.getClass();
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(objectClass);
        Object keyValue = GenericDaoHelper.toKeyValue(scheme, dbEntity);
        if (keyValue == null && scheme.hasNestedObjects()) throw new Error("Key value can't be null for object with connections");

        if(contentValues == null) contentValues = new ContentValues();

        ArrayList<GenericContentProviderOperation> operations = GenericDaoHelper.getContentProviderOperationBatch(scheme, dbEntity, contentValues, insertParams, requestParameters);
        dbHelper.applyBatch(operations);
        dbHelper.notifyChange(scheme, keyValue);

        return SUCCESS;
    }


    private void notifyDeleted(Scheme scheme) {
        dbHelper.notifyChange(scheme);
        for (Scheme foreignScheme : scheme.getForeignSchemes()) {
            dbHelper.notifyChange(foreignScheme);
        }
    }

    public boolean delete(Class entityClass, @NotNull String[] keyValues) {
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        if (scheme.getKeyField() == null) {
            throw new IllegalArgumentException("You should mark some fields as key to use this method!");
        }

        String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
        int result = dbHelper.delete(scheme.getName(), where, keyValues);
        if (result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }


    public boolean delete(Class entityClass, QueryParameters deleteParams, String whereClause, String... whereArgs) {
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        int result = dbHelper.delete(scheme.getName(), whereClause, whereArgs, deleteParams);

        if (result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public boolean delete(Class entityClass) {
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        int result = dbHelper.delete(scheme.getName(), null, null);

        if (result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public boolean delete(Class entityClass, String whereClause, String... whereArgs) {
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        int result = dbHelper.delete(scheme.getName(), whereClause, whereArgs);

        if (result > 0) {
            notifyDeleted(scheme);
        }

        return result > 0;
    }

    public <DbEntity> boolean delete(DbEntity dbEntity) {
        if (dbEntity == null)
            return false;

        Class objectClass = dbEntity.getClass();
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(objectClass);

        if (scheme.getKeyField() != null) {
            String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
            Object fieldValue = GenericDaoHelper.getValueForField(scheme, objectClass, dbEntity, scheme.getAnnotatedFields().get(scheme.getKeyField()).getConnectedField());
            String[] values = new String[]{String.valueOf(fieldValue)};

            int result = dbHelper.delete(scheme.getName(), where, values);
            if (result > 0) {
                notifyDeleted(scheme);
            }

            return result > 0;
        }

        return false;
    }

    public <DbEntity> boolean delete(Collection<DbEntity> dbEntities) {
        if (dbEntities == null)
            return false;

        Class objectClass = GenericDaoHelper.getParametrizedType(dbEntities.getClass());
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(objectClass);

        if (scheme.getKeyField() != null) {
            String where = GenericDaoHelper.arrayToInWhereString(scheme.getKeyFieldFullName());
            List<String> fieldValues = new ArrayList<>();

            for (DbEntity entity : dbEntities) {
                Object fieldValue = GenericDaoHelper.getValueForField(scheme, objectClass, entity, scheme.getAnnotatedFields().get(scheme.getKeyField()).getConnectedField());
                fieldValues.add(fieldValue.toString());
            }

            String[] values = new String[]{GenericDaoHelper.arrayToQueryString(fieldValues.toArray(new String[fieldValues.size()]))};

            int result = dbHelper.delete(scheme.getName(), where, values);
            if (result > 0) {
                notifyDeleted(scheme);
            }

            return result > 0;
        }

        return false;
    }

    public int getCount(Class entityClass, @NotNull String where, String... args) {
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        Cursor cursor = dbHelper.query(scheme.getName(), new String[]{"count(*)"}, where, args, null, null, null);

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
        return getObjects(null, entityClass, queryMap);
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

    public <DbEntity> List<DbEntity> getObjects(RequestParameters requestParameters, Class<DbEntity> entityClass, HashMap<String, String> queryMap) {
        if (queryMap == null)
            return null;

        Pair<String, String[]> selectionPair = getSelectionPairFromHashMap(queryMap);
        return getObjects(requestParameters, entityClass, selectionPair.first, selectionPair.second, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(RequestParameters requestParameters, Class<DbEntity> entityClass) {
        return getObjects(requestParameters, entityClass, null, null, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass) {
        return getObjects(null, entityClass);
    }

    public <DbEntity> List<DbEntity> getObjects(RequestParameters requestParameters, Class<DbEntity> entityClass, String where, String... args) {
        return getObjects(requestParameters, entityClass, where, args, null, null, null, null);
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass, String where, String... args) {
        return getObjects(null, entityClass, where, args);
    }

    public <DbEntity> List<DbEntity> getObjects(RequestParameters requestParameters, Class<DbEntity> entityClass, String where, String[] args, String groupBy, String having, String orderBy, String limit) {
        if(requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);
        ArrayList<DbEntity> objects = new ArrayList<>();

        final String tableName = scheme.getName();
        if (tableName != null) {
            QueryParameters.Builder queryParametersBuilder = new QueryParameters.Builder();
            queryParametersBuilder.addParameter(GenericDaoContentProvider.IS_MANY_TO_ONE_NESTED_AFFECTED, String.valueOf(requestParameters.isManyToOneGotWithParent()));
            Cursor cursor = dbHelper.query(scheme.getName(), null, where, args, groupBy, having, orderBy, limit, queryParametersBuilder.build());

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    objects = new ArrayList<>();
                    do {
                        DbEntity entity = GenericDaoHelper.fromCursor(scheme, cursor, entityClass);
                        objects.add(entity);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        if (!RequestParameters.RequestMode.JUST_PARENT.equals(requestParameters.getRequestMode())) {
            for (DbEntity entity : objects) {
                fillEntityWithNestedObjects(entity, requestParameters);
            }
        }

        return objects;
    }

    public <DbEntity> List<DbEntity> getObjects(Class<DbEntity> entityClass, String where, String[] args, String groupBy, String having, String orderBy, String limit) {
        return getObjects(null, entityClass, where, args, groupBy, having, orderBy, limit);
    }

    public <DbEntity> DbEntity getObjectById(@Nullable RequestParameters requestParameters, Class<DbEntity> entityClass, @NotNull String... keyValues) {
        if(requestParameters == null) {
            requestParameters = new RequestParameters.Builder().build();
        }
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(entityClass);

        DbEntity entity = null;

        if (!TextUtils.isEmpty(scheme.getKeyField())) {
            QueryParameters.Builder queryParametersBuilder = new QueryParameters.Builder();
            queryParametersBuilder.addParameter(GenericDaoContentProvider.IS_MANY_TO_ONE_NESTED_AFFECTED, String.valueOf(requestParameters.isManyToOneGotWithParent()));
            String where = GenericDaoHelper.arrayToWhereString(scheme.getKeyFieldFullName());
            Cursor cursor = dbHelper.query(scheme.getName(), null, where, keyValues, null, null, null, null,queryParametersBuilder.build());
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    entity = GenericDaoHelper.fromCursor(scheme, cursor, entityClass);
                }
                cursor.close();
            }

            if (entity != null && !RequestParameters.RequestMode.JUST_PARENT.equals(requestParameters.getRequestMode())) {
                fillEntityWithNestedObjects(entity, requestParameters);
            }
        }


        return entity;
    }

    public <DbEntity> DbEntity getObjectById(Class<DbEntity> entityClass, @NotNull String... keyValues) {
        return getObjectById(null, entityClass, keyValues);
    }


    public <DbEntity> void fillEntityWithNestedObjects(DbEntity entity, RequestParameters requestParameters) {
        String keyValue = GenericDaoHelper.toKeyValue(entity);
        if (TextUtils.isEmpty(keyValue))
            return;

        Class objectClass = entity.getClass();
        Scheme scheme = GenericDaoHelper.getSchemeInstanceOrThrow(objectClass);

        String columnName = GenericDaoHelper.getColumnNameFromTable(scheme.getName());

        if(!requestParameters.isManyToOneGotWithParent()) fillEntityWithManyToOneFields(scheme, entity, objectClass,keyValue, requestParameters);
        fillEntityWithManyToManyFields(scheme, entity, objectClass, columnName, keyValue, requestParameters);
        fillEntityWithOneToManyFields(scheme, entity, objectClass, columnName, keyValue, requestParameters);
    }

    private <DbEntity> void fillEntityWithManyToOneFields(Scheme scheme, DbEntity entity, Class objectClass, String keyValue, RequestParameters requestParameters) {
        for (String manyToOneField : scheme.getManyToOneFields()) {
            Column manyToOneColumn = scheme.getAnnotatedFields().get(manyToOneField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), manyToOneColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }

            String manyToOneColumnName = manyToOneColumn.getName();
            QueryParameters.Builder queryParametersBuilder = new QueryParameters.Builder();
            queryParametersBuilder.addParameter(GenericDaoContentProvider.IS_MANY_TO_ONE_NESTED_AFFECTED, String.valueOf(false));
            Cursor objectCursor = dbHelper.query(scheme.getName(), new String[]{manyToOneColumnName}, scheme.getKeyField() + "=?", new String[]{keyValue}, null, null, null, null, queryParametersBuilder.build());

            if(objectCursor != null && objectCursor.moveToFirst()) {
                String key = objectCursor.getString(objectCursor.getColumnIndex(manyToOneColumnName));
                if(TextUtils.isEmpty(key)) return;

                Scheme manyToOneScheme = GenericDaoHelper.getSchemeInstanceOrThrow(manyToOneColumn.getConnectedField().getType());
                Object object = GenericDao.getInstance().getObjectById(requestParameters, manyToOneColumn.getConnectedField().getType(), key);

                //noinspection unchecked
                GenericDaoHelper.setValueForField(scheme, objectClass, entity, manyToOneColumn.getConnectedField(), object);
            }
        }
    }

    private <DbEntity> void fillEntityWithManyToManyFields(Scheme scheme, DbEntity entity, Class objectClass, String columnName, String keyValue, RequestParameters requestParameters) {
        for (String manyToManyField : scheme.getManyToManyFields()) {
            Column manyToManyColumn = scheme.getAnnotatedFields().get(manyToManyField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), manyToManyColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
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

                    Object value = GenericDao.getInstance().getObjectById(requestParameters, relatedColumnClass, GenericDaoHelper.fromKeyValue(keyArray));
                    //noinspection unchecked
                    valuesList.add(value);
                } while (relationsCursor.moveToNext());
                relationsCursor.close();

                GenericDaoHelper.setValueForField(scheme, objectClass, entity, manyToManyColumn.getConnectedField(), valuesList);
            }
        }
    }

    private <DbEntity> void fillEntityWithOneToManyFields(Scheme scheme, DbEntity entity, Class objectClass, String columnName, String keyValue, RequestParameters requestParameters) {
        for (String oneToManyField : scheme.getOneToManyFields()) {
            Column oneToManyColumn = scheme.getAnnotatedFields().get(oneToManyField);
            if (!CollectionUtils.contains(scheme.getAllFields().get(objectClass), oneToManyColumn.getConnectedField(), Scheme.FIELD_NAME_COMPARATOR)) {
                continue;
            }
            Scheme oneToManyScheme = GenericDaoHelper.getSchemeInstanceOrThrow(Scheme.getToManyClass(oneToManyColumn));
            List objectsCollection = GenericDao.getInstance().getObjects(requestParameters, Scheme.getToManyClass(oneToManyColumn), oneToManyScheme.getName() + "." + columnName + "=?", keyValue);

            //noinspection unchecked
            GenericDaoHelper.setValueForField(scheme, objectClass, entity, oneToManyColumn.getConnectedField(), new ArrayList<>(objectsCollection));
        }
    }


}
