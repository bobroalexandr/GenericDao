package dev.androidutilities.utils;

public final class Log {
	public static String DEFAULT_TAG = "log";

	public static final void i(String tag, String string) {
		android.util.Log.i(tag, string);
	}

	public static final void e(String tag, String string) {
		android.util.Log.e(tag, string);
	}

	public static final void d(String tag, String string) {
		android.util.Log.d(tag, string);
	}

	public static final void v(String tag, String string) {
		android.util.Log.v(tag, string);
	}

	public static final void w(String tag, String string) {
		android.util.Log.w(tag, string);
	}

	public static final void i(String string) {
		android.util.Log.i(DEFAULT_TAG, string);
	}

	public static final void e(String string) {
		android.util.Log.e(DEFAULT_TAG, string);
	}

	public static final void d(String string) {
		android.util.Log.d(DEFAULT_TAG, string);
	}

	public static final void v(String string) {
		android.util.Log.v(DEFAULT_TAG, string);
	}

	public static final void w(String string) {
		android.util.Log.w(DEFAULT_TAG, string);
	}

	public static final void i(String format, Object... args) {
		android.util.Log.i(DEFAULT_TAG, String.format(format, args));
	}

	public static final void e(String format, Object... args) {
		android.util.Log.e(DEFAULT_TAG, String.format(format, args));
	}

	public static final void d(String format, Object... args) {
		android.util.Log.d(DEFAULT_TAG, String.format(format, args));
	}

	public static final void v(String format, Object... args) {
		android.util.Log.v(DEFAULT_TAG, String.format(format, args));
	}

	public static final void w(String format, Object... args) {
		android.util.Log.w(DEFAULT_TAG, String.format(format, args));
	}

	public static final void d(String msg, Throwable t) {
		android.util.Log.d(DEFAULT_TAG, msg, t);
	}

	public static final void e(String msg, Throwable t) {
		android.util.Log.e(DEFAULT_TAG, msg, t);
	}

	public static final void i(String msg, Throwable t) {
		android.util.Log.i(DEFAULT_TAG, msg, t);
	}

	public static final void w(String msg, Throwable t) {
		android.util.Log.w(DEFAULT_TAG, msg, t);
	}

	public static final void v(String msg, Throwable t) {
		android.util.Log.v(DEFAULT_TAG, msg, t);
	}
}
