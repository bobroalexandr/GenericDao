package alex.bobro.genericdao.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class Reflection {

	public static class Creator<T> {

		private final Constructor<T> c;

		private Creator(@NotNull Constructor<T> c) {
			if (!c.isAccessible()) {
				c.setAccessible(true);
			}
			this.c = c;
		}

		public Constructor<T> getConstructor() {
			return c;
		}

		@SuppressWarnings("unchecked")
		public T newInstanceFor(Object... args) {
			try {
				return c.newInstance(args);
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}

	public static <T> Creator<T> creator(@NotNull Class<T> clazz, Class<?>... args) {
		try {
			return new Creator<>(clazz.getDeclaredConstructor(args));
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}


	public static class Accessor<T> {
		private final Field f;

		private Accessor(@NotNull Field f) {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			this.f = f;
		}

		public Field getField() {
			return f;
		}

		@SuppressWarnings("unchecked")
		public <V> V get(T receiver) {
			try {
				return (V) f.get(receiver);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			}
		}

		public <V> void set(T receiver, V value) {
			try {
				f.set(receiver, value);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			}
		}
	}

	public static <T> Accessor<T> accessor(boolean isCritical, @NotNull String fieldName, @NotNull Class<T> clazz, @Nullable Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		try {
			Field field = clazz.getDeclaredField(fieldName);
			Class<?> fieldType = field.getType();
			if (!fieldType.equals(type)) {
				fieldType = (Class<?>) field.getGenericType();
				if (fieldType == null || !fieldType.equals(type)) {
					throw new NoSuchFieldException(String.format(
							"There is no such field: %s %s;", type.getSimpleName(), fieldName));
				}
			}
			return new Accessor<>(field);
		} catch (NoSuchFieldException e) {
			if(isCritical) {
				throw new Error(e);
			} else {
				return null;
			}
		}
	}

	public static <T> Accessor<T> accessor(Field field) {
		return new Accessor<>(field);
	}


	public static class Invoker<T> {

		private final Method m;

		private Invoker(@NotNull Method m) {
			if (!m.isAccessible()) {
				m.setAccessible(true);
			}
			this.m = m;
		}

		public Method getMethod() {
			return m;
		}

		@SuppressWarnings("unchecked")
		public <R> R invokeFor(T receiver, Object... args) {
			try {
				return (R) m.invoke(receiver, args);
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}

	public static <T> Invoker<T> invoker(boolean isCritical, @NotNull String methodName, @NotNull Class<T> clazz, Class<?>... args) {
		try {
			return new Invoker<>(clazz.getDeclaredMethod(methodName, args));
		} catch (NoSuchMethodException e) {
			if(isCritical) {
				throw new Error(e);
			} else {
				return null;
			}
		}
	}

	public static @Nullable Method getMethod(Class<?> clazz, String methodName) {
		try {
			return clazz.getMethod(methodName);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
