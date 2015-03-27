package com.securepreferences;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;

public class TestSecurePreferences extends AndroidTestCase {

    final static String DEFAULT_KEY = "testingkeyfoo";
    final static String DEFAULT_VALUE = "testingvaluebar";

    public static final String TAG = "TestSecurePreferences";

    public static final String MY_CUSTOM_PREFS = "my_custom_prefs";
    public static final String USER_PREFS_WITH_PASSWORD = "user_prefs_with_password";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecurePreferences.setLoggingEnabled(true);
    }

    private void deletePrefFile(String prefFileName) {
        File f = getContext().getDatabasePath(prefFileName);
        if (f!=null && f.exists()){
            boolean result = f.delete();
            if(result){
                Log.d(TAG, prefFileName+" deleted ok");
            }else{
                Log.d(TAG, prefFileName+" NOT deleted :(");
            }
        }
    }

    public void testKeyGenerated() {
        //both use the default prefs file
        SecurePreferences securePrefs = new SecurePreferences(mContext);
        SharedPreferences normalPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        Map<String, String> allOfTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allOfTheNormalPrefs = normalPrefs.getAll();

        assertTrue(
                "securePrefs should be empty as the key is excluded from the getAll map",
                allOfTheSecurePrefs.isEmpty());

        assertTrue(
                "The normal prefs version should contain a single entry the key",
                allOfTheNormalPrefs.size() == 1);

        clearPrefs(securePrefs);
    }

    public void testKeyGeneratedCustomPrefFile() {
        //fails?
        SecurePreferences securePrefs = new SecurePreferences(mContext, null, MY_CUSTOM_PREFS);

        SharedPreferences normalPrefs = getContext().getSharedPreferences(MY_CUSTOM_PREFS, Context.MODE_PRIVATE);

        Map<String, String> allOfTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allOfTheNormalPrefs = normalPrefs.getAll();

        assertTrue(
                "securePrefs should be empty as the key is excluded from the getAll map",
                allOfTheSecurePrefs.isEmpty());

        assertTrue(
                "The normal prefs version should contain a single entry the key",
                allOfTheNormalPrefs.size() == 1);

        deletePrefFile(MY_CUSTOM_PREFS);
    }


    public void testKeyGeneratedFromUserPassword() {
        SecurePreferences securePrefs = new SecurePreferences(mContext, "password", USER_PREFS_WITH_PASSWORD);

        SharedPreferences normalPrefs = getContext().getSharedPreferences(USER_PREFS_WITH_PASSWORD, Context.MODE_PRIVATE);

        Map<String, ?> allThePrefs = normalPrefs.getAll();

        assertTrue(
                "the securePrefs should not contain any enteries as the key is generated via user password.",
                allThePrefs.isEmpty());

        deletePrefFile(USER_PREFS_WITH_PASSWORD);
    }


    public void testChangeUserPassword() {
        SecurePreferences securePrefs = new SecurePreferences(mContext, "password", USER_PREFS_WITH_PASSWORD);
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

        deletePrefFile(USER_PREFS_WITH_PASSWORD);
    }

	public void testSaveString() {
		final String key = "foo";
		final String value = "bar";
		SharedPreferences securePrefs = new SecurePreferences(mContext);
		Editor edit = securePrefs.edit();
		edit.putString(key, value);
		edit.commit();

		String retrievedValue = securePrefs.getString(key, null);
		assertEquals(value, retrievedValue);

        clearPrefs(securePrefs);
	}

    public void testSaveStringInCustomPref() {
        final String key = "customfoo";
        final String value = "custombar";

        SecurePreferences securePrefs = new SecurePreferences(mContext, null, MY_CUSTOM_PREFS);
        Editor edit = securePrefs.edit();
        edit.putString(key, value);
        edit.commit();

        String retrievedValue = securePrefs.getString(key, null);
        assertEquals(value, retrievedValue);

        clearPrefs(securePrefs);
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

    public void testSaveFloat() {
		final String key = "foo";
		final float value = 0.99f;
		SharedPreferences securePrefs = new SecurePreferences(mContext);
		Editor edit = securePrefs.edit();
		edit.putFloat(key, value);
		edit.commit();

		float retrievedValue = securePrefs.getFloat(key, -1);

		assertEquals(value, retrievedValue);

        clearPrefs(securePrefs);
	}

	public void testSaveUnencrpyted() {
		final String key = "foo";
		final String value = "bar";

		SecurePreferences securePrefs = new SecurePreferences(mContext);
		SecurePreferences.Editor secureEdit = securePrefs
				.edit();
		secureEdit.putUnencryptedString(key, value);
		secureEdit.commit();

		String retrievedValue = securePrefs.getUnencryptedString(key, null);
		assertEquals(value, retrievedValue);
        clearPrefs(securePrefs);
	}

	public void testKeyIsEncrpyted() {


		SecurePreferences securePrefs = new SecurePreferences(mContext);
		SecurePreferences.Editor secureEdit = securePrefs
				.edit();
		secureEdit.putUnencryptedString(DEFAULT_KEY, DEFAULT_VALUE);
		secureEdit.commit();

		// the key should still be encrypted so the normal prefs should fail to
		// find 'key'
		SharedPreferences normalPrefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String retrievedValue = normalPrefs.getString(DEFAULT_KEY, null);

		assertNull(DEFAULT_VALUE, retrievedValue);

        clearPrefs(securePrefs);
	}

    public void testDestroyKeys(){
        SecurePreferences securePrefs = new SecurePreferences(mContext);
        Editor edit = securePrefs.edit();
        edit.putString(DEFAULT_KEY, DEFAULT_VALUE);
        edit.commit();

        securePrefs.destoryKeys();

        try {
            String retrievedValue = securePrefs.getString(DEFAULT_KEY, null);
            fail("Null pointer should be thrown not retrievedValue:" + retrievedValue);
        }catch (NullPointerException e){

        }
        clearPrefs(securePrefs);
    }

}
