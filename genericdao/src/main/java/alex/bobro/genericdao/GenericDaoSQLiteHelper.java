package alex.bobro.genericdao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by Alex on 13.03.14.
 */
public abstract class GenericDaoSQLiteHelper extends SQLiteOpenHelper {

    private final Context context;
    private final String name;

    protected GenericDaoSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        this.context = context;
        this.name = name;
    }


    public Context getContext() {
        return context;
    }

    public String getName() {
        return name;
    }


    public interface Creator<T extends GenericDaoSQLiteHelper> {
        public T newInstance(Context context, String name);
    }


    private static final String OBLIGATORY_CREATOR_FIELD_NAME = "CREATOR";

    private static <T extends GenericDaoSQLiteHelper> T invokeCreator(Class<T> clazz, Context context, String name) {
        try {
            @SuppressWarnings("unchecked")
            Creator<T> creator = (Creator<T>) clazz
                    .getField(OBLIGATORY_CREATOR_FIELD_NAME).get(null);
            return creator.newInstance(context, name);
        } catch (NoSuchFieldException e) {
            throw new Error("SQLInterlockHelper successor MUST implements a Creator<T> interface! " +
                    "It should be stored in 'public static Creator<T> CREATOR'");
        } catch (IllegalAccessException e) {
            throw new Error("SQLInterlockHelper successor CREATOR field is inaccessible or non-static!");
        }
    }

    private static final class SQLiteInterlockHelperSingleton {
        public static final Map<Class, GenericDaoSQLiteHelper> INSTANCE_MAP
                = Collections.synchronizedMap(new WeakHashMap<Class, GenericDaoSQLiteHelper>());
    }


    private static boolean isObsolete(GenericDaoSQLiteHelper cachedInstance, Context context, String name) {
        return cachedInstance == null || !(cachedInstance.getContext().equals(context)
                && TextUtils.equals(cachedInstance.getName(), name));
    }

    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "unchecked"})
    protected static <T extends GenericDaoSQLiteHelper> T getInstance(@NotNull Class<T> clazz, @NotNull Context context, @NotNull String name) {
        GenericDaoSQLiteHelper instanceLocal = SQLiteInterlockHelperSingleton.INSTANCE_MAP.get(clazz);

        if (isObsolete(instanceLocal, context, name)) {
            synchronized (clazz) {
                instanceLocal = SQLiteInterlockHelperSingleton.INSTANCE_MAP.get(clazz);

                if (isObsolete(instanceLocal, context, name)) {
                    SQLiteInterlockHelperSingleton.INSTANCE_MAP.put(clazz, instanceLocal
                            = invokeCreator(clazz, context, name));
                }
            }
        }
        return (T) instanceLocal;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");
        createTables(db);
    }

    protected abstract void appendSchemes(List<Class> classes);

    private List<Class> getSchemesClasses() {
        List<Class> classes = new ArrayList<>();
        appendSchemes(classes);
        return classes;
    }

    protected void createTables(SQLiteDatabase db) {
        for (String key : Scheme.getInstances().keySet()) {
            Scheme scheme = Scheme.getInstances().get(key);
            scheme.createTables(db);
        }
    }

}
