package com.securepreferences;

import java.io.File;
import java.util.Map;

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
	protected void tearDown() throws Exception {
		SecurePreferences securePrefs = new SecurePreferences(mContext);
		Editor edit = securePrefs.edit();
		edit.clear();
		edit.commit();
		super.tearDown();
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
		SecurePreferences.setLoggingEnabled(true);

		SecurePreferences securePrefs = new SecurePreferences(mContext);

		Map<String, String> prefs = securePrefs.getAll();

		assertFalse(
				"securePrefs should contain at least one entry of the enc key",
				prefs.isEmpty());
	}

    public void testKeyGeneratedCustomPrefFile() {
        SecurePreferences.setLoggingEnabled(true);

        SecurePreferences securePrefs = new SecurePreferences(mContext, null, MY_CUSTOM_PREFS);

        Map<String, String> prefs = securePrefs.getAll();

        assertFalse(
                "securePrefs should contain at least one entry of the enc key",
                prefs.isEmpty());
        deletePrefFile(MY_CUSTOM_PREFS);

    }


    public void testKeyGeneratedFromUserPassword() {
        SecurePreferences.setLoggingEnabled(true);

        SecurePreferences securePrefs = new SecurePreferences(mContext, "password", USER_PREFS_WITH_PASSWORD);

        Map<String, String> prefs = securePrefs.getAll();

        assertTrue(
                "securePrefs should not contain a key as it's a generated via user password.",
                prefs.isEmpty());

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
	}

	public void testSaveUnencrpyted() {
		final String key = "foo";
		final String value = "bar";

		SecurePreferences securePrefs = new SecurePreferences(mContext);
		SecurePreferences.Editor secureEdit = (SecurePreferences.Editor) securePrefs
				.edit();
		secureEdit.putUnencryptedString(key, value);
		secureEdit.commit();

		String retrievedValue = securePrefs.getUnencryptedString(key, null);
		assertEquals(value, retrievedValue);
	}

	public void testKeyIsEncrpyted() {


		SecurePreferences securePrefs = new SecurePreferences(mContext);
		SecurePreferences.Editor secureEdit = (SecurePreferences.Editor) securePrefs
				.edit();
		secureEdit.putUnencryptedString(DEFAULT_KEY, DEFAULT_VALUE);
		secureEdit.commit();

		// the key should still be encrypted so the normal prefs should fail to
		// find 'key'
		SharedPreferences normalPrefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String retrievedValue = normalPrefs.getString(DEFAULT_KEY, null);

		assertNull(DEFAULT_VALUE, retrievedValue);
	}

    public void testDestroyKeys(){
        SecurePreferences securePrefs = new SecurePreferences(mContext);
        Editor edit = securePrefs.edit();
        edit.putString(DEFAULT_KEY, DEFAULT_VALUE);
        edit.commit();

        securePrefs.destoryKeys();

        String retrievedValue = securePrefs.getString(DEFAULT_KEY, null);


    }

}
