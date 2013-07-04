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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.securepreferences.SecurePreferences;

public class MainActivity extends Activity {

	private SecurePreferences mSecurePrefs;

	private SharedPreferences mPrefsWithoutWraper;

	private TextView encValuesTextView;

	private static final String KEY = "Foo";
	private static final String VALUE = "Bar";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSecurePrefs = new SecurePreferences(this);
		mPrefsWithoutWraper = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		encValuesTextView = (TextView) findViewById(R.id.fooValueEncTV);
		updateEncValueDisplay();

		mPrefsWithoutWraper
				.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
					@Override
					public void onSharedPreferenceChanged(
							SharedPreferences sharedPreferences, String key) {
						updateEncValueDisplay();
					}
				});

	}

	private void updateEncValueDisplay() {

		Map<String, ?> all = mPrefsWithoutWraper.getAll();
		if (!all.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			Set<String> keys = all.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = it.next();
				builder.append("key:" + key);
				Object value = all.get(key);
				if (value instanceof String) {
					builder.append("value:" + (String) value);
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
		Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
	}

	public void onRemoveButtonClick(View v) {
		mSecurePrefs.edit().remove(MainActivity.KEY).commit();
		Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
	}

	public void onActivityButtonClick(View v) {
		startActivity(new Intent(this, OldPreferenceActivity.class));
	}

	public void onFragmentButtonClick(View v) {
		startActivity(new Intent(this, OldPreferenceActivity.class));
	}
}
