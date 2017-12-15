package com.securepreferences;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Wrapper class for Android's {@link SharedPreferences} interface, which adds a
 * layer of encryption to the persistent storage and retrieval of sensitive
 * key-value pairs of primitive data types.
 * <p>
 * This class obfuscate keys and encrypts the values stored in the SharedPreferences XML file.
 * It provides important - but nevertheless imperfect - protection against simple attacks by casual
 * snoopers. It is crucial to remember that even encrypted data may still be susceptible to attacks,
 * especially on rooted devices.
 * <p>
 * Recommended to use with complex user password, in which case the encryption key used will be derived
 * from the password (then kept in memory) and not stored in the file.
 * <p>
 * `SecurePreferences implements SecretKeyDatasource` to optionally allow the encoded
 * encryption key and encoded encrypted values to be stored in the same file. Storing in separate file
 * would just highlight where and which value is the key. This isn't used/needed is using Password based encryption
 * <p>
 * OnSharedPreferenceChangeListener is not supported instead use `registerOnSecurePreferenceChangeListener`
 */
public class SecurePreferences implements SharedPreferences, SecretKeyDatasource, EncryptedValueMigrator {

    private final Set<OnSecurePreferencesChangeListener> securePreferenceChangeListeners;
    private final DecryptingPreferenceChangeListener decryptingPreferenceChangeListener;

    private final SharedPreferences sharedPreferences;
    private final PrefKeyObfuscator keyNameObfuscator;
    private final Encoder encoder;
    @Nullable
    private PrefValueEncrypter prefValueEncrypter;

    //this key name of Encryption is obfuscated with the `PrefKeyObfuscator` if stored
    private final String KEY_NAME = "SECRET_KEY_NAME";

    /**
     * @param sharedPreferences
     * @param keyNameObfuscator  used to Obfuscate the key name that's stored in the prefs file
     * @param prefValueEncrypter used to encrypt and decrypt pref values. It's optional for instantiation but is needed for use. If *not* set by time get or set values a IllegalStateException will be thrown
     */
    public SecurePreferences(
            SharedPreferences sharedPreferences,
            PrefKeyObfuscator keyNameObfuscator,
            @Nullable PrefValueEncrypter prefValueEncrypter) {

        this.sharedPreferences = sharedPreferences;
        this.keyNameObfuscator = keyNameObfuscator;
        this.prefValueEncrypter = prefValueEncrypter;
        encoder = new Encoder();
        securePreferenceChangeListeners = new HashSet<>();
        decryptingPreferenceChangeListener = new DecryptingPreferenceChangeListener();
    }


    public void setValueEncrypter(PrefValueEncrypter valueEncrypter) {
        prefValueEncrypter = valueEncrypter;
    }


    private void checkInitialised() {
        if (prefValueEncrypter == null) {
            throw new IllegalStateException("PrefValueEncrypter has not been set");
        }
    }


    @Override
    public byte[] getKey() {
        String obfuscatedKey = obfuscatedKeyName(KEY_NAME);
        String key = sharedPreferences.getString(obfuscatedKey, null);
        return encoder.decode(key);
    }

    @Override
    public void saveKey(byte[] key) {
        String obfuscatedKey = obfuscatedKeyName(KEY_NAME);
        String base64Key = encoder.encode(key);
        getEditorNotEncrypted().putString(obfuscatedKey, base64Key).commit();
    }

    @Override
    public boolean checkKeyIsPresent() {
        String obfuscatedKey = obfuscatedKeyName(KEY_NAME);
        return sharedPreferences.contains(obfuscatedKey);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void destroyKey() {
        String obfuscatedKey = obfuscatedKeyName(KEY_NAME);
        sharedPreferences.edit().remove(obfuscatedKey).commit();
    }


    public String obfuscatedKeyName(String keyName) {
        return keyNameObfuscator.obfuscate(keyName);
    }


    @Nullable
    @Override
    public String getString(String key, @Nullable String defaultValue) {
        return decryptStringValue(key, defaultValue);
    }

    /**
     * Get cipher text values without attempting to decrypt. This can be useful if you've store values that are
     * already encrypted and encoded
     *
     * @return Encrypted value of the key or the defaultValue if no value exists
     */
    public String getEncryptedString(String key, String defaultValue) {
        String obfuscatedKey = keyNameObfuscator.obfuscate(key);
        return sharedPreferences.getString(obfuscatedKey, defaultValue);
    }

    private String decryptStringValue(String key, String defaultValue) {
        checkInitialised();

        String obfuscatedKey = keyNameObfuscator.obfuscate(key);
        String cipherText = sharedPreferences.getString(obfuscatedKey, null);
        if (cipherText == null) {
            return defaultValue;
        } else {
            try {
                return prefValueEncrypter.decrypt(cipherText);
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
    }

    @Override
    public int getInt(String key, int defaultInt) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new ClassCastException(e.getMessage());
            }
        } else {
            return defaultInt;
        }
    }

    @Override
    public long getLong(String key, long defaultLong) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            try {
                return Long.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new ClassCastException(e.getMessage());
            }
        } else {
            return defaultLong;
        }
    }

    @Override
    public float getFloat(String key, float defaultFloat) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            try {
                return Float.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new ClassCastException(e.getMessage());
            }
        } else {
            return defaultFloat;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultBoolean) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            return Boolean.parseBoolean(stringValue);
        } else {
            return defaultBoolean;
        }
    }

    @Override
    public boolean contains(String key) {
        String obfuscatedKey = obfuscatedKeyName(key);
        return sharedPreferences.contains(obfuscatedKey);
    }

    @Override
    public Editor edit() {
        return new ObfuscatedEditor(getEditorNotEncrypted());
    }

    private Editor getEditorNotEncrypted() {
        return sharedPreferences.edit();
    }


    private Set<String> getAllKeysExcludingEncryptionKey() {
        final Set<String> keyNames = sharedPreferences.getAll().keySet();
        keyNames.remove(obfuscatedKeyName(KEY_NAME));
        return keyNames;
    }

    /**
     * @return Map all the prefs decrypted, however keys are still using the prefKeyObfuscator
     */
    @Override
    public Map<String, String> getAll() {
        final Map<String, ?> encryptedMap = sharedPreferences.getAll();

        final Map<String, String> decryptedMap = new HashMap<>(encryptedMap.size());

        for (Map.Entry<String, ?> entry : encryptedMap.entrySet()) {
            try {

                //don't include the key
                if (obfuscatedKeyName(KEY_NAME).equals(entry.getKey())) {
                    continue;
                }

                Object cipherText = entry.getValue();
                if (cipherText != null) {
                    //the prefs should all be strings
                    decryptedMap.put(entry.getKey(), prefValueEncrypter.decrypt(cipherText.toString()));
                }
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
        return decryptedMap;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defaultValues) {
        final Set<String> encryptedSet = sharedPreferences.getStringSet(obfuscatedKeyName(key), null);
        if (encryptedSet == null) {
            return defaultValues;
        }
        final Set<String> decryptedSet = new HashSet<>(encryptedSet.size());
        for (String encryptedValue : encryptedSet) {
            try {
                decryptedSet.add(prefValueEncrypter.decrypt(encryptedValue));
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
        return decryptedSet;
    }


    public final class ObfuscatedEditor implements Editor {
        private Editor mEditor;

        ObfuscatedEditor(Editor editor) {
            mEditor = editor;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            if (value == null) {
                return remove(key);
            }
            return encryptStringValue(key, value);
        }


        private Editor encryptStringValue(String key, String value) {
            checkInitialised();

            String obfuscatedKey = obfuscatedKeyName(key);
            try {
                String cipherText = prefValueEncrypter.encrypt(value);
                return mEditor.putString(obfuscatedKey, cipherText);
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            if (values == null) {
                mEditor.remove(obfuscatedKeyName(key));
                return this;
            } else {
                final Set<String> encryptedValues = new HashSet<>();
                try {
                    for (String value : values) {
                        encryptedValues.add(prefValueEncrypter.encrypt(value));
                    }
                    mEditor.putStringSet(keyNameObfuscator.obfuscate(key),
                            encryptedValues);
                    return this;
                } catch (GeneralSecurityException e) {
                    throw new SecurityException(e);
                }
            }
        }

        @Override
        public Editor putInt(String key, int i) {
            return encryptStringValue(key, String.valueOf(i));
        }

        @Override
        public Editor putLong(String key, long l) {
            return encryptStringValue(key, String.valueOf(l));
        }

        @Override
        public Editor putFloat(String key, float v) {
            return encryptStringValue(key, String.valueOf(v));
        }

        @Override
        public Editor putBoolean(String key, boolean b) {
            return encryptStringValue(key, String.valueOf(b));
        }

        @Override
        public Editor remove(String key) {
            String obfuscatedKey = obfuscatedKeyName(key);
            return mEditor.remove(obfuscatedKey);
        }

        @Override
        public Editor clear() {
            prefValueEncrypter.clearKeys();
            Editor editor = mEditor.clear();
            return editor;
        }

        @Override
        public boolean commit() {
            return mEditor.commit();
        }

        @Override
        public void apply() {
            mEditor.apply();
        }
    }


    /**
     * Cycle through the unencrypt all the current prefs to mem cache, clear current keys and PrefValueEncrypter,
     * then encypt with newValueEncrypter
     * <p>
     * Note: the pref keys will remain the same
     *
     * @param newValueEncrypter
     */
    @SuppressLint("ApplySharedPref")
    @Override
    public void migrateValues(PrefValueEncrypter newValueEncrypter) {
        Set<String> prefKeys = getAllKeysExcludingEncryptionKey();

        Map<String, String> unencryptedPrefs = new HashMap<>(prefKeys.size());

        //iterate through the current prefs unencrypting each one
        for (String prefKey : prefKeys) {
            String value = getString(prefKey, null);
            unencryptedPrefs.put(prefKey, value);
        }

        edit().clear().commit();

        prefValueEncrypter = newValueEncrypter;

        try {
            SharedPreferences.Editor editor = getEditorNotEncrypted();
            for (String prefKey : prefKeys) {
                String unencryptedValue = unencryptedPrefs.get(prefKey);
                editor.putString(prefKey, prefValueEncrypter.encrypt(unencryptedValue));
            }
            editor.commit();
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Problem encrypting during migration", e);
        }
    }


    public void registerOnSecurePreferenceChangeListener(OnSecurePreferencesChangeListener onSecurePreferencesChangeListener) {
        if (securePreferenceChangeListeners.isEmpty()) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(decryptingPreferenceChangeListener);
        }
        securePreferenceChangeListeners.add(onSecurePreferencesChangeListener);
    }

    public void unregisterOnSecurePreferenceChangeListener(
            OnSecurePreferencesChangeListener onSecurePreferencesChangeListener) {
        securePreferenceChangeListeners.remove(onSecurePreferencesChangeListener);
        if (securePreferenceChangeListeners.isEmpty()) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(decryptingPreferenceChangeListener);
        }
    }


    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        throw new UnsupportedOperationException("Not supported, use registerOnSecurePreferenceChangeListener");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        throw new UnsupportedOperationException("Not supported, use unregisterOnSecurePreferenceChangeListener");
    }

    class DecryptingPreferenceChangeListener implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            String oldDecryptedValue = decryptOldValue(sharedPreferences, key);
            String currentDecryptedValue = getString(key, null);

            for (OnSecurePreferencesChangeListener listener : securePreferenceChangeListeners) {
                listener.onSecurePreferencesChanged(key, oldDecryptedValue, currentDecryptedValue);
            }
        }

        private String decryptOldValue(SharedPreferences sharedPreferences, String key) {
            String value = sharedPreferences.getString(obfuscatedKeyName(key), null);
            String oldDecryptedValue = null;
            if (value != null) {
                checkInitialised();
                try {
                    oldDecryptedValue = prefValueEncrypter.decrypt(value);
                } catch (GeneralSecurityException e) {
                    throw new SecurityException(e);
                }
            }
            return oldDecryptedValue;
        }

    }


    public interface OnSecurePreferencesChangeListener {
        void onSecurePreferencesChanged(String key, @Nullable String oldValue, @Nullable String newValue);
    }

}
