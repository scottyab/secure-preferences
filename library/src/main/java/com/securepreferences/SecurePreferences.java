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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.security.GeneralSecurityException;
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
 * Recommended to use with user password, in which case the key will be derived from the password and not stored in the file.
 * <p>
 * TODO: Handle OnSharedPreferenceChangeListener
 */
public class SecurePreferences implements SharedPreferences {

    private static final int ORIGINAL_ITERATION_COUNT = 10000;

    //the backing pref file
    private SharedPreferences sharedPreferences;

    private static boolean sLoggingEnabled = false;

    private static final String TAG = SecurePreferences.class.getName();

    //name of the currently loaded sharedPrefFile, can be null if default
    private String sharedPrefFilename;

    private PrefValueEncrypter prefValueEncrypter;
    private final PrefKeyObsfucator prefKeyObsfucator;


    /**
     * User password defaults to app generated password that's stores obfucated with the other preference values. Also this uses the Default shared pref file
     *
     * @param context should be ApplicationContext not Activity
     */
    public SecurePreferences(Context context) {
        this(context, "", null);
    }


    /**
     * @param context        should be ApplicationContext not Activity
     * @param iterationCount The iteration count for the keys generation
     */
    public SecurePreferences(Context context, int iterationCount) {
        this(context, "", null, iterationCount);
    }

    /**
     * @param context            should be ApplicationContext not Activity
     * @param password           user password/code used to generate encryption key.
     * @param sharedPrefFilename name of the shared pref file. If null use the default shared prefs
     */
    public SecurePreferences(Context context, final String password, final String sharedPrefFilename) {
        this(context, null, password, sharedPrefFilename, ORIGINAL_ITERATION_COUNT);
    }


    /**
     * @param context        should be ApplicationContext not Activity
     * @param iterationCount The iteration count for the keys generation
     */
    public SecurePreferences(Context context, final String password, final String sharedPrefFilename, int iterationCount) {
        this(context, null, password, sharedPrefFilename, iterationCount);
    }


    /**
     * @param context            should be ApplicationContext not Activity
     * @param secretKey          that you've generated
     * @param sharedPrefFilename name of the shared pref file. If null use the default shared prefs
     */
    public SecurePreferences(Context context, final AesCbcWithIntegrity.SecretKeys secretKey, final String sharedPrefFilename) {
        this(context, secretKey, null, sharedPrefFilename, 0);
    }

    private SecurePreferences(Context context, final AesCbcWithIntegrity.SecretKeys secretKey, final String password, final String sharedPrefFilename, int iterationCount) {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferenceFile(context, sharedPrefFilename);
        }

        prefKeyObsfucator = new HashBasedPrefKeyObsfucator();

        try {
            AesCbcWithIntegrityPrefValueEncrypter.Builder builder = AesCbcWithIntegrityPrefValueEncrypter.newBuilder();

            if (secretKey != null) {
                prefValueEncrypter = builder.withKey(secretKey).build();
            } else if (TextUtils.isEmpty(password)) {
                // Initialize or create encryption key
                final String keyNameForEncKey = generateAesKeyName(context, iterationCount);
                String keyAsString = sharedPreferences.getString(keyNameForEncKey, null);

                if (keyAsString == null) {
                    prefValueEncrypter = builder.build();
                    saveEncKeyToPrefs(keyNameForEncKey);
                } else {
                    prefValueEncrypter = builder.withStringEncodedKey(keyAsString).build();
                }
            } else {
                //use the password to generate the key
                final byte[] salt = Utils.getDeviceSerialNumber(context).getBytes();
                prefValueEncrypter = builder.withPasswordSaltAndIterationsToGenerateKey(password, salt, iterationCount).build();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private void saveEncKeyToPrefs(String keyNameForEncKey) {
        //saving new key
        boolean committed = sharedPreferences.edit().putString(keyNameForEncKey, prefValueEncrypter.getKey().toString()).commit();
        if (!committed) {
            throw new IllegalStateException("Key not committed to prefs");
        }
    }


    /**
     * if a prefFilename is not defined the getDefaultSharedPreferences is used.
     *
     * @param context should be ApplicationContext not Activity
     * @return
     */
    private SharedPreferences getSharedPreferenceFile(Context context, String prefFilename) {
        this.sharedPrefFilename = prefFilename;

        if (TextUtils.isEmpty(prefFilename)) {
            return PreferenceManager
                    .getDefaultSharedPreferences(context);
        } else {
            return context.getSharedPreferences(prefFilename, Context.MODE_PRIVATE);
        }
    }

    /**
     * nulls in memory keys
     */
    public void destroyKeys() {
        prefValueEncrypter.clearKeys();
    }


    /**
     * Uses device and application values to generate the pref key for the encryption key
     *
     * @param context        should be ApplicationContext not Activity
     * @param iterationCount The iteration count for the keys generation
     * @return String to be used as the AESkey Pref key
     * @throws GeneralSecurityException if something goes wrong in generation
     */
    private String generateAesKeyName(Context context, int iterationCount) throws GeneralSecurityException {
        final String password = context.getPackageName();
        final byte[] salt = Utils.getDeviceSerialNumber(context).getBytes();
        AesCbcWithIntegrity.SecretKeys generatedKeyName = AesCbcWithIntegrity.generateKeyFromPassword(password, salt, iterationCount);

        return obfuscateKeyName(generatedKeyName.toString());
    }


    /**
     * The Pref keys must be same each time so we're using a hash to obscure the stored value
     *
     * @param prefKey
     * @return SHA-256 Hash of the preference key
     */
    public String obfuscateKeyName(String prefKey) {
        return prefKeyObsfucator.obsfucate(prefKey);
    }


    private String encryptAndSuppress(String cleartext) {
        try {
            return encrypt(cleartext);
        } catch (GeneralSecurityException e) {
            if (sLoggingEnabled) {
                Log.e(TAG, "Suppressed encryption failed: " + e.getMessage());
            }
        }
        return null;
    }

    private String encrypt(String cleartext) throws GeneralSecurityException {
        return prefValueEncrypter.encrypt(cleartext);
    }

    private String decryptAndSuppress(final String ciphertext) {
        try {
            return decrypt(ciphertext);
        } catch (GeneralSecurityException e) {
            if (sLoggingEnabled) {
                Log.e(TAG, "Suppressed decryption failed: " + e.getMessage());
            }
        }
        return null;
    }

    private String decrypt(final String ciphertext) throws GeneralSecurityException {
        return prefValueEncrypter.decrypt(ciphertext);
    }

    /**
     * @return map of with decrypted values (excluding the key if present)
     */
    @Override
    public Map<String, String> getAll() {
        //wont be null as per http://androidxref.com/5.1.0_r1/xref/frameworks/base/core/java/android/app/SharedPreferencesImpl.java
        final Map<String, ?> encryptedMap = sharedPreferences.getAll();
        final Map<String, String> decryptedMap = new HashMap<String, String>(
                encryptedMap.size());
        for (Entry<String, ?> entry : encryptedMap.entrySet()) {
            try {
                Object cipherText = entry.getValue();
                //don't include the key
                if (cipherText != null && !cipherText.equals(prefValueEncrypter.getKey().toString())) {
                    //the prefs should all be strings
                    decryptedMap.put(entry.getKey(),
                            decryptAndSuppress(cipherText.toString()));
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
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        return (encryptedValue != null) ? decryptAndSuppress(encryptedValue) : defaultValue;
    }

    /**
     * Added to get a values as as it can be useful to store values that are
     * already encrypted and encoded
     *
     * @param key          pref key
     * @param defaultValue
     * @return Encrypted value of the key or the defaultValue if no value exists
     */
    public String getEncryptedString(String key, String defaultValue) {
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        return (encryptedValue != null) ? encryptedValue : defaultValue;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Set<String> getStringSet(String key, Set<String> defaultValues) {
        final Set<String> encryptedSet = sharedPreferences.getStringSet(
                obfuscateKeyName(key), null);
        if (encryptedSet == null) {
            return defaultValues;
        }
        final Set<String> decryptedSet = new HashSet<String>(
                encryptedSet.size());
        for (String encryptedValue : encryptedSet) {
            decryptedSet.add(decryptAndSuppress(encryptedValue));
        }
        return decryptedSet;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        if (encryptedValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(decryptAndSuppress(encryptedValue));
        } catch (NumberFormatException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        if (encryptedValue == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(decryptAndSuppress(encryptedValue));
        } catch (NumberFormatException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        if (encryptedValue == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(decryptAndSuppress(encryptedValue));
        } catch (NumberFormatException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        final String encryptedValue = sharedPreferences.getString(
                obfuscateKeyName(key), null);
        if (encryptedValue == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(decryptAndSuppress(encryptedValue));
        } catch (NumberFormatException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    @Override
    public boolean contains(String key) {
        return sharedPreferences.contains(obfuscateKeyName(key));
    }


    /**
     * Cycle through the unencrypt all the current prefs to mem cache, clear, then encypt with key generated from new password.
     * This method can be used if switching from the generated key to a key derived from user password
     * <p>
     * Note: the pref keys will remain the same as they are SHA256 hashes.
     *
     * @param newPassword
     * @param context        should be ApplicationContext not Activity
     * @param iterationCount The iteration count for the keys generation
     */
    @SuppressLint("CommitPrefEdits")
    public void handlePasswordChange(String newPassword, Context context, int iterationCount) throws GeneralSecurityException {


        final byte[] salt = Utils.getDeviceSerialNumber(context).getBytes();
        final PrefValueEncrypter newEncrypter = AesCbcWithIntegrityPrefValueEncrypter
                .newBuilder()
                .withPasswordSaltAndIterationsToGenerateKey(newPassword, salt, iterationCount)
                .build();


        Map<String, ?> allOfThePrefs = sharedPreferences.getAll();
        Map<String, String> unencryptedPrefs = new HashMap<String, String>(allOfThePrefs.size());
        //iterate through the current prefs unencrypting each one
        for (String prefKey : allOfThePrefs.keySet()) {
            Object prefValue = allOfThePrefs.get(prefKey);
            if (prefValue instanceof String) {
                //all the encrypted values will be Strings
                final String prefValueString = (String) prefValue;
                final String plainTextPrefValue = decryptAndSuppress(prefValueString);
                unencryptedPrefs.put(prefKey, plainTextPrefValue);
            }
        }

        //destroy and clear the current pref file
        destroyKeys();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        //refresh the sharedPreferences object ref: I found it was retaining old ref/values
        sharedPreferences = null;
        sharedPreferences = getSharedPreferenceFile(context, sharedPrefFilename);

        //assign new key
        this.prefValueEncrypter = newEncrypter;

        SharedPreferences.Editor updatedEditor = sharedPreferences.edit();

        //iterate through the unencryptedPrefs encrypting each one with new key
        Iterator<String> unencryptedPrefsKeys = unencryptedPrefs.keySet().iterator();
        while (unencryptedPrefsKeys.hasNext()) {
            String prefKey = unencryptedPrefsKeys.next();
            String prefPlainText = unencryptedPrefs.get(prefKey);
            updatedEditor.putString(prefKey, encryptAndSuppress(prefPlainText));
        }
        updatedEditor.commit();
    }

    public void handlePasswordChange(String newPassword, Context context) throws GeneralSecurityException {
        handlePasswordChange(newPassword, context, ORIGINAL_ITERATION_COUNT);
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
    public final class Editor implements SharedPreferences.Editor {
        private SharedPreferences.Editor mEditor;

        /**
         * Constructor.
         */
        @SuppressLint("CommitPrefEdits")
        private Editor() {
            mEditor = sharedPreferences.edit();
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            mEditor.putString(obfuscateKeyName(key),
                    encryptAndSuppress(value));
            return this;
        }

        /**
         * This is useful for storing values that have be encrypted by something
         * else or for testing
         *
         * @param key   - encrypted as usual
         * @param value will not be encrypted
         * @return
         */
        public SharedPreferences.Editor putUnencryptedString(String key,
                                                             String value) {
            mEditor.putString(obfuscateKeyName(key), value);
            return this;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public SharedPreferences.Editor putStringSet(String key,
                                                     Set<String> values) {
            final Set<String> encryptedValues = new HashSet<String>(
                    values.size());
            for (String value : values) {
                encryptedValues.add(encryptAndSuppress(value));
            }
            mEditor.putStringSet(obfuscateKeyName(key),
                    encryptedValues);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            mEditor.putString(obfuscateKeyName(key),
                    encryptAndSuppress(Integer.toString(value)));
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            mEditor.putString(obfuscateKeyName(key),
                    encryptAndSuppress(Long.toString(value)));
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            mEditor.putString(obfuscateKeyName(key),
                    encryptAndSuppress(Float.toString(value)));
            return this;
        }

        @Override
        public SharedPrefe rences.Editor putBoolean(String key, boolean value) {
            mEditor.putString(obfuscateKeyName(key),
                    encryptAndSuppress(Boolean.toString(value)));
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            mEditor.remove(obfuscateKeyName(key));
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
            mEditor.apply();
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
        sharedPreferences
                .registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * @param listener    OnSharedPreferenceChangeListener
     * @param decryptKeys Callbacks receive the "key" parameter decrypted
     */
    public void registerOnSharedPreferenceChangeListener(
            final OnSharedPreferenceChangeListener listener, boolean decryptKeys) {

        if (!decryptKeys) {
            registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
