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
 * 
 * AppIcon - http://www.iconarchive.com/show/windows-8-metro-icons-by-dakirby309/Other-Power-Lock-Metro-icon.html
 * 
 */

package com.securepreferences.sample;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.securepreferences.SecurePreferences;

public class MainActivity extends Activity {
    private SecurePreferences mSecurePrefs;
	private SharedPreferences mInSecurePrefs;

	private TextView encValuesTextView;

	private static final String KEY = "Foo";
	private static final String VALUE = "Bar";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
		initPrefs();

		updateEncValueDisplay();

		mInSecurePrefs
				.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
					@Override
					public void onSharedPreferenceChanged(
							SharedPreferences sharedPreferences, String key) {
						updateEncValueDisplay();
					}
				});

        // listen for specific keys and receive unencrypted key name on change
        mSecurePrefs
                .registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(
                            SharedPreferences sharedPreferences, String key) {
                        Toast.makeText(MainActivity.this,
                                "SecurePreference changed with key: " + key,
                                Toast.LENGTH_SHORT).show();
                    }
                },
                true);
	}

	private void initViews() {
		encValuesTextView = (TextView) findViewById(R.id.fooValueEncTV);
		TextView linkToGibhubTv = (TextView) findViewById(R.id.LinkToGithub);
		linkToGibhubTv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private void initPrefs() {
		mSecurePrefs = new SecurePreferences(this);
		SecurePreferences.setLoggingEnabled(true);
		mInSecurePrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
	}

	/**
	 * this is just for demo purposes so you can see the dumped content of the
	 * actual shared prefs file without needing a rooted device
	 */
	private void updateEncValueDisplay() {
		Map<String, ?> all = mInSecurePrefs.getAll();
		if (!all.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			Set<String> keys = all.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = it.next();
				builder.append("key:" + key);
				Object value = all.get(key);
				if (value instanceof String) {
					builder.append("\nvalue:" + (String) value);
				}
				builder.append("\n");
			}
			encValuesTextView.setText(builder.toString());
		}

	}

	public void onGetButtonClick(View v) {
		final String value = mSecurePrefs.getString(MainActivity.KEY, null);
		Toast.makeText(this, "Value = " + value, Toast.LENGTH_SHORT).show();
	}

	public void onSetButtonClick(View v) {
		mSecurePrefs.edit().putString(MainActivity.KEY, MainActivity.VALUE)
				.commit();
		Toast.makeText(
				this,
				MainActivity.KEY + " with value:" + MainActivity.VALUE
						+ ". Saved", Toast.LENGTH_SHORT).show();
	}

	public void onRemoveButtonClick(View v) {
		mSecurePrefs.edit().remove(MainActivity.KEY).commit();
		Toast.makeText(this,
				"key:" + MainActivity.KEY + " removed from secure prefs",
				Toast.LENGTH_SHORT).show();
	}

	public void onClearAllButtonClick(View v) {
		mSecurePrefs.edit().clear().commit();
		mInSecurePrefs.edit().clear().commit();
		initPrefs();
		updateEncValueDisplay();
		Toast.makeText(this, "key:" + "All secure prefs cleared",
				Toast.LENGTH_SHORT).show();
	}

	public void onActivityButtonClick(View v) {
		startActivity(new Intent(this, OldPreferenceActivity.class));
	}

	public void onFragmentButtonClick(View v) {
		// TODO: show an example with something like unified prefs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			startActivity(new Intent(this, NewPreferenceActivity.class));
		} else {
			Toast.makeText(this,
					"PreferenceFragment not support before Android 3.0",
					Toast.LENGTH_SHORT).show();
		}
	}
}
