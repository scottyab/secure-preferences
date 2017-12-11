package com.securepreferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for new users of the library who don't want or need to delve in to inner workings and just want to up their security
 */
public class SecurePreferenceCreator {

    private static String DEFAULT_SECURE_PREFS_FILE_NAME = "DefaultSecurePrefsFile";

    public static ObfuscatedSharedPreferences createQuickAesObfuscatedSharedPreferences(Context context) {
        return SecurePreferenceCreator.createQuickAesObfuscatedSharedPreferences(context.getSharedPreferences(DEFAULT_SECURE_PREFS_FILE_NAME, Context.MODE_PRIVATE));
    }

    public static ObfuscatedSharedPreferences createQuickAesObfuscatedSharedPreferences(SharedPreferences sharedPreferences) {
        return new ObfuscatedSharedPreferences(sharedPreferences, new HashBasedPrefKeyObfuscator());
    }

    /**
     * Key names are not obfuscated, this allows mapping to xml
     *
     * @param sharedPreferences
     * @return
     */
    public static ObfuscatedSharedPreferences createObfuscatedSharedPreferencesToUseOnPreferenceScreen(SharedPreferences sharedPreferences) {
        return new ObfuscatedSharedPreferences(sharedPreferences, new NoOpPrefKeyObfuscator());
    }
}
