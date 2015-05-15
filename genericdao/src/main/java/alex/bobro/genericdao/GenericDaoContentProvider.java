package alex.bobro.genericdao;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import alex.bobro.genericdao.util.OutValue;

/**
 * Created by alex on 24.11.14.
 */
public abstract class GenericDaoContentProvider extends ContentProvider {

    public static final int SINGLE_TABLE = 100;

    public static final String CONFLICT_ALGORITHM = "conflictAlgorithm";
    public static final String SHOULD_NOTIFY = "shouldNotify";
    public static final String IS_MANY_TO_ONE_NESTED_AFFECTED = "isManyToOneNestedAffected";
    public static final String ID = "id";

    private UriMatcher uriMatcher;
    private String authority;

    @Override
    public boolean onCreate() {
        authority = MetaDataParser.getAuthority(getContext(), getClass());
        uriMatcher = buildUriMatcher();
        return true;
    }

    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(authority, "table/*", SINGLE_TABLE);
        return matcher;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
//        Log.i("test!", "query " + uri);
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        String table = UriHelper.getTable(uri);
        Scheme scheme = Scheme.getSchemeInstance(table);

        boolean isManyToOneNestedAffected = Boolean.valueOf(UriHelper.getQueryValueFromUri(uri, IS_MANY_TO_ONE_NESTED_AFFECTED, Boolean.TRUE.toString()));
        String joinTable = (scheme == null || !isManyToOneNestedAffected) ? table : scheme.createJoinClause(null, null, new OutValue<>(0));

        Cursor cursor = db.query(joinTable, projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
//        Log.i("test!", "insert " + uri);
        SQLiteDatabase db = getDbHelper().getWritableDatabase();

        String table = UriHelper.getTable(uri);
        String conflictAlgorithm = uri.getQueryParameter(CONFLICT_ALGORITHM);
        Scheme scheme = Scheme.getSchemeInstance(table);
        Uri resultUri = uri;

        long id = -1;
        if(scheme == null) {
            id = TextUtils.isEmpty(conflictAlgorithm) ? db.insert(table, null, values) : db.insertWithOnConflict(table, null, values, Integer.parseInt(conflictAlgorithm));
        } else {
            String keyField = scheme.getKeyFieldName();
            String keyValue = String.valueOf(values.get(keyField));

            if(!isExists(db, table, keyField, keyValue)) {
                id = db.insert(table, null, values);
            } else {
                id = update(uri, values, GenericDaoHelper.arrayToWhereString(keyField), new String[]{keyValue});
            }
            resultUri = resultUri.buildUpon().appendPath(keyValue).build();
        }


        notifyUri(resultUri);
        return resultUri;
    }

    private boolean isExists(SQLiteDatabase sqLiteDatabase, @NotNull String table, @NotNull String fieldName, @NotNull String fieldValue) {
        Cursor cursor = sqLiteDatabase.query(table, null, GenericDaoHelper.arrayToWhereString(fieldName), new String[]{fieldValue}, null, null, null, null);

        boolean answer = false;
        if (cursor != null) {
            answer = cursor.moveToFirst();
            cursor.close();
        }
        return answer;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
//        Log.i("test!", "delete " + uri);
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");

        String table = UriHelper.getTable(uri);
        Scheme scheme = Scheme.getSchemeInstance(table);
        int count = db.delete(table, selection, selectionArgs);
        if(count > 0) {
            notifyUri(uri);
            if(scheme != null) {
                for (Scheme foreignScheme : scheme.getForeignSchemes()) {
                    notifyUri(foreignScheme.getUri(getContext()));
                }
            }
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
//        Log.i("test!", "update " + uri);
        SQLiteDatabase db = getDbHelper().getWritableDatabase();

        String table = UriHelper.getTable(uri);
        int count = db.update(table, values, selection, selectionArgs);
        notifyUri(uri);
        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        String table = UriHelper.getTable(uri);
        String conflictAlgorithm = uri.getQueryParameter(CONFLICT_ALGORITHM);

        int count = 0;
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                if(TextUtils.isEmpty(conflictAlgorithm))
                    db.insert(table, null, value);
                else
                    db.insertWithOnConflict(table, null, value, Integer.parseInt(conflictAlgorithm));

                count++;
            }
            db.setTransactionSuccessful();
            notifyUri(uri);
        } finally {
            db.endTransaction();
        }
        return count;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderResult[] results = null;
        SQLiteDatabase database = getDbHelper().getWritableDatabase();
        database.beginTransaction();
        results = super.applyBatch(operations);
        database.setTransactionSuccessful();
        database.endTransaction();

        return results;
    }

    public abstract SQLiteOpenHelper getDbHelper();


    private void notifyUri(Uri uri) {
        String shouldNotify = uri.getQueryParameter(SHOULD_NOTIFY);
        if (getContext() != null && String.valueOf(Boolean.TRUE.booleanValue()).equals(shouldNotify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }



}
