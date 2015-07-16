package com.securepreferences.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.securepreferences.SecurePreferences;
import com.securepreferences.sample.utils.TickTock;

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
            TickTock tickTock = new TickTock();
            tickTock.tic();
            mSecurePrefs = new SecurePreferences(this, "", "my_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
            Log.d("securepref", "SecurePreferences init time: " + TickTock.formatDuration(tickTock.toc()));
        }
        return mSecurePrefs;
    }
}
