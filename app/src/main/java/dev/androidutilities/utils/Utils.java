package dev.androidutilities.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

public class Utils {
	
	private static final Random RANDOM = new Random();
	private static final char[] ALPHABET = new char[26 * 2 + 10];
	
	static {
		int i = 0;
		for (i = 0; i < 10; ++i)
			ALPHABET[i] = Integer.toString(i).charAt(0);
		for (char c = 'a'; c <= 'z'; ++c)
			ALPHABET[i++] = c;
		for (char c = 'A'; c <= 'Z'; ++c)
			ALPHABET[i++] = c;
	}
	
	public static String getRandomString(int length) {
		char[] buffer = new char[length];
		for (int i = 0; i < length; ++i)
			buffer[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
		return String.valueOf(buffer);
	}
	
	public static byte[] getRandomArray(int length) {
		byte[] data = new byte[length];
		RANDOM.nextBytes(data);
		return data;
	}
	
	public static int getRandomInt(int max) {
		return RANDOM.nextInt(max);
	}
	
	public static byte[] serialize(Serializable object) {
		try {
			byte[] result;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);   
			out.writeObject(object);
			result = bos.toByteArray();
			out.close();
			bos.close();
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T deserialize(byte[] bytes) {
		try {
			T result;
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInput in = new ObjectInputStream(bis);
			result = (T)in.readObject();
			bis.close();
			in.close();
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
