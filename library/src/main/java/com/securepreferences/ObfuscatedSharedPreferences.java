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
 * This class is meant obfuscate keys and values stored in the SharedPreferences XML file. It's
 * far from not bullet proof security and only adds security by obscurity hence the name.
 * It does mean that attacker (app/person) cannot simply read the content of the prefs.xml file
 * in plain text. Likely they would need to decompile the app and figure out the what encryption
 * has been used and how the SharedPreferences Data is obfuscated.
 */
public class ObfuscatedSharedPreferences implements SharedPreferences, SecretKeyDatasource {

    private final SharedPreferences mSharedPreferences;
    private final PrefKeyObfuscator mKeyNameObfuscator;
    private final Encoder mEncoder;
    private PrefValueEncrypter mValueEncrypter;

    //this key name of Encryption is obfuscated when stored
    private final String KEY_NAME = "SECRET_KEY_NAME";

    public ObfuscatedSharedPreferences(
            SharedPreferences sharedPreferences,
            PrefKeyObfuscator keyNameObfuscator) {
        mSharedPreferences = sharedPreferences;
        mKeyNameObfuscator = keyNameObfuscator;
        mEncoder = new Encoder();

        // TODO: 11/12/2017 need to pass this in mValueEncrypter  SecurePreferenceCreator to wire things together alernative is to use dagger

        // ObfuscatedSharedPreferences implements SecretKeyDatasource to keeps the encoded
        // encryption key and encoded encrypted values in the same file. Storing in separate file
        // would just highlight where and which value is the key. 
        mValueEncrypter = new Aes128PrefValueEncrypter(mEncoder, this);
    }


    @Override
    public byte[] getKey() {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(KEY_NAME);
        String key = mSharedPreferences.getString(obfuscatedKey, null);
        return mEncoder.decode(key);
    }

    @Override
    public void saveKey(byte[] key) {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(KEY_NAME);
        String base64Key = mEncoder.encode(key);
        editUnEncrypted().putString(obfuscatedKey, base64Key).commit();
    }

    @Override
    public boolean checkKeyIsPresent() {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(KEY_NAME);
        return mSharedPreferences.contains(obfuscatedKey);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void destroyKey() {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(KEY_NAME);
        mSharedPreferences.edit().remove(obfuscatedKey).commit();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defaultValue) {
        return decryptStringValue(key, defaultValue);
    }

    private String decryptStringValue(String key, String defaultValue) {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(key);
        String cipherText = mSharedPreferences.getString(obfuscatedKey, null);
        if (cipherText == null) {
            return defaultValue;
        } else {
            try {
                return mValueEncrypter.decrypt(cipherText);
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
    }


    @Override
    public int getInt(String key, int defaultInt) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            return Integer.valueOf(stringValue);
        } else {
            return defaultInt;
        }
    }

    @Override
    public long getLong(String key, long defaultLong) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            return Long.valueOf(stringValue);
        } else {
            return defaultLong;
        }
    }

    @Override
    public float getFloat(String key, float defaultFloat) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            return Float.valueOf(stringValue);
        } else {
            return defaultFloat;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultBoolean) {
        String stringValue = decryptStringValue(key, null);
        if (stringValue != null) {
            return Boolean.getBoolean(stringValue);
        } else {
            return defaultBoolean;
        }
    }

    @Override
    public boolean contains(String key) {
        String obfuscatedKey = mKeyNameObfuscator.obfuscate(key);
        return mSharedPreferences.contains(obfuscatedKey);
    }

    @Override
    public Editor edit() {
        return new ObfuscatedEditor(editUnEncrypted());
    }

    private Editor editUnEncrypted() {
        return mSharedPreferences.edit();
    }

    @Override
    public Map<String, ?> getAll() {
        final Map<String, ?> encryptedMap = mSharedPreferences.getAll();

        final Map<String, String> decryptedMap = new HashMap<>(encryptedMap.size());

        for (Map.Entry<String, ?> entry : encryptedMap.entrySet()) {
            try {

                //don't include the key
                if (mKeyNameObfuscator.obfuscate(KEY_NAME).equals(entry.getKey())) {
                    continue;
                }

                Object cipherText = entry.getValue();
                if (cipherText != null) {
                    //the prefs should all be strings
                    decryptedMap.put(entry.getKey(), mValueEncrypter.decrypt(cipherText.toString()));
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
        final Set<String> encryptedSet = mSharedPreferences.getStringSet(mKeyNameObfuscator.obfuscate(key), null);
        if (encryptedSet == null) {
            return defaultValues;
        }
        final Set<String> decryptedSet = new HashSet<>(encryptedSet.size());
        for (String encryptedValue : encryptedSet) {
            try {
                decryptedSet.add(mValueEncrypter.decrypt(encryptedValue));
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
        return decryptedSet;
    }

    /**
     * Not supported
     */
    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        throw new UnsupportedOperationException("Not supported for ObfuscatedSharedPreferences");
    }

    /**
     * Not supported
     */
    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        throw new UnsupportedOperationException("Not supported for ObfuscatedSharedPreferences");
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
            String obfuscatedKey = mKeyNameObfuscator.obfuscate(key);
            try {
                String cipherText = mValueEncrypter.encrypt(value);

                return mEditor.putString(obfuscatedKey, cipherText);
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            final Set<String> encryptedValues = new HashSet<>(values.size());
            try {
                for (String value : values) {
                    encryptedValues.add(mValueEncrypter.encrypt(value));
                }
                mEditor.putStringSet(mKeyNameObfuscator.obfuscate(key),
                        encryptedValues);
                return this;
            } catch (GeneralSecurityException e) {
                throw new SecurityException(e);
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
            String obfuscatedKey = mKeyNameObfuscator.obfuscate(key);
            return mEditor.remove(obfuscatedKey);
        }

        @Override
        public Editor clear() {
            resetKey();
            Editor editor = mEditor.clear();
            mValueEncrypter.reset();
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

    private void resetKey() {
        mValueEncrypter.clearKeys();
        destroyKey();
    }

}
