package alex.bobro.genericdao.util;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Modifier;

public class SingletonHelper<T> {

	private final Reflection.Creator<T> creator;

	public SingletonHelper(@NonNull Class<T> clazz, Class<?>... args) {
		creator = Reflection.creator(clazz, args);

		if (Modifier.isPublic(creator.getConstructor().getModifiers()))
			throw new Error("Singleton class constructor mustn't be PUBLIC!");
	}


	private volatile T instance;


	private T writeInstance(@NonNull String error, @Nullable T newInstance) {
		T instanceLocal = instance;

//		if (instanceLocal != null && newInstance != null) {
//			throw new Error(error);
//		}

		synchronized (this) {
			instanceLocal = instance;

//			if (instanceLocal != null && newInstance != null) {
//				throw new Error(error);
//			}

			return instance = newInstance;
		}
	}


	private static final String DEFAULT_ERROR_INITIALIZED   = " has been initialized already!";
	private static final String DEFAULT_ERROR_UNINITIALIZED = " hasn't been initialized yet!";

	public void initialize(@Nullable String alreadyInitializedError, Object... args) {
		if (android.text.TextUtils.isEmpty(alreadyInitializedError)) {
			alreadyInitializedError = creator.getConstructor().getDeclaringClass().getSimpleName() + DEFAULT_ERROR_INITIALIZED;
		}
		writeInstance(alreadyInitializedError, creator.newInstanceFor(args));
	}

	public void uninitialize(@Nullable String notInitializedYetError) {
		if (android.text.TextUtils.isEmpty(notInitializedYetError)) {
			notInitializedYetError = creator.getConstructor().getDeclaringClass().getSimpleName() + DEFAULT_ERROR_UNINITIALIZED;
		}
		writeInstance(notInitializedYetError, null);
	}


	public T obtain(@Nullable String notInitializedYetError) {
		T instanceLocal = instance;

		if (instanceLocal == null) {
			if (android.text.TextUtils.isEmpty(notInitializedYetError)) {
				notInitializedYetError = creator.getConstructor().getDeclaringClass().getSimpleName() + DEFAULT_ERROR_UNINITIALIZED;
			}
			throw new Error(notInitializedYetError);
		}
		return instanceLocal;
	}


	public T obtainInitialized(Object... args) {
		return writeInstance(creator.getConstructor().getDeclaringClass().getSimpleName() + DEFAULT_ERROR_UNINITIALIZED, creator.newInstanceFor(args));
	}

}
