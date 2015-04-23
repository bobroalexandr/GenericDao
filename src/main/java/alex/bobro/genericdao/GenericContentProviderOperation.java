package alex.bobro.genericdao;


import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class GenericContentProviderOperation {

    public enum Type {
        INSERT, DELETE, UPDATE;
    }

    private Type type;
    private String table;
    private ContentValues contentValues;
    private QueryParameters queryParameters;

    private GenericContentProviderOperation(Builder builder) {
        this.type = builder.type;
        this.table = builder.table;
        this.contentValues = builder.contentValues;
        this.queryParameters = builder.queryParameters;
    }

    public static Builder newInsert() {
        return new Builder(Type.INSERT);
    }

    public static Builder newDelete() {
        return new Builder(Type.DELETE);
    }

    public static Builder newUpdate() {
        return new Builder(Type.UPDATE);
    }

    public ContentProviderOperation toContentProviderOperation(Context context) {
        Uri uri = UriHelper.generateBuilder(context, table, queryParameters).build();

        ContentProviderOperation.Builder contentProviderOperationBuilder = null;
        switch (type) {
            case INSERT:
                contentProviderOperationBuilder = ContentProviderOperation.newInsert(uri);
                break;

            case DELETE:
                contentProviderOperationBuilder = ContentProviderOperation.newDelete(uri);
                break;

            case UPDATE:
                contentProviderOperationBuilder = ContentProviderOperation.newUpdate(uri);
                break;

            default:
                contentProviderOperationBuilder = ContentProviderOperation.newInsert(uri);
                break;
        }
        contentProviderOperationBuilder.withValues(contentValues);

        return contentProviderOperationBuilder.build();
    }

    public static class Builder {
        private Type type;
        private String table;
        private ContentValues contentValues;
        private QueryParameters queryParameters;

        public Builder(Type type) {
            this.type = type;
        }

        public GenericContentProviderOperation build() {
            return new GenericContentProviderOperation(this);
        }

        public Builder withTable(String table) {
            this.table = table;
            return this;
        }

        public Builder withContentValues(ContentValues contentValues) {
            this.contentValues = contentValues;
            return this;
        }

        public Builder withQueryParameters(QueryParameters queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }
    }
}
