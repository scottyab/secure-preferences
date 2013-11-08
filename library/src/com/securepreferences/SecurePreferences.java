/*
 * Copyright (C) 2013, Daniel Abraham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.securepreferences;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.securepreferences.util.Base64;

/**
 * Wrapper class for Android's {@link SharedPreferences} interface, which adds a
 * layer of encryption to the persistent storage and retrieval of sensitive
 * key-value pairs of primitive data types.
 * <p>
 * This class provides important - but nevertheless imperfect - protection
 * against simple attacks by casual snoopers. It is crucial to remember that
 * even encrypted data may still be susceptible to attacks, especially on rooted
 * or stolen devices!
 * <p>
 * 
 * @see <a
 *      href="http://www.codeproject.com/Articles/549119/Encryption-Wrapper-for-Android-SharedPreferences">CodeProject
 *      article</a>
 */
public class SecurePreferences implements SharedPreferences {

	private static final int KEY_SIZE = 256;
	// requires Spongycastle crypto libraries
	// private static final String AES_KEY_ALG = "AES/GCM/NoPadding";
	// private static final String AES_KEY_ALG = "AES/CBC/PKCS5Padding";
	private static final String AES_KEY_ALG = "AES";
	private static final String PRIMARY_PBE_KEY_ALG = "PBKDF2WithHmacSHA1";
	private static final String BACKUP_PBE_KEY_ALG = "PBEWithMD5AndDES";
	private static final int ITERATIONS = 2000;
	// change to SC if using Spongycastle crypto libraries
	private static final String PROVIDER = "BC";

	private static SharedPreferences sFile;
	private static byte[] sKey;
	private static boolean sLoggingEnabled = false;
	private static final String TAG = SecurePreferences.class.getName();

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            the caller's context
	 */
	public SecurePreferences(Context context) {
		// Proxy design pattern
		if (SecurePreferences.sFile == null) {
			SecurePreferences.sFile = PreferenceManager
					.getDefaultSharedPreferences(context);
		}
		// Initialize encryption/decryption key
		try {
			final String key = SecurePreferences.generateAesKeyName(context);
			String value = SecurePreferences.sFile.getString(key, null);
			if (value == null) {
				value = SecurePreferences.generateAesKeyValue();
				SecurePreferences.sFile.edit().putString(key, value).commit();
			}
			SecurePreferences.sKey = SecurePreferences.decode(value);
		} catch (Exception e) {
			if (sLoggingEnabled) {
				Log.e(TAG, "Error init:" + e.getMessage());
			}
			throw new IllegalStateException(e);
		}
	}

	private static String encode(byte[] input) {
		return Base64.encodeToString(input, Base64.NO_PADDING | Base64.NO_WRAP);
	}

	private static byte[] decode(String input) {
		return Base64.decode(input, Base64.NO_PADDING | Base64.NO_WRAP);
	}

	private static String generateAesKeyName(Context context)
			throws InvalidKeySpecException, NoSuchAlgorithmException,
			NoSuchProviderException {
		final char[] password = context.getPackageName().toCharArray();

		final byte[] salt = getDeviceSerialNumber(context).getBytes();

		SecretKey key;
		try {
			// TODO: what if there's an OS upgrade and now supports the primary
			// PBE
			key = SecurePreferences.generatePBEKey(password, salt,
					PRIMARY_PBE_KEY_ALG, ITERATIONS, KEY_SIZE);
		} catch (NoSuchAlgorithmException e) {
			// older devices may not support the have the implementation try
			// with a weaker
			// algorthm
			key = SecurePreferences.generatePBEKey(password, salt,
					BACKUP_PBE_KEY_ALG, ITERATIONS, KEY_SIZE);
		}
		return SecurePreferences.encode(key.getEncoded());
	}

	/**
	 * Derive a secure key based on the passphraseOrPin
	 * 
	 * @param passphraseOrPin
	 * @param salt
	 * @param algorthm
	 *            - which PBE algorthm to use. some <4.0 devices don;t support
	 *            the prefered PBKDF2WithHmacSHA1
	 * @param iterations
	 *            - Number of PBKDF2 hardening rounds to use. Larger values
	 *            increase computation time (a good thing), defaults to 1000 if
	 *            not set.
	 * @param keyLength
	 * @return Derived Secretkey
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchProviderException
	 */
	private static SecretKey generatePBEKey(char[] passphraseOrPin,
			byte[] salt, String algorthm, int iterations, int keyLength)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchProviderException {

		if (iterations == 0) {
			iterations = 1000;
		}

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(
				algorthm, PROVIDER);
		KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations,
				keyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		return secretKey;
	}

	/**
	 * Gets the hardware serial number of this device.
	 * 
	 * @return serial number or Settings.Secure.ANDROID_ID if not available.
	 */
	private static String getDeviceSerialNumber(Context context) {
		// We're using the Reflection API because Build.SERIAL is only available
		// since API Level 9 (Gingerbread, Android 2.3).
		try {
			String deviceSerial = (String) Build.class.getField("SERIAL").get(
					null);
			if (TextUtils.isEmpty(deviceSerial)) {
				deviceSerial = Settings.Secure.getString(
						context.getContentResolver(),
						Settings.Secure.ANDROID_ID);
			}
			return deviceSerial;
		} catch (Exception ignored) {
			// default to Android_ID
			return Settings.Secure.getString(context.getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
	}

	private static String generateAesKeyValue() throws NoSuchAlgorithmException {
		// Do *not* seed secureRandom! Automatically seeded from system entropy
		final SecureRandom random = new SecureRandom();

		// Use the largest AES key length which is supported by the OS
		final KeyGenerator generator = KeyGenerator.getInstance("AES");
		try {
			generator.init(KEY_SIZE, random);
		} catch (Exception e) {
			try {
				generator.init(192, random);
			} catch (Exception e1) {
				generator.init(128, random);
			}
		}
		return SecurePreferences.encode(generator.generateKey().getEncoded());
	}

	private static String encrypt(String cleartext) {
		if (cleartext == null || cleartext.length() == 0) {
			return cleartext;
		}
		try {
			final Cipher cipher = Cipher.getInstance(AES_KEY_ALG, PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
					SecurePreferences.sKey, AES_KEY_ALG));
			return SecurePreferences.encode(cipher.doFinal(cleartext
					.getBytes("UTF-8")));
		} catch (Exception e) {
			if (sLoggingEnabled) {
				Log.w(TAG, "encrypt", e);
			}
			return null;
		}
	}

	private static String decrypt(String ciphertext) {
		if (ciphertext == null || ciphertext.length() == 0) {
			return ciphertext;
		}
		try {
			final Cipher cipher = Cipher.getInstance(AES_KEY_ALG, PROVIDER);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
					SecurePreferences.sKey, AES_KEY_ALG));
			return new String(cipher.doFinal(SecurePreferences
					.decode(ciphertext)), "UTF-8");
		} catch (Exception e) {
			if (sLoggingEnabled) {
				Log.w(TAG, "decrypt", e);
			}
			return null;
		}
	}

	@Override
	public Map<String, String> getAll() {
		final Map<String, ?> encryptedMap = SecurePreferences.sFile.getAll();
		final Map<String, String> decryptedMap = new HashMap<String, String>(
				encryptedMap.size());
		for (Entry<String, ?> entry : encryptedMap.entrySet()) {
			try {
				decryptedMap.put(SecurePreferences.decrypt(entry.getKey()),
						SecurePreferences.decrypt(entry.getValue().toString()));
			} catch (Exception e) {
				// Ignore unencrypted key/value pairs
			}
		}
		return decryptedMap;
	}

	@Override
	public String getString(String key, String defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		return (encryptedValue != null) ? SecurePreferences
				.decrypt(encryptedValue) : defaultValue;
	}

	/**
	 * 
	 * Added to get a values as as it can be useful to store values that are
	 * already encrypted and encoded
	 * 
	 * @param key
	 * @param defaultValue
	 * @return Unencrypted value of the key or the defaultValue if
	 */
	public String getStringUnencrypted(String key, String defaultValue) {
		final String nonEncryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		return (nonEncryptedValue != null) ? nonEncryptedValue : defaultValue;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Set<String> getStringSet(String key, Set<String> defaultValues) {
		final Set<String> encryptedSet = SecurePreferences.sFile.getStringSet(
				SecurePreferences.encrypt(key), null);
		if (encryptedSet == null) {
			return defaultValues;
		}
		final Set<String> decryptedSet = new HashSet<String>(
				encryptedSet.size());
		for (String encryptedValue : encryptedSet) {
			decryptedSet.add(SecurePreferences.decrypt(encryptedValue));
		}
		return decryptedSet;
	}

	@Override
	public int getInt(String key, int defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		if (encryptedValue == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(SecurePreferences.decrypt(encryptedValue));
		} catch (NumberFormatException e) {
			throw new ClassCastException(e.getMessage());
		}
	}

	@Override
	public long getLong(String key, long defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		if (encryptedValue == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(SecurePreferences.decrypt(encryptedValue));
		} catch (NumberFormatException e) {
			throw new ClassCastException(e.getMessage());
		}
	}

	@Override
	public float getFloat(String key, float defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		if (encryptedValue == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(SecurePreferences.decrypt(encryptedValue));
		} catch (NumberFormatException e) {
			throw new ClassCastException(e.getMessage());
		}
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.encrypt(key), null);
		if (encryptedValue == null) {
			return defaultValue;
		}
		try {
			return Boolean.parseBoolean(SecurePreferences
					.decrypt(encryptedValue));
		} catch (NumberFormatException e) {
			throw new ClassCastException(e.getMessage());
		}
	}

	@Override
	public boolean contains(String key) {
		return SecurePreferences.sFile.contains(SecurePreferences.encrypt(key));
	}

	@Override
	public Editor edit() {
		return new Editor();
	}

	/**
	 * Wrapper for Android's {@link android.content.SharedPreferences.Editor}.
	 * <p>
	 * Used for modifying values in a {@link SecurePreferences} object. All
	 * changes you make in an editor are batched, and not copied back to the
	 * original {@link SecurePreferences} until you call {@link #commit()} or
	 * {@link #apply()}.
	 */
	public static class Editor implements SharedPreferences.Editor {
		private SharedPreferences.Editor mEditor;

		/**
		 * Constructor.
		 */
		private Editor() {
			mEditor = SecurePreferences.sFile.edit();
		}

		@Override
		public SharedPreferences.Editor putString(String key, String value) {
			mEditor.putString(SecurePreferences.encrypt(key),
					SecurePreferences.encrypt(value));
			return this;
		}

		/**
		 * This is useful for storing values that have be encrypted by something
		 * else
		 * 
		 * @param key
		 *            - encrypted as usual
		 * @param value
		 *            will not be encrypted
		 * @return
		 */
		public SharedPreferences.Editor putStringNoEncrypted(String key,
				String value) {
			mEditor.putString(SecurePreferences.encrypt(key), value);
			return this;
		}

		@Override
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public SharedPreferences.Editor putStringSet(String key,
				Set<String> values) {
			final Set<String> encryptedValues = new HashSet<String>(
					values.size());
			for (String value : values) {
				encryptedValues.add(SecurePreferences.encrypt(value));
			}
			mEditor.putStringSet(SecurePreferences.encrypt(key),
					encryptedValues);
			return this;
		}

		@Override
		public SharedPreferences.Editor putInt(String key, int value) {
			mEditor.putString(SecurePreferences.encrypt(key),
					SecurePreferences.encrypt(Integer.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putLong(String key, long value) {
			mEditor.putString(SecurePreferences.encrypt(key),
					SecurePreferences.encrypt(Long.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putFloat(String key, float value) {
			mEditor.putString(SecurePreferences.encrypt(key),
					SecurePreferences.encrypt(Float.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putBoolean(String key, boolean value) {
			mEditor.putString(SecurePreferences.encrypt(key),
					SecurePreferences.encrypt(Boolean.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor remove(String key) {
			mEditor.remove(SecurePreferences.encrypt(key));
			return this;
		}

		@Override
		public SharedPreferences.Editor clear() {
			mEditor.clear();
			return this;
		}

		@Override
		public boolean commit() {
			return mEditor.commit();
		}

		@Override
		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public void apply() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				mEditor.apply();
			} else {
				commit();
			}
		}
	}

	public static boolean isLoggingEnabled() {
		return sLoggingEnabled;
	}

	public static void setLoggingEnabled(boolean loggingEnabled) {
		sLoggingEnabled = loggingEnabled;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		SecurePreferences.sFile
				.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		SecurePreferences.sFile
				.unregisterOnSharedPreferenceChangeListener(listener);
	}
}
