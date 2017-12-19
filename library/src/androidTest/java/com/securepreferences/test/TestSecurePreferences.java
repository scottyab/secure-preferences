package com.securepreferences.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.securepreferences.AesCbcWithIntegrityPrefValueEncrypter;
import com.securepreferences.SecurePreferenceCreator;
import com.securepreferences.SecurePreferences;
import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//todo needs refactoring to use Testing Support library
public class TestSecurePreferences extends AndroidTestCase {

    private final static String DEFAULT_KEY = "testingkeyfoo";
    private final static String DEFAULT_VALUE = "testingvaluebar";

    private static final String TAG = "TestSecurePreferences";

    private static final String DEFAULT_PREFS_FILE_NAME = "com.securepreferences.test_preferences";
    private static final String MY_CUSTOM_PREFS = "my_custom_prefs";
    private static final String USER_PREFS_WITH_PASSWORD = "user_prefs_with_password";


    public TestSecurePreferences() {
        //want to make sure the pref files are wiped before we start testing
        try {
            tearDown();
        } catch (Exception e) {
            Log.d(TAG, "Exception in teamDown ", e);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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

        SecurePreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(getContext(), prefFileName);

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


        SecurePreferences securePrefs = null;
        try {
            securePrefs = SecurePreferenceCreator.createPasswordBasedSecurePreferences(getContext(), "password", prefFileName);
            SharedPreferences normalPrefs = getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

            Map<String, ?> allTheSecurePrefs = securePrefs.getAll();
            Map<String, ?> allThePrefs = normalPrefs.getAll();

            assertTrue(
                    "the preference file should not contain any enteries as the key is generated via user password.",
                    allThePrefs.isEmpty());


            //clean up here as pref file created for each test
            deletePrefFile(prefFileName);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    public void testShouldFailToDecryptIfIncorrectPassword() {
        final String key = "mysecret";
        final String value = "keepsafe";

        SecurePreferences securePrefs = null;
        try {
            securePrefs = SecurePreferenceCreator.createPasswordBasedSecurePreferences(getContext(), "password", USER_PREFS_WITH_PASSWORD);
            securePrefs.edit().putString(key, value).commit();
            securePrefs = null;

            SecurePreferences securePrefsWithWrongPass = SecurePreferenceCreator.createPasswordBasedSecurePreferences(getContext(), "incorrectpassword", USER_PREFS_WITH_PASSWORD);
            String myValue = securePrefsWithWrongPass.getString(key, null);

            if (value.equals(myValue)) {
                fail("Using the wrong password, should not return the decrpyted value");
            }

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }


    public void testSuccessfullySavesStringSet() {

        final String key = "TEST_STRING_SET_KEY";
        final String value = "bar";
        final String value2 = "bar2";
        final String value3 = "bar3";

        Set<String> mySet = new HashSet<String>();
        mySet.add(value);
        mySet.add(value2);
        mySet.add(value3);

        SecurePreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        Editor edit = securePrefs.edit();
        edit.putStringSet(key, mySet);
        edit.commit();

        Set<String> retrievedSet = securePrefs.getStringSet(key, null);
        assertEquals(mySet, retrievedSet);

    }

    public void testSuccessfullySaveString() {

        final String key = "fooString";
        final String value = "bar";
        SharedPreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        Editor edit = securePrefs.edit();
        edit.putString(key, value);
        edit.commit();

        String retrievedValue = securePrefs.getString(key, null);
        assertEquals(value, retrievedValue);
    }

    public void testSaveStringInCustomPref() {

        final String key = "customfoo";
        final String value = "custombar";

        SecurePreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(getContext(), MY_CUSTOM_PREFS);
        Editor edit = securePrefs.edit();
        edit.putString(key, value);
        edit.commit();

        String retrievedValue = securePrefs.getString(key, null);
        assertEquals(value, retrievedValue);

        deletePrefFile(MY_CUSTOM_PREFS);

    }

    public void testSuccessfullySaveInt() {
        final String key = "fooInt";
        final int value = 12345978;
        SharedPreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putInt(key, value);
        edit.commit();

        int retrievedValue = securePrefs.getInt(key, -1);

        assertEquals(value, retrievedValue);
    }

    public void testSuccessfullySaveFloat() {
        final String key = "foofloat";
        final float value = 0.99f;
        SharedPreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putFloat(key, value);
        edit.commit();

        float retrievedValue = securePrefs.getFloat(key, -1);

        assertEquals(value, retrievedValue);
    }

    public void testDestroyKeys(){
        SecurePreferences securePrefs = SecurePreferenceCreator.createQuickAesSecurePreferences(getContext());
        Editor edit = securePrefs.edit();
        edit.putString(DEFAULT_KEY, DEFAULT_VALUE);
        edit.commit();

        securePrefs.destroyKey();
        try {
            String retrievedValue = securePrefs.getString(DEFAULT_KEY, null);
            fail("Null pointer should be thrown not retrievedValue:" + retrievedValue);
        } catch (IllegalStateException e) {
            //expected IllegalStateException
        }
    }

    public void testSupplyOwnKeys() {
        try {
            AesCbcWithIntegrity.SecretKeys mykeys = AesCbcWithIntegrity.generateKey();

            SecurePreferences securePrefs = SecurePreferenceCreator.createSecurePreferencesWithKey(getContext(), mykeys, "my-key-file");
            Editor edit = securePrefs.edit();
            edit.putString(DEFAULT_KEY, DEFAULT_VALUE);
            edit.commit();

            String retrievedValue = securePrefs.getString(DEFAULT_KEY, null);

            assertEquals(DEFAULT_VALUE, retrievedValue);

        } catch (GeneralSecurityException e) {
            Log.d(TAG, "GeneralSecurityException in testSupplyOwnKeys ", e);
            fail("Error generating a key");
        }
    }


    public void testUserPasswordBasedPrefGenerateSameKeyFromSamePassword() {
        final String FIRST_PASSWORD = "myfirstpassword";
        SecurePreferences securePrefs = null;
        try {
            securePrefs = SecurePreferenceCreator.createPasswordBasedSecurePreferences(getContext(), FIRST_PASSWORD, USER_PREFS_WITH_PASSWORD);

            Editor editor = securePrefs.edit();
            final String key = "pwchgfoo";
            final String value = "pwchgbar";
            editor.putString(key, value);
            editor.commit();

            String valueFromPrefs = securePrefs.getString(key, null);

            //get another secure prefs using the same password.
            SecurePreferences securePrefs2 = SecurePreferenceCreator.createPasswordBasedSecurePreferences(getContext(), FIRST_PASSWORD, USER_PREFS_WITH_PASSWORD);

            String valueFromPrefs2 = securePrefs2.getString(key, null);

            assertEquals("Both decrypted values should be the same", valueFromPrefs, valueFromPrefs2);
            assertEquals("Decrypted value should match the original value", value, valueFromPrefs2);

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }


    public void testChangeUserPassword() {
        final String key = "FOO_PASS_CHANGE_KEY";
        final String value = "FOO_PASS_CHANGE_VALUE";
        final String FIRST_PASSWORD = "myfirstpassword";
        final String NEW_PASSWORD = "newPassword";
        final byte[] SALT = new byte[]{0x00, 0x01, 0x02, 0x03};

        try {
            SecurePreferences securePrefs = SecurePreferenceCreator.createPasswordBasedSecurePreferences(
                    getContext(), FIRST_PASSWORD, SALT, SecurePreferenceCreator.ITERATION_COUNT_QUICK_LESS_SECURE,
                    USER_PREFS_WITH_PASSWORD);
            Editor editor = securePrefs.edit();

            editor.putString(key, value);
            editor.commit();

            String cipherText = securePrefs.getEncryptedString(key, null);

            AesCbcWithIntegrityPrefValueEncrypter aesCbcWithIntegrityPrefValueEncrypter
                    = AesCbcWithIntegrityPrefValueEncrypter.builder()
                    .withPasswordSaltAndIterationsToGenerateKey(NEW_PASSWORD, SALT,
                            SecurePreferenceCreator.ITERATION_COUNT_QUICK_LESS_SECURE).build();

            securePrefs.migrateValues(aesCbcWithIntegrityPrefValueEncrypter);


            String cipherTextFromNewPassword = securePrefs.getEncryptedString(key, null);
            String valueFromNewPassword = securePrefs.getString(key, null);


            assertNotNull("Cipher Text for key: " + key + " should not be null", cipherTextFromNewPassword);

            assertNotSame("The two cipher texts should not be the same", cipherText, cipherTextFromNewPassword);

            assertEquals(value, valueFromNewPassword);

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
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
            while ((line = br.readLine()) != null) {
                if (line.contains(pattern)) {
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
        return new File(prefFilePath);
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
