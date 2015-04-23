package alex.bobro.genericdao;


import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Map;

public interface GenericContentProvider {

    public long insert(String table, String nullColumnHack, ContentValues values, QueryParameters parameters);

    public int bulkInsert(String table, ContentValues[] values, QueryParameters parameters);

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs, QueryParameters parameters);

    public int delete(String table, String whereClause, String[] whereArgs, QueryParameters parameters);

    public Cursor query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit, QueryParameters parameters);

    public Cursor query(boolean distinct, String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit, QueryParameters parameters);

    public abstract long insert(String table, String nullColumnHack, ContentValues values);


    public abstract int bulkInsert(String table, ContentValues[] values);

    public abstract int update(String table, ContentValues values, String whereClause, String[] whereArgs);

    public abstract int delete(String table, String whereClause, String[] whereArgs);

    public abstract Cursor query(String table, String[] columns, String selection,
                                 String[] selectionArgs, String groupBy, String having,
                                 String orderBy, String limit);

    public abstract Cursor query(String table, String[] columns, String selection,
                                 String[] selectionArgs, String groupBy, String having,
                                 String orderBy);

    public ContentProviderResult[] applyBatch(ArrayList<GenericContentProviderOperation> operations);

    public void notifyChange(Scheme scheme);

    public void notifyChange(Scheme scheme, Object id);
}
