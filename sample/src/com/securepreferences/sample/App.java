package com.securepreferences.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.securepreferences.SecurePreferences;
import com.securepreferences.sample.utils.TickTock;
import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.security.GeneralSecurityException;

import hugo.weaving.DebugLog;

/**
 * Sample app
 */
public class App extends Application {


    private static final String TAG = "secureprefsample";
    protected static App instance;
    private SecurePreferences mSecurePrefs;
    private SecurePreferences mUserPrefs;
    public App(){
        super();
        instance = this;
    }
    public static App get() {
        return instance;
    }

    /**
     * Single point for the app to get the secure prefs object
     * @return
     */
    @DebugLog
    public SharedPreferences getSharedPreferences() {
        if(mSecurePrefs==null){
            mSecurePrefs = new SecurePreferences(this, "", "my_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
        }
        return mSecurePrefs;
    }


    /**
     * This is just an example of how you might want to create your own key with less iterations 1,000 rather than default 10,000. This makes it quicker but less secure.
     * @return
     */
    @DebugLog
    public SharedPreferences getSharedPreferences1000() {
        try {
            AesCbcWithIntegrity.SecretKeys myKey = AesCbcWithIntegrity.generateKeyFromPassword(Build.SERIAL,AesCbcWithIntegrity.generateSalt(),1000);
            SharedPreferences securePrefs1000 = new SecurePreferences(this, myKey, "my_prefs_1000.xml");
            return securePrefs1000;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to create custom key for SecurePreferences", e);
        }
        return null;
    }

    @DebugLog
    public SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }


    @DebugLog
    public SecurePreferences getUserPinBasedSharedPreferences(String password){
        if(mUserPrefs==null) {
            mUserPrefs = new SecurePreferences(this, password, "user_prefs.xml");
        }
        return mUserPrefs;
    }

    @DebugLog
    public boolean changeUserPrefPassword(String newPassword){
        if(mUserPrefs!=null){
            try {
                mUserPrefs.handlePasswordChange(newPassword, this);
                return true;
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Error during password change", e);
            }
        }
        return false;
    }
}
