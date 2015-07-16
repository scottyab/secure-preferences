package com.securepreferences.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.securepreferences.SecurePreferences;

public class TestSecurePreferences extends AndroidTestCase {

    final static String DEFAULT_KEY = "testingkeyfoo";
    final static String DEFAULT_VALUE = "testingvaluebar";

    public static final String TAG = "TestSecurePreferences";

    public static final String DEFAULT_PREFS_FILE_NAME = "com.securepreferences.test_preferences";
    public static final String MY_CUSTOM_PREFS = "my_custom_prefs";
    public static final String USER_PREFS_WITH_PASSWORD = "user_prefs_with_password";


    public TestSecurePreferences() {
        //want to make sure the pref files are wiped before we start testing
        try {
            tearDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SecurePreferences.setLoggingEnabled(true);
    }



    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        //clear down all the files that may of been created
        deletePrefFile(USER_PREFS_WITH_PASSWORD);
        deletePrefFile(DEFAULT_PREFS_FILE_NAME);
    }


    public void testKeyGeneratedCustomPrefFile() {
        final String prefFileName = generatePrefFileNameForTest();

        SecurePreferences securePrefs = new SecurePreferences(getContext(), null, prefFileName);
        SharedPreferences normalPrefs = getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

        Map<String, String> allOfTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allOfTheNormalPrefs = normalPrefs.getAll();

        assertTrue(
                "securePrefs should be empty as the key is excluded from the getAll map",
                allOfTheSecurePrefs.isEmpty());

        assertTrue(
                "The normal prefs version should contain a single entry the key",
                allOfTheNormalPrefs.size() == 1);

        //clean up here as pref file created for each test
        deletePrefFile(prefFileName);

    }


    /**
     * Test that when secure prefs created using password, that a key isn't in the prefs
     */
    public void testKeyGeneratedFromUserPassword() {
        final String prefFileName = generatePrefFileNameForTest();

        SecurePreferences securePrefs = new SecurePreferences(getContext(), "password", prefFileName);
        SharedPreferences normalPrefs = getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

        Map<String, ?> allTheSecurePrefs = securePrefs.getAll();
        Map<String, ?> allThePrefs = normalPrefs.getAll();

            assertTrue(
                    "the preference file should not contain any enteries as the key is generated via user password.",
                    allThePrefs.isEmpty());


        //clean up here as pref file created for each test
        deletePrefFile(prefFileName);
    }

    /**
     * Test if incorrect password the prefs are not decrypted
     */
    public void testIncorrectUserPassword() {
        final String key = "mysecret";
        final String value = "keepsafe";

        SecurePreferences securePrefs = new SecurePreferences(getContext(), "password", USER_PREFS_WITH_PASSWORD);
        securePrefs.edit().putString(key, value).commit();
        securePrefs=null;

        SecurePreferences securePrefsWithWrongPass = new SecurePreferences(getContext(), "incorrectpassword", USER_PREFS_WITH_PASSWORD);
        String myValue = securePrefsWithWrongPass.getString(key, null);
        if(value.equals(myValue)){
            fail("Using the wrong password, should not return the decrpyted value");
        }

    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void testSaveStringSet() {

        final String key = "fooString";
        final String value = "bar";
        final String value2 = "bar2";
        final String value3 = "bar3";

        Set<String> mySet = new HashSet<String>();
        mySet.add(value);
        mySet.add(value2);
        mySet.add(value3);

        SecurePreferences securePrefs = new SecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putStringSet(key, mySet);
        edit.commit();

        Set<String> retrievedSet = securePrefs.getStringSet(key, null);
        assertEquals(mySet, retrievedSet);

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

        deletePrefFile(MY_CUSTOM_PREFS);

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

        securePrefs.destroyKeys();

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

    /**
     * Load the pref xml file and read through to see if it has any <string tags.
     * @param prefFileName
     * @return true if contains | false if none are found
     */
    private boolean checkRawPrefFileIsEmptyOfStringEnteries(String prefFileName) throws IOException{
        String pattern =  "<string";
        File f = getPrefFile(prefFileName);

        if (f!=null && f.exists()){
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = "";
                while((line = br.readLine()) != null) {
                    if(line.contains(pattern)){
                        Log.d(TAG, "line contains " + pattern);
                        return true;
                    }
                }
        }else{
            Log.d(TAG, "File not out to search: " + prefFileName);

        }
        return false;
    }



    private String generatePrefFileNameForTest(){
        return UUID.randomUUID().toString();
    }

    private File getPrefFile(String prefFileName){
        ///data/data/com.securepreferences.test/shared_prefs;
        String sharedPrefFolderPath = getContext().getFilesDir().getParent() + "/shared_prefs";

        String prefFilePath = sharedPrefFolderPath + "/" + prefFileName + ".xml";
        File f = new File(prefFilePath);
        return f;
    }

    private void deletePrefFile(String prefFileName) {
        File f = getPrefFile(prefFileName);
        if (f!=null && f.exists()){
            boolean result = f.delete();
            if(result){
                Log.d(TAG, prefFileName+" deleted ok");
            }else{
                Log.d(TAG, prefFileName+" NOT deleted :(");
            }
        }else{
            Log.d(TAG, prefFileName+" NOT deleted as doesn't exist");
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
