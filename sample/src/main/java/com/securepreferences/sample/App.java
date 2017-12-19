package com.securepreferences.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.facebook.stetho.Stetho;
import com.securepreferences.AesCbcWithIntegrityPrefValueEncrypter;
import com.securepreferences.SecurePreferenceCreator;
import com.securepreferences.SecurePreferences;

import java.security.GeneralSecurityException;

import hugo.weaving.DebugLog;

public class App extends Application {

    private static final String TAG = "secureprefsample";
    protected static App instance;

    private SecurePreferences securePrefs;
    private SecurePreferences securePrefsFromPassword;

    public App() {
        super();
        instance = this;
    }

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        initSecurePrefs();
    }


    @DebugLog
    public void initPasswordBasedSecurePrefs(String password, String deviceSerialNumber) throws GeneralSecurityException {
        securePrefsFromPassword = SecurePreferenceCreator.createPasswordBasedSecurePreferences(
                this, password, deviceSerialNumber.getBytes(), SecurePreferenceCreator.ITERATION_COUNT_STRONGER_SLOWER, "user_prefs.xml");
    }

    public SecurePreferences getPasswordBasedPrefs() {
        return securePrefsFromPassword;
    }

    @DebugLog
    private void initSecurePrefs() {
        securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(this, "my_secure_prefs.xml");
    }

    /**
     * Single point for the app to get the secure prefs object
     *
     * @return
     */
    public SecurePreferences getSecurePreferences() {
        return securePrefs;
    }

    @DebugLog
    public SharedPreferences getDefaultNotSecureSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * example of changing the password used for the
     *
     * @param newPassword
     * @param salt        used with password to derive a key
     * @throws GeneralSecurityException
     */
    @DebugLog
    public void changeUserPrefPassword(String newPassword, String salt) throws GeneralSecurityException {
        if (securePrefsFromPassword != null) {
            AesCbcWithIntegrityPrefValueEncrypter aesCbcWithIntegrityPrefValueEncrypter
                    = AesCbcWithIntegrityPrefValueEncrypter.builder()
                    .withPasswordSaltAndIterationsToGenerateKey(newPassword,
                            salt.getBytes(),
                            SecurePreferenceCreator.ITERATION_COUNT_QUICK_LESS_SECURE
                    ).build();

            securePrefsFromPassword.migrateValues(aesCbcWithIntegrityPrefValueEncrypter);
        } else {
            throw new IllegalStateException("securePrefsFromPassword has not been initialised.");
        }
    }
}
