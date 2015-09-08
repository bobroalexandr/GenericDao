package alex.bobro.genericdao;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UriHelper {


    public static class Builder {
        private final Uri.Builder uriBuilder;
        private final Uri baseUri;

        public Builder(Context context, Class<? extends ContentProvider> providerClass) {
            String authority = MetaDataParser.getAuthority(context, providerClass);
            this.baseUri = Uri.parse("content://" + authority);
            uriBuilder = baseUri.buildUpon();
        }

        public Builder(Context context) {
            String authority = MetaDataParser.getFirstAuthority(context);
            this.baseUri = Uri.parse("content://" + authority);
            this.uriBuilder = baseUri.buildUpon();
        }

        public Builder addTable(String mainTable) {
            uriBuilder.appendPath(GenericDaoContentProvider.TABLE).appendPath(mainTable);
            return this;
        }

        public Builder addQuery(String key, String parameter) {
            uriBuilder.appendQueryParameter(key, parameter);
            return this;
        }

        public Builder addPath(String path) {
            uriBuilder.appendPath(path);
            return this;
        }

        public Uri build() {
            return uriBuilder.build();
        }
    }


    public static String getQueryValueFromUri(Uri uri, String key, String defValue) {
        String value = uri.getQueryParameter(key);
        return value == null ? defValue : value;
    }

    public static Builder generateBuilder(Context context, String table, QueryParameters parameters) {
        Builder builder = new Builder(context);
        builder.addTable(table);
        fillQueryFromMap(builder, parameters);

        return builder;
    }

    public static Builder generateBuilder(Context context, List<QueryParameters> parameters) {
        Builder builder = new Builder(context);
        try {
            builder.addQuery(GenericDaoContentProvider.PARAMS, GenericDaoHelper.fromQueryParametersListToBase64(parameters));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder;
    }



    private static void fillQueryFromMap(Builder builder, QueryParameters parameters) {
        if(parameters != null) {
            Map<String, String> parametersMap = parameters.getParameters();
            for (String key : parametersMap.keySet()) {
                if(!GenericDaoContentProvider.ID.equals(key))
                    builder.addQuery(key, parametersMap.get(key));
                else
                    builder.addPath(parametersMap.get(key));
            }
        }

    }

    public static String getTable(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();

        int tableIndex = pathSegments.indexOf(GenericDaoContentProvider.TABLE);
        if(tableIndex == -1 || pathSegments.size() < tableIndex + 2) {
            throw new Error("NO TABLE IN URI!");
        }

        return pathSegments.get(tableIndex + 1);
    }

    public static String getId(Uri uri) {
        return uri.getLastPathSegment();
    }


}
