package com.securepreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.security.GeneralSecurityException;

/**
 * Helper class for new users of the library who don't want or need to delve in to inner workings and just want to get started quickly
 */
public class SecurePreferenceCreator {

    public static int ITERATION_COUNT_QUICK_LESS_SECURE = 100;
    public static int ITERATION_COUNT_MEDIUM = 500;
    public static int ITERATION_COUNT_STRONGER_SLOWER = 10_000;

    /**
     * Wraps the sharedPreferences in SecurePreferences with default AES 128 encrypter and hash based key Obfuscator
     */
    public static SecurePreferences createQuickAesSecurePreferences(SharedPreferences sharedPreferences) {
        return createSecurePreferences(sharedPreferences, new HashBasedPrefKeyObfuscator());
    }

    /**
     * Creates SecurePreferences using default shared preferences file with default AES 128 encrypter and hash based key Obfuscator
     *
     * @param context should be ApplicationContext not Activity
     */
    public static SecurePreferences createQuickAesSecurePreferences(Context context) {
        return SecurePreferenceCreator.createQuickAesSecurePreferences(getSharedPreference(context, null));
    }

    /**
     * @param context      should be ApplicationContext not Activity
     * @param prefFilename name of the shared pref file. If empty the default shared prefs is used
     */
    public static SecurePreferences createQuickAesSecurePreferences(Context context, @Nullable String prefFilename) {
        return SecurePreferenceCreator.createQuickAesSecurePreferences(getSharedPreference(context, prefFilename));
    }

    /**
     * Key names are not obfuscated, this allows easy mapping to xml, however it's less secure as attackers would know what the encrypted values are
     *
     * @param sharedPreferences
     * @return
     */
    public static SecurePreferences createSecurePreferencesToUseOnPreferenceScreen(SharedPreferences sharedPreferences) {
        return createSecurePreferences(sharedPreferences, new NoOpPrefKeyObfuscator());
    }

    private static SecurePreferences createSecurePreferences(SharedPreferences sharedPreferences, PrefKeyObfuscator keyObfuscator) {
        SecurePreferences prefs = new SecurePreferences(sharedPreferences, keyObfuscator, null);
        Aes128PrefValueEncrypter valueEncrypter = new Aes128PrefValueEncrypter(new Encoder(), prefs);
        prefs.setValueEncrypter(valueEncrypter);
        return prefs;
    }

    private static SharedPreferences getSharedPreference(Context context, @Nullable String prefFilename) {
        if (TextUtils.isEmpty(prefFilename)) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            return context.getSharedPreferences(prefFilename, Context.MODE_PRIVATE);
        }
    }


    /**
     * Should be called Async as generating the PBE encryption key is time consuming depending on device and iteration count
     *
     * @param context        should be ApplicationContext not Activity
     * @param password       user entered password or passcode which is used to generate encryption key.
     * @param salt           salt used in the encryption key generation, needs to be same each time.
     * @param iterationCount used as part of password based encryption key generation larger the number the more secure but also the longer it takes
     * @param prefFilename   name of the shared pref file. If empty the default shared prefs is used
     * @throws GeneralSecurityException if there's an issue
     */
    public static SecurePreferences createPasswordBasedSecurePreferences(Context context, String password, byte[] salt, int iterationCount, @Nullable String prefFilename) throws GeneralSecurityException {
        Encoder encoder = new Encoder();
        AesCbcWithIntegrityPrefValueEncrypter pbeEncrypter = AesCbcWithIntegrityPrefValueEncrypter.builder()
                .withEncoder(encoder)
                .withPasswordSaltAndIterationsToGenerateKey(password, salt, iterationCount)
                .build();
        return new SecurePreferences(getSharedPreference(context, prefFilename), new HashBasedPrefKeyObfuscator(), pbeEncrypter);
    }

    /**
     * Uses serial number or Settings.Secure.ANDROID_ID if not available for the salt requires Read Phone state permission
     * Should be called Async as generating the PBE encryption key is time consuming depending on device and iteration count
     *
     * @param context      should be ApplicationContext not Activity
     * @param password     user entered password or passcode which is used to generate encryption key.
     * @param prefFilename name of the shared pref file. If empty the default shared prefs is used
     * @return
     * @throws GeneralSecurityException
     */
    public static SecurePreferences createPasswordBasedSecurePreferences(Context context, String password, @Nullable String prefFilename) throws GeneralSecurityException {
        byte[] salt = Utils.deviceId(context).getBytes();
        return createPasswordBasedSecurePreferences(context, password, salt, ITERATION_COUNT_MEDIUM, prefFilename);
    }


    /**
     * @param context      should be ApplicationContext not Activity
     * @param secretKey    that you've generated
     * @param prefFilename name of the shared pref file.  If empty the default shared prefs is used
     * @return
     * @throws GeneralSecurityException
     */
    public static SecurePreferences createSecurePreferencesWithKey(Context context, AesCbcWithIntegrity.SecretKeys secretKey, @Nullable String prefFilename) throws GeneralSecurityException {
        Encoder encoder = new Encoder();
        AesCbcWithIntegrityPrefValueEncrypter pbeEncrypter = AesCbcWithIntegrityPrefValueEncrypter.builder()
                .withEncoder(encoder)
                .withKey(secretKey)
                .build();
        return new SecurePreferences(getSharedPreference(context, prefFilename), new HashBasedPrefKeyObfuscator(), pbeEncrypter);
    }

}
