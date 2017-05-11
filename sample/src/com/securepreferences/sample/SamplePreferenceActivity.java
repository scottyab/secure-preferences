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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.securepreferences.SecurePreferences;

import java.util.Map.Entry;

/**
 * Shows example of how to use secure prefs with PreferenceFragment. Note the code in the onStart and onStop.
 * With out this the preference fragment/activity will just save things unencrypted to default preferences.
 */
public class SamplePreferenceActivity extends PreferenceActivity {

    private static final String TAG = SamplePreferenceActivity.class.getSimpleName();
    private SharedPreferences mInsecurePrefs;
    private SecurePreferences mSecurePrefs;

    private CheckBoxPreference mCheckBoxPref;
    private EditTextPreference mTextPref;
    private ListPreference mListPref;


    private String checkBoxKeyHash;
    private String textKeyHash;
    private String listKeyHash;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //DefaultSharedPreferences is used by the PreferenceActivity
        mInsecurePrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //both use a different pref file
        mSecurePrefs = App.get().getSecurePreferences();

        //we need to use the hashed version of the keys to look them up (annoying i know!)
        checkBoxKeyHash = mSecurePrefs.obfuscateKeyName(getString(R.string.checkbox_key));
        textKeyHash = mSecurePrefs.obfuscateKeyName(getString(R.string.text_key));
        listKeyHash = mSecurePrefs.obfuscateKeyName(getString(R.string.list_key));

        //look up the pref with it's real key name as in the xml not the hash
        mCheckBoxPref = (CheckBoxPreference) findPreference(getString(R.string.checkbox_key));
        mTextPref = (EditTextPreference) findPreference(getString(R.string.text_key));
        mListPref = (ListPreference) findPreference(getString(R.string.list_key));


        //poplate the initial  summary with the value
        mTextPref.setSummary(mSecurePrefs.getString(getString(R.string.text_key), "Not set"));

        //TODO set preference change listener to update the summary
        mTextPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mTextPref.setSummary((String) newValue);
                return true;
            }
        });

        //poplate the initial  summary with the value
        mListPref.setSummary(mSecurePrefs.getString(getString(R.string.list_key), "Not set"));

        //TODO set preference change listener to update the summary
        mListPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mListPref.setSummary("Selected: " + (String) newValue);
                return true;
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();

        // Decrypt relevant key/value pairs, if they exist and set the values of the prefs
        // this is automatic usually, but we need to map the hashed keys to the unhashed keys in the preference.xml)
        for (Entry<String, ?> entry : mSecurePrefs.getAll().entrySet()) {
            final String key = entry.getKey();
            if (key == null) {
                continue;
            } else if (key.equals(checkBoxKeyHash)) {
                mCheckBoxPref.setChecked(mSecurePrefs.getBoolean(getString(R.string.checkbox_key), false));
            } else if (key.equals(textKeyHash)) {
                mTextPref.setText(mSecurePrefs.getString(getString(R.string.text_key), null));
            } else if (key.equals(listKeyHash)) {
                String value = mSecurePrefs.getString(getString(R.string.list_key), null);
                if (!TextUtils.isEmpty(value)) {
                    final int valueInt = Integer.parseInt(value);
                    if (valueInt != 0) {
                        mListPref.setValueIndex(valueInt - 1); // Zero based index = selection
                        // value - 1
                    }
                }
            } else {
                Log.d(TAG, "No match found for " + key);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        //because the standard PreferenceActivity deals with unencrpyted prefs, we get them and replace with encrypted version when the activity is stopped
        final Editor insecureEditor = mInsecurePrefs.edit();
        final Editor secureEditor = mSecurePrefs.edit();
        String key = getString(R.string.checkbox_key);
        if (mInsecurePrefs.contains(key)) {
            Log.d(TAG, "match found for " + key + " adding encrypted copy to secure prefs");
            //add the enc versions to the secure prefs
            secureEditor.putBoolean(key, mInsecurePrefs.getBoolean(key, false));
            //remove entry from the default/insecure prefs
            insecureEditor.remove(key);
        }
        key = getString(R.string.text_key);
        if (mInsecurePrefs.contains(key)) {
            Log.d(TAG, "match found for " + key + " adding encrypted copy to secure prefs");
            secureEditor.putString(key, mInsecurePrefs.getString(key, null));
            insecureEditor.remove(key);
        }
        key = getString(R.string.list_key);
        if (mInsecurePrefs.contains(key)) {
            Log.d(TAG, "match found for " + key + " adding encrypted copy to secure prefs");
            secureEditor.putString(key, mInsecurePrefs.getString(key, null));
            insecureEditor.remove(key);
        }

        insecureEditor.commit();
        secureEditor.commit();
    }
}
