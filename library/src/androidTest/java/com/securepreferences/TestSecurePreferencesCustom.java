package com.securepreferences;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Map;

public class TestSecurePreferencesCustom extends AndroidTestCase {

    public static final String TAG = "TestSecurePreferences";

    public static final String MY_CUSTOM_PREFS = "my_custom_prefs";
    public static final String USER_PREFS_WITH_PASSWORD = "user_prefs_with_password";

    public TestSecurePreferencesCustom() {

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecurePreferences.setLoggingEnabled(true);
    }



    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();


        SecurePreferences.sKeys=null;
        SecurePreferences.sFile=null;

        //clear down all the files that may of been created
        deletePrefFile(USER_PREFS_WITH_PASSWORD);
        deletePrefFile(MY_CUSTOM_PREFS);
    }




    public void testKeyGenerated() {
        //both use the default prefs file
        SecurePreferences securePrefs = new SecurePreferences(getContext());
        SharedPreferences normalPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());

        Map<String, String> allOfTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allOfTheNormalPrefs = normalPrefs.getAll();

        assertTrue(
                "securePrefs should be empty as the key is excluded from the getAll map",
                allOfTheSecurePrefs.isEmpty());

        assertTrue(
                "The normal prefs version should contain a single entry the key",
                allOfTheNormalPrefs.size() == 1);
    }

    /*
    public void testKeyGeneratedFromUserPassword() {

         SecurePreferences securePrefs = new SecurePreferences(getContext(), "password", USER_PREFS_WITH_PASSWORD);

        SharedPreferences normalPrefs = getContext().getSharedPreferences(USER_PREFS_WITH_PASSWORD, Context.MODE_PRIVATE);

        Map<String, ?> allThePrefs = normalPrefs.getAll();

        assertTrue(
                "the securePrefs should not contain any enteries as the key is generated via user password.",
                allThePrefs.isEmpty());
    }


*/



    private void deletePrefFile(String prefFileName) {
        String sharedPrefFolderPath = "/data/data/com.securepreferences.test/shared_prefs";
        String prefFilePath = sharedPrefFolderPath + "/" + prefFileName + ".xml";
        File f = new File(prefFilePath);
        if (f!=null && f.exists()){
            boolean result = f.delete();
            if(result){
                Log.d(TAG, prefFileName+" deleted ok");
            }else{
                Log.d(TAG, prefFileName+" NOT deleted :(");
            }
        }else{
            Log.d(TAG, prefFileName+" doesn't exist");
        }
    }

    /**
     * tear down
     * @param prefs
     */
    private void clearPrefs(SharedPreferences prefs) {
        Editor edit = prefs.edit();
        //tear down
        edit.clear();
        edit.commit();
    }
}
