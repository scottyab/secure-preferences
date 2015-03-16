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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.securepreferences.SecurePreferences;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private SecurePreferences mSecurePrefs;
	private SharedPreferences mInSecurePrefs;

	private TextView encValuesTextView;

	private static final String KEY = "Foo";
	private static final String VALUE = "Bar";

    private static String GITHUB_LINK = "https://github.com/scottyab/secure-preferences";

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
                        toast("SecurePreference changed with key: " + key);
                    }
                },
                true);
	}

	private void initViews() {
		encValuesTextView = (TextView) findViewById(R.id.fooValueEncTV);
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
				builder.append("prefkey:" + key);
				Object value = all.get(key);
				if (value instanceof String) {
					builder.append("\nprefvalue:" + (String) value);
				}
				builder.append("\n\n");
			}
			encValuesTextView.setText(builder.toString());
		}

	}

	public void onGetButtonClick(View v) {
		final String value = mSecurePrefs.getString(MainActivity.KEY, null);
        toast(MainActivity.KEY +"'s, value= " + value);

	}

	public void onSetButtonClick(View v) {
		mSecurePrefs.edit().putString(MainActivity.KEY, MainActivity.VALUE)
				.commit();
		toast(MainActivity.KEY + " with enc value:" + MainActivity.VALUE
						+ ". Saved");
	}

	public void onRemoveButtonClick(View v) {
		mSecurePrefs.edit().remove(MainActivity.KEY).commit();
        toast("key:" + MainActivity.KEY + " removed from secure prefs");
	}

	public void onClearAllButtonClick(View v) {
		mSecurePrefs.edit().clear().commit();
		mInSecurePrefs.edit().clear().commit();
		initPrefs();
		updateEncValueDisplay();
        toast("All secure prefs cleared");
	}

    private void toast(String msg){
        Toast.makeText(this,
                msg,
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
			toast("PreferenceFragment not support before Android 3.0");
		}
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_github) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(GITHUB_LINK));
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
