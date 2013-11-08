/*
 * Copyright (C) 2013, Daniel Abraham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.securepreferences.sample;

import java.util.Map.Entry;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.securepreferences.SecurePreferences;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NewPreferenceFragment extends PreferenceFragment {

	private SharedPreferences mInsecurePrefs;
	private SharedPreferences mSecurePrefs;

	private CheckBoxPreference mCheckBox;
	private EditTextPreference mText;
	private ListPreference mList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		mInsecurePrefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		mSecurePrefs = new SecurePreferences(getActivity());

		mCheckBox = (CheckBoxPreference) findPreference(getString(R.string.checkbox_key));
		mText = (EditTextPreference) findPreference(getString(R.string.text_key));
		mList = (ListPreference) findPreference(getString(R.string.list_key));
	}

	@Override
	public void onStart() {
		super.onStart();

		// Decrypt relevant key/value pairs, if they exist
		for (Entry<String, ?> entry : mSecurePrefs.getAll().entrySet()) {
			final String key = entry.getKey();
			if (key == null) {
				continue;
			} else if (key.equals(getString(R.string.checkbox_key))) {
				mCheckBox.setChecked(mSecurePrefs.getBoolean(key, false));
			} else if (key.equals(getString(R.string.text_key))) {
				mText.setText(mSecurePrefs.getString(key, null));
			} else if (key.equals(getString(R.string.list_key))) {
				final int value = Integer.parseInt(mSecurePrefs.getString(key,
						null));
				mList.setValueIndex(value - 1); // Zero based index = selection
												// value - 1
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		// Replace unencrypted key/value pairs with encrypted ones
		final Editor insecureEditor = mInsecurePrefs.edit();
		final Editor secureEditor = mSecurePrefs.edit();
		String key = getString(R.string.checkbox_key);
		if (mInsecurePrefs.contains(key)) {
			secureEditor.putBoolean(key, mInsecurePrefs.getBoolean(key, false));
			insecureEditor.remove(key);
		}
		key = getString(R.string.text_key);
		if (mInsecurePrefs.contains(key)) {
			secureEditor.putString(key, mInsecurePrefs.getString(key, null));
			insecureEditor.remove(key);
		}
		key = getString(R.string.list_key);
		if (mInsecurePrefs.contains(key)) {
			secureEditor.putString(key, mInsecurePrefs.getString(key, null));
			insecureEditor.remove(key);
		}
		insecureEditor.commit();
		secureEditor.commit();
	}
}
