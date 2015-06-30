/*
 * Copyright (C) 2015, Scott Alexander-Bown, Daniel Abraham
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Wrapper class for Android's {@link SharedPreferences} interface, which adds a
 * layer of encryption to the persistent storage and retrieval of sensitive
 * key-value pairs of primitive data types.
 * <p>
 * This class provides important - but nevertheless imperfect - protection
 * against simple attacks by casual snoopers. It is crucial to remember that
 * even encrypted data may still be susceptible to attacks, especially on rooted devices
 * <p>
 *
 * TODO: handle migration of keys
 * TODO: handle user change password
 *
 */
public class SecurePreferences implements SharedPreferences {

    //default for testing
    static SharedPreferences sFile;
    static AesCbcWithIntegrity.SecretKeys sKeys;

    private static boolean sLoggingEnabled = false;

    // links user's OnSharedPreferenceChangeListener to secure OnSharedPreferenceChangeListener
    /*
    private static HashMap<OnSharedPreferenceChangeListener, OnSharedPreferenceChangeListener>
            sOnSharedPreferenceChangeListeners;
    */

    private static final String TAG = SecurePreferences.class.getName();

    /**
     * Cycle through the unencrypt all the current prefs to mem cache, clear, then encypt with key generated from new password
     * @param newPassword
     */
    public void handlePasswordChange(String newPassword, Context context) throws GeneralSecurityException {

        AesCbcWithIntegrity.SecretKeys newKey= AesCbcWithIntegrity.generateKeyFromPassword(newPassword, getDeviceSerialNumber(context));

        Map<String, ?> allOfThePrefs = SecurePreferences.sFile.getAll();
        Map<String, String> unencryptedPrefs = new HashMap<String, String>(allOfThePrefs.size());
        Iterator<String> keys = allOfThePrefs.keySet().iterator();
        while(keys.hasNext()) {
            String prefKey = keys.next();
            Object prefValue = allOfThePrefs.get(prefKey);
            if(prefValue instanceof String){
                //all the encrypted values will be Strings
                final String prefValueString = (String)prefValue;
                final String plainTextPrefValue = decrypt(prefValueString);
                unencryptedPrefs.put(prefKey, plainTextPrefValue);
            }
        }
        destoryKeys();
        SharedPreferences.Editor editor = edit();
        editor.clear();
        editor.commit();

        sKeys = newKey;
        Iterator<String> unencryptedPrefsKeys = unencryptedPrefs.keySet().iterator();
        while (unencryptedPrefsKeys.hasNext()) {
            String prefKey = unencryptedPrefsKeys.next();
            String prefPlainText = unencryptedPrefs.get(prefKey);
            editor.putString(prefKey, encrypt(prefPlainText));
        }
        editor.commit();
    }




    /**
     * User password defaults to app generated and use the Default pref file
     *
     * @param context
     */
    public SecurePreferences(Context context) {
        this(context, null, null);
    }

    /**
     *
     * @param context
     * @param password user password/code used to generate encrpytion key.
     * @param sharedPrefFilename name of the shared pref file. If null use the default shared prefs
     */
    public SecurePreferences(Context context, final String password, final String sharedPrefFilename) {

        if (SecurePreferences.sFile == null) {
            SecurePreferences.sFile = getSharedPreferenceFile(context, sharedPrefFilename);
        }
        // Initialize or create encryption key
        if(TextUtils.isEmpty(password)) {
            try {
                final String key = SecurePreferences.generateAesKeyName(context);

                String keyAsString = SecurePreferences.sFile.getString(key, null);
                if (keyAsString == null) {
                    sKeys = AesCbcWithIntegrity.generateKey();
                    //saving new key
                    boolean commited = SecurePreferences.sFile.edit().putString(key, sKeys.toString()).commit();
                    if(!commited){
                        Log.w(TAG, "Key not committed to prefs");
                    }
                }else{
                    sKeys = AesCbcWithIntegrity.keys(keyAsString);
                }

                if(sKeys==null){
                    throw new GeneralSecurityException("Problem generating Key");
                }

            } catch (GeneralSecurityException e) {
                if (sLoggingEnabled) {
                    Log.e(TAG, "Error init:" + e.getMessage());
                }
                throw new IllegalStateException(e);
            }
        }else{
            //use the password to generate the key
            try {
                sKeys= AesCbcWithIntegrity.generateKeyFromPassword(password, getDeviceSerialNumber(context));

                if(sKeys==null){
                    throw new GeneralSecurityException("Problem generating Key From Password");
                }
            } catch (GeneralSecurityException e) {
                if (sLoggingEnabled) {
                    Log.e(TAG, "Error init using user password:" + e.getMessage());
                }
                throw new IllegalStateException(e);
            }
        }
        // initialize OnSecurePreferencesChangeListener HashMap
        /*
        sOnSharedPreferenceChangeListeners =
                new HashMap<OnSharedPreferenceChangeListener, OnSharedPreferenceChangeListener>(10);
        */
    }

    /**
     * if a prefFilename is not defined the getDefaultSharedPreferences is used.
     * @param context
     * @return
     */
    private SharedPreferences getSharedPreferenceFile(Context context, String prefFilename) {
        if(TextUtils.isEmpty(prefFilename)) {
            return PreferenceManager
                    .getDefaultSharedPreferences(context);
        }
        else{
          return context.getSharedPreferences(prefFilename, Context.MODE_PRIVATE);
        }
    }

    /**
     * nulls in memory keys and file
     */
    public void destoryKeys(){
        sKeys=null;

    }


    /**
     * Uses device and application values to generate the pref key for the encryption key
     * @param context
     * @return String to be used as the AESkey Pref key
     * @throws GeneralSecurityException if something goes wrong in generation
     */
	private static String generateAesKeyName(Context context) throws GeneralSecurityException
    {
		final String password = context.getPackageName();
		final byte[] salt = getDeviceSerialNumber(context).getBytes();
        AesCbcWithIntegrity.SecretKeys generatedKeyName = AesCbcWithIntegrity.generateKeyFromPassword(password, salt);
        if(generatedKeyName==null){
            throw new GeneralSecurityException("Key not generated");
        }

		return hashPrefKey(generatedKeyName.toString());
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
				return Settings.Secure.getString(
						context.getContentResolver(),
						Settings.Secure.ANDROID_ID);
			}else {
                return deviceSerial;
            }
		} catch (Exception ignored) {
			// default to Android_ID
			return Settings.Secure.getString(context.getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
	}


    /**
     * The Pref keys must be same each time so we're using a hash to obsurce the stored value
     * @param prefKey
     * @return
     */
    public static String hashPrefKey(String prefKey)  {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = prefKey.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);

        return Base64.encodeToString(digest.digest(), AesCbcWithIntegrity.BASE64_FLAGS);

        } catch (NoSuchAlgorithmException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "Problem generating hash", e);
            }
        } catch (UnsupportedEncodingException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "Problem generating hash", e);
            }
        }
        return null;
    }



	private static String encrypt(String cleartext) {
		if (TextUtils.isEmpty(cleartext)) {
			return cleartext;
		}
		try {
			return AesCbcWithIntegrity.encrypt(cleartext, sKeys).toString();
		} catch (GeneralSecurityException e) {
			if (sLoggingEnabled) {
				Log.w(TAG, "encrypt", e);
			}
			return null;
		} catch (UnsupportedEncodingException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "encrypt", e);
            }
        }
        return null;
    }

    /**
     *
     * @param ciphertext
     * @return decrypted plain text, unless decryption fails, in which case null
     */
    private static String decrypt(final String ciphertext) {
        if (TextUtils.isEmpty(ciphertext)) {
            return ciphertext;
        }
        try {
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(ciphertext);

            return AesCbcWithIntegrity.decryptString(cipherTextIvMac, sKeys);
        } catch (GeneralSecurityException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "decrypt", e);
            }
        } catch (UnsupportedEncodingException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "decrypt", e);
            }
        }
        return null;
    }

    /**
     *
     * @return map of with decrypted values (excluding the key if present)
     */
	@Override
	public Map<String, String> getAll() {
        //wont be null as per http://androidxref.com/5.1.0_r1/xref/frameworks/base/core/java/android/app/SharedPreferencesImpl.java
		final Map<String, ?> encryptedMap = SecurePreferences.sFile.getAll();
		final Map<String, String> decryptedMap = new HashMap<String, String>(
				encryptedMap.size());
            for (Entry<String, ?> entry : encryptedMap.entrySet()) {
                try {
                    Object cipherText = entry.getValue();
                    //don't include the key
                    if(cipherText!=null && !cipherText.equals(sKeys.toString())){
                        //the prefs should all be strings
                        decryptedMap.put(entry.getKey(),
                                SecurePreferences.decrypt(cipherText.toString()));
                    }
                } catch (Exception e) {
                    if (sLoggingEnabled) {
                        Log.w(TAG, "error during getAll", e);
                    }
                    // Ignore issues that unencrypted values and use instead raw cipher text string
                    decryptedMap.put(entry.getKey(),
                            entry.getValue().toString());
                }
            }
            return decryptedMap;
	}

	@Override
	public String getString(String key, String defaultValue) {
		final String encryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.hashPrefKey(key), null);
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
	public String getUnencryptedString(String key, String defaultValue) {
		final String nonEncryptedValue = SecurePreferences.sFile.getString(
				SecurePreferences.hashPrefKey(key), null);
		return (nonEncryptedValue != null) ? nonEncryptedValue : defaultValue;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Set<String> getStringSet(String key, Set<String> defaultValues) {
		final Set<String> encryptedSet = SecurePreferences.sFile.getStringSet(
				SecurePreferences.hashPrefKey(key), null);
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
				SecurePreferences.hashPrefKey(key), null);
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
				SecurePreferences.hashPrefKey(key), null);
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
				SecurePreferences.hashPrefKey(key), null);
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
				SecurePreferences.hashPrefKey(key), null);
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
		return SecurePreferences.sFile.contains(SecurePreferences.hashPrefKey(key));
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
			mEditor.putString(SecurePreferences.hashPrefKey(key),
					SecurePreferences.encrypt(value));
			return this;
		}

		/**
		 * This is useful for storing values that have be encrypted by something
		 * else or for testing
		 * 
		 * @param key
		 *            - encrypted as usual
		 * @param value
		 *            will not be encrypted
		 * @return
		 */
		public SharedPreferences.Editor putUnencryptedString(String key,
				String value) {
			mEditor.putString(SecurePreferences.hashPrefKey(key), value);
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
			mEditor.putStringSet(SecurePreferences.hashPrefKey(key),
					encryptedValues);
			return this;
		}

		@Override
		public SharedPreferences.Editor putInt(String key, int value) {
			mEditor.putString(SecurePreferences.hashPrefKey(key),
					SecurePreferences.encrypt(Integer.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putLong(String key, long value) {
			mEditor.putString(SecurePreferences.hashPrefKey(key),
					SecurePreferences.encrypt(Long.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putFloat(String key, float value) {
			mEditor.putString(SecurePreferences.hashPrefKey(key),
					SecurePreferences.encrypt(Float.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor putBoolean(String key, boolean value) {
			mEditor.putString(SecurePreferences.hashPrefKey(key),
					SecurePreferences.encrypt(Boolean.toString(value)));
			return this;
		}

		@Override
		public SharedPreferences.Editor remove(String key) {
			mEditor.remove(SecurePreferences.hashPrefKey(key));
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
            final OnSharedPreferenceChangeListener listener) {
        SecurePreferences.sFile
                .registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * @param listener OnSharedPreferenceChangeListener
     * @param decryptKeys Callbacks receive the "key" parameter decrypted
     */
    public void registerOnSharedPreferenceChangeListener(
            final OnSharedPreferenceChangeListener listener, boolean decryptKeys) {

        if(!decryptKeys) {
            registerOnSharedPreferenceChangeListener(listener);
            return;
        }


        // wrap user's OnSharedPreferenceChangeListener with another that decrypts key before
        // calling the onSharedPreferenceChanged callback
        /*
        OnSharedPreferenceChangeListener secureListener =
                new OnSharedPreferenceChangeListener() {
                    private OnSharedPreferenceChangeListener mInsecureListener = listener;
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                          String key) {
                        try {
                            //this doesn't work anymore as the key isn't enc, it's hashed
                            String decryptedKey = decrypt(key);
                            if(decryptedKey != null) {
                                mInsecureListener.onSharedPreferenceChanged(sharedPreferences,
                                        decryptedKey);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to decrypt key: " + key);
                        }
                    }
                };
        sOnSharedPreferenceChangeListeners.put(listener, secureListener);
        SecurePreferences.sFile
                .registerOnSharedPreferenceChangeListener(secureListener);
        */
    }

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
        /*
        if(sOnSharedPreferenceChangeListeners.containsKey(listener)) {
            OnSharedPreferenceChangeListener secureListener =
                    sOnSharedPreferenceChangeListeners.remove(listener);
            SecurePreferences.sFile
                    .unregisterOnSharedPreferenceChangeListener(secureListener);
        } else {
        */
            SecurePreferences.sFile
                    .unregisterOnSharedPreferenceChangeListener(listener);
        //}
	}
}
