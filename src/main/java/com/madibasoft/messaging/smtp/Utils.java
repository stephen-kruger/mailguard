package com.madibasoft.messaging.smtp;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Utils {
	private static final String CIPHER = "AES/CBC/PKCS5PADDING";
	private static final String initVector = "encryptionIntVec";

	public static String encrypt2(String value, String key)
			throws UnsupportedEncodingException, InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

		byte[] encrypted = cipher.doFinal(value.getBytes());
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(encrypted);
	}

	public static String decrypt2(String encrypted, String key)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
		Base64.Decoder decoder = Base64.getDecoder();

		byte[] original = cipher.doFinal(decoder.decode(encrypted));

		return new String(original);
	}

	/*
	 * The silly logging setup thinks each line is a distinct log, which screws up
	 * stack traces. So jam all the stack trace lines into a single JSON object
	 * which doesn't hurt Go programmers eyes.
	 */
	public static void jsonError(Logger log, String context, Throwable t) {
		JsonObject jo = new JsonObject();
		jo.addProperty("context", context);
		jo.addProperty("message", t.getMessage());
		jo.addProperty("class", t.getClass().getName());
		JsonArray ja = new JsonArray();
		for (StackTraceElement element : t.getStackTrace()) {
			if (!element.isNativeMethod())
				addStackTrace(ja, element);
		}
		jo.add("trace", ja);
		log.error("{}", jo);
	}

	private static void addStackTrace(JsonArray parent, StackTraceElement element) {
		JsonObject jo = new JsonObject();
		jo.addProperty("file", element.getFileName() + " (" + element.getLineNumber() + ')');
		jo.addProperty("class", element.getClassName());
		jo.addProperty("method", element.getMethodName());
		parent.add(jo);
	}
}
