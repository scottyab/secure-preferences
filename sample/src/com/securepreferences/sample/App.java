package com.securepreferences.sample;

import android.app.Application;
import android.content.SharedPreferences;

import com.securepreferences.SecurePreferences;

/**
 * Sample app
 */
public class App extends Application {
    protected static App instance;
    private SecurePreferences mSecurePrefs;

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
    public SharedPreferences getSharedPreferences() {
        if(mSecurePrefs==null){
            mSecurePrefs = new SecurePreferences(this, null, "my_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
        }
        return mSecurePrefs;
    }
}
