package com.securepreferences.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.securepreferences.SecurePreferences;
import com.securepreferences.sample.utils.TickTock;

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
            TickTock tickTock = new TickTock();
            tickTock.tic();
            mSecurePrefs = new SecurePreferences(this, "", "my_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
            Log.d(TAG, "SecurePreferences init time: " + TickTock.formatDuration(tickTock.toc()));
        }
        return mSecurePrefs;
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
