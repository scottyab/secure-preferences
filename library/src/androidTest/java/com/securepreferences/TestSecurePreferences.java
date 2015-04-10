package com.securepreferences;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Path;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.ApplicationTestCase;
import android.util.Log;

public class TestSecurePreferences extends AndroidTestCase {

    final static String DEFAULT_KEY = "testingkeyfoo";
    final static String DEFAULT_VALUE = "testingvaluebar";

    public static final String TAG = "TestSecurePreferences";

    public static final String DEFAULT_PREFS_FILE_NAME = "com.securepreferences.test.prefs";
    public static final String MY_CUSTOM_PREFS = "my_custom_prefs";
    public static final String USER_PREFS_WITH_PASSWORD = "user_prefs_with_password";

    public TestSecurePreferences() {

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecurePreferences.setLoggingEnabled(true);
    }



    @Override
    protected void tearDown() throws Exception {
        super.tearDown();


        SecurePreferences.sKeys=null;
        SecurePreferences.sFile=null;
        //clear down all the files that may of been created
        deletePrefFile(USER_PREFS_WITH_PASSWORD);
        deletePrefFile(DEFAULT_PREFS_FILE_NAME);
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

    public void testKeyGeneratedCustomPrefFile() {
        //fails?
        SecurePreferences securePrefs = new SecurePreferences(getContext(), null, MY_CUSTOM_PREFS);
        SharedPreferences normalPrefs = getContext().getSharedPreferences(MY_CUSTOM_PREFS, Context.MODE_PRIVATE);

        Map<String, String> allOfTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allOfTheNormalPrefs = normalPrefs.getAll();

        assertTrue(
                "securePrefs should be empty as the key is excluded from the getAll map",
                allOfTheSecurePrefs.isEmpty());

        assertTrue(
                "The normal prefs version should contain a single entry the key",
                allOfTheNormalPrefs.size() == 1);


    }


    public void testKeyGeneratedFromUserPassword() {

         SecurePreferences securePrefs = new SecurePreferences(getContext(), "password", USER_PREFS_WITH_PASSWORD);

        SharedPreferences normalPrefs = getContext().getSharedPreferences(USER_PREFS_WITH_PASSWORD, Context.MODE_PRIVATE);

        Map<String, ?> allThePrefs = normalPrefs.getAll();

        assertTrue(
                "the securePrefs should not contain any enteries as the key is generated via user password.",
                allThePrefs.isEmpty());
    }




	public void testSaveString() {

		final String key = "fooString";
		final String value = "bar";
		SharedPreferences securePrefs = new SecurePreferences(getContext());
		Editor edit = securePrefs.edit();
		edit.putString(key, value);
		edit.commit();

		String retrievedValue = securePrefs.getString(key, null);
		assertEquals(value, retrievedValue);
	}

    public void testSaveStringInCustomPref() {

        final String key = "customfoo";
        final String value = "custombar";

        SecurePreferences securePrefs = new SecurePreferences(getContext(), null, MY_CUSTOM_PREFS);
        Editor edit = securePrefs.edit();
        edit.putString(key, value);
        edit.commit();

        String retrievedValue = securePrefs.getString(key, null);
        assertEquals(value, retrievedValue);
    }

    public void testSaveInt() {
        final String key = "fooInt";
        final int value = 12345978;
        SharedPreferences securePrefs = new SecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putInt(key, value);
        edit.commit();

        int retrievedValue = securePrefs.getInt(key, -1);

        assertEquals(value, retrievedValue);
    }

    public void testSaveFloat() {
		final String key = "foofloat";
		final float value = 0.99f;
		SharedPreferences securePrefs = new SecurePreferences(getContext());
		Editor edit = securePrefs.edit();
		edit.putFloat(key, value);
		edit.commit();

		float retrievedValue = securePrefs.getFloat(key, -1);

		assertEquals(value, retrievedValue);
	}

	public void testSaveUnencrpyted() {
		final String key = "unencryptedkey";
		final String value = "bar";

		SecurePreferences securePrefs = new SecurePreferences(getContext());
		SecurePreferences.Editor secureEdit = securePrefs
				.edit();
		secureEdit.putUnencryptedString(key, value);
		secureEdit.commit();

		String retrievedValue = securePrefs.getUnencryptedString(key, null);
		assertEquals(value, retrievedValue);
	}

	public void testKeyIsEncrpyted() {


		SecurePreferences securePrefs = new SecurePreferences(getContext());
		SecurePreferences.Editor secureEdit = securePrefs
				.edit();
		secureEdit.putUnencryptedString(DEFAULT_KEY, DEFAULT_VALUE);
		secureEdit.commit();

		// the key should still be encrypted so the normal prefs should fail to
		// find 'key'
		SharedPreferences normalPrefs = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		String retrievedValue = normalPrefs.getString(DEFAULT_KEY, null);

		assertNull(DEFAULT_VALUE, retrievedValue);

	}

    public void testDestroyKeys(){
        SecurePreferences securePrefs = new SecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putString(DEFAULT_KEY, DEFAULT_VALUE);
        edit.commit();

        securePrefs.destoryKeys();

        try {
            String retrievedValue = securePrefs.getString(DEFAULT_KEY, null);
            fail("Null pointer should be thrown not retrievedValue:" + retrievedValue);
        }catch (NullPointerException e){

        }
    }

    public void testChangeUserPassword() {
        SecurePreferences securePrefs = new SecurePreferences(getContext(), "myfirstpassword", USER_PREFS_WITH_PASSWORD);
        Editor editor = securePrefs.edit();
        final String key = "pwchgfoo";
        final String value = "pwchgbar";
        editor.putString(key,value);
        editor.commit();

        String cipherText = securePrefs.getUnencryptedString(key, null);
        try {
            securePrefs.handlePasswordChange("newPassword", getContext());
        } catch (GeneralSecurityException e) {
            fail("error changing passwd: " + e.getMessage());
        }

        String cipherTextFromNewPassword = securePrefs.getUnencryptedString(key, null);

        assertNotSame("The two cipher texts should not be the same", cipherText, cipherTextFromNewPassword);
    }



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
