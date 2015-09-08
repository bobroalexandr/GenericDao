package alex.bobro.genericdao;


import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public interface GenericContentProvider {

    long insert(String table, String nullColumnHack, ContentValues values, QueryParameters parameters);

    int bulkInsert(String table, ContentValues[] values, QueryParameters parameters);

    int bulkInsert(ContentValues[] values, List<QueryParameters> parameters);

    int update(String table, ContentValues values, String whereClause, String[] whereArgs, QueryParameters parameters);

    int delete(String table, String whereClause, String[] whereArgs, QueryParameters parameters);

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy, String limit, QueryParameters parameters);

    Cursor query(boolean distinct, String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy, String limit, QueryParameters parameters);

    long insert(String table, String nullColumnHack, ContentValues values);


    int bulkInsert(String table, ContentValues[] values);

    int update(String table, ContentValues values, String whereClause, String[] whereArgs);

    int delete(String table, String whereClause, String[] whereArgs);

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy, String limit);

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy);

    ContentProviderResult[] applyBatch(ArrayList<GenericContentProviderOperation> operations);

    void notifyChange(Scheme scheme);

    void notifyChange(Scheme scheme, Object id);


}
