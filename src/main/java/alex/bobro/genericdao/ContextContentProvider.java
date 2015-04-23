package alex.bobro.genericdao;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by alex on 12/16/14.
 */
public class ContextContentProvider extends AbstractGenericContentProvider  {

    private Context context;

    public ContextContentProvider(Context context) {
        this.context = context;
    }


    @Override
    public long insert(String table, String nullColumnHack, ContentValues values, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        context.getContentResolver().insert(builder.build(), values);
        return 0;
    }


    @Override
    public int bulkInsert(String table, ContentValues[] values, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        return context.getContentResolver().bulkInsert(builder.build(), values);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        return context.getContentResolver().update(builder.build(), values, whereClause, whereArgs);
    }


    @Override
    public int delete(String table, String whereClause, String[] whereArgs, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        return context.getContentResolver().delete(builder.build(), whereClause, whereArgs);
    }

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        return context.getContentResolver().query(builder.build(),columns,selection,selectionArgs, orderBy);
    }

    @Override
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, QueryParameters parameters) {
        UriHelper.Builder builder = UriHelper.generateBuilder(getContext(), table, parameters);
        return context.getContentResolver().query(builder.build(),columns,selection,selectionArgs,orderBy);
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<GenericContentProviderOperation> operations) {
        ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();
        for (GenericContentProviderOperation genericContentProviderOperation : operations) {
            contentProviderOperations.add(genericContentProviderOperation.toContentProviderOperation(getContext()));
        }
        try {
            return context.getContentResolver().applyBatch(MetaDataParser.getFirstAuthority(getContext()), contentProviderOperations);
        } catch (RemoteException e) {
            return null;
        } catch (OperationApplicationException e) {
            return null;
        }
    }


    @Override
    public void notifyChange(Scheme scheme) {
        if(scheme != null) {
            context.getContentResolver().notifyChange(scheme.getUri(getContext()), null);
        }
    }

    @Override
    public void notifyChange(Scheme scheme, Object id) {
        if(scheme != null) {
            context.getContentResolver().notifyChange(scheme.getUri(getContext()).buildUpon().appendPath(String.valueOf(id)).build(), null);
        }
    }

    public Context getContext() {
        return context;
    }
}
