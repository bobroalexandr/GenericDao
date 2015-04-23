package alex.bobro.genericdao;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by alex on 3/4/15.
 */
public abstract class AbstractGenericContentProvider implements GenericContentProvider {

    public long insert(String table, String nullColumnHack, ContentValues values) {
        return this.insert(table, nullColumnHack, values, null);
    }


    public int bulkInsert(String table, ContentValues[] values) {
        return this.bulkInsert(table, values, null);
    }


    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return this.update(table, values, whereClause, whereArgs, null);
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        return this.delete(table, whereClause, whereArgs,null);
    }

    public Cursor query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit) {
        return this.query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy) {
        return this.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, null);
    }

}
