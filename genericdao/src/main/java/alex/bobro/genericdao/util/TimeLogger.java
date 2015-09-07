package alex.bobro.genericdao.util;

import android.util.Log;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 7/17/15.
 */
public class TimeLogger {

    private static String TAG = TimeLogger.class.getSimpleName();
    public static Map<Object, Pair<Long, TimeUnit>> keys = new HashMap<>();

    public static void startLogging(Object key, TimeUnit timeUnit) {
        Long time = (TimeUnit.NANOSECONDS.equals(timeUnit)) ? System.nanoTime() : System.currentTimeMillis();
        keys.put(key, new Pair<Long, TimeUnit>(time, timeUnit));
    }

    public static void logTime(Object key, String log) {
        Pair<Long, TimeUnit> pair = keys.get(key);
        if (pair == null) throw new Error("omg");
        Long time = (TimeUnit.NANOSECONDS.equals(pair.second)) ? System.nanoTime() : System.currentTimeMillis();
        Log.i(TAG, log + " " + (time - pair.first));
        keys.put(key, new Pair<>((TimeUnit.NANOSECONDS.equals(pair.second)) ? System.nanoTime() : System.currentTimeMillis(), pair.second));
    }

    public static void stopLogging(Object key) {
        keys.remove(key);
    }

}
