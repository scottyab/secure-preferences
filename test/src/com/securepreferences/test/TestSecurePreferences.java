package com.securepreferences.test;

import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.securepreferences.SecurePreferences;

public class TestSecurePreferences extends AndroidTestCase {

	@Override
	protected void tearDown() throws Exception {
		SecurePreferences securePrefs = new SecurePreferences(mContext);
		Editor edit = securePrefs.edit();
		edit.clear();
		edit.commit();
		super.tearDown();
	}

	public void testKeyGenerated() {
		SecurePreferences.setLoggingEnabled(true);

		SecurePreferences securePrefs = new SecurePreferences(mContext);

		Map<String, String> prefs = securePrefs.getAll();

		assertFalse(
				"securePrefs should contain at least one entry of the enc key",
				prefs.isEmpty());
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
		secureEdit.putStringNoEncrypted(key, value);
		secureEdit.commit();

		String retrievedValue = securePrefs.getStringUnencrypted(key, null);
		assertEquals(value, retrievedValue);
	}

	public void testKeyIsEncrpyted() {
		final String key = "foo";
		final String value = "bar";

		SecurePreferences securePrefs = new SecurePreferences(mContext);
		SecurePreferences.Editor secureEdit = (SecurePreferences.Editor) securePrefs
				.edit();
		secureEdit.putStringNoEncrypted(key, value);
		secureEdit.commit();

		// the key should still be encrypted so the normal prefs should fail to
		// find 'key'
		SharedPreferences normalPrefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String retrievedValue = normalPrefs.getString(key, null);

		assertNull(value, retrievedValue);
	}

}
