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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import hugo.weaving.DebugLog;

public class MainActivity extends ActionBarActivity {
    private SharedPreferences mSecurePrefs;

	private TextView encValuesTextView;

	private static final String KEY = "Foo";
	private static final String VALUE = "Bar";

    private static String githubLink = "https://github.com/scottyab/secure-preferences";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();

        mSecurePrefs = App.get().getSecurePreferences();
        App.get().getSharedPreferences1000();
		updateEncValueDisplay();
        mSecurePrefs
				.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
					@Override
					public void onSharedPreferenceChanged(
							SharedPreferences sharedPreferences, String key) {
						updateEncValueDisplay();
					}
				});

	}

	private void initViews() {
		encValuesTextView = (TextView) findViewById(R.id.fooValueEncTV);
	}

    private SharedPreferences getSharedPref(){
        if(mSecurePrefs==null){
            mSecurePrefs = App.get().getSecurePreferences();
        }
        return mSecurePrefs;
    }

	/**
	 * this is just for demo purposes so you can see the dumped content of the
	 * actual shared prefs file without needing a rooted device
	 */
	private void updateEncValueDisplay() {
		Map<String, ?> all = getSharedPref().getAll();
        StringBuilder builder = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date resultdate = new Date(System.currentTimeMillis());
		builder.append("updated: " +sdf.format(resultdate) + "\n");

        if (!all.isEmpty()) {

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
		}else {
            builder.append("\nEMPTY");

        }

        encValuesTextView.setText(builder.toString());
	}

	@DebugLog
	public void onGetButtonClick(View v) {
		final String value = getSharedPref().getString(MainActivity.KEY, null);
        toast(MainActivity.KEY + "'s, value= " + value);

	}

	@DebugLog
	public void onSetButtonClick(View v) {
		getSharedPref().edit().putString(MainActivity.KEY, MainActivity.VALUE)
				.commit();
		toast(MainActivity.KEY + " with enc value:" + MainActivity.VALUE
						+ ". Saved");
	}

	@DebugLog
	public void onRemoveButtonClick(View v) {
		getSharedPref().edit().remove(MainActivity.KEY).commit();
        toast("key:" + MainActivity.KEY + " removed from secure prefs");
	}

	@DebugLog
	public void onClearAllButtonClick(View v) {
		getSharedPref().edit().clear().commit();
		updateEncValueDisplay();
        toast("All secure prefs cleared");
	}

    private void toast(String msg){
        Toast.makeText(this,
                msg,
                Toast.LENGTH_SHORT).show();
    }

	public void onActivityButtonClick(View v) {
		startActivity(new Intent(this, SamplePreferenceActivity.class));
	}

	public void onFragmentButtonClick(View v) {
		// TODO: show an example with something like unified prefs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			startActivity(new Intent(this, ActivityWithPreferenceFragment.class));
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
            i.setData(Uri.parse(githubLink));
            startActivity(i);
            return true;
        }else if(id == R.id.action_create_user_prefs){
            showCreateUserPrefsDialog(false);
            return true;
        }
        else if(id == R.id.action_change_password_user_prefs){
            showCreateUserPrefsDialog(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCreateUserPrefsDialog(boolean chnagePassword) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater li = LayoutInflater.from(this);
        View dialogContent = li.inflate(R.layout.dialog_password, null);

        TextView messageTV = (TextView)dialogContent.findViewById(R.id.message);
        final EditText passwordET = (EditText)dialogContent.findViewById(R.id.passwordET);

        builder.setView(dialogContent);

        if(chnagePassword) {
            builder.setTitle("Change password");
            messageTV.setText("Assumes you've already created/loaded a user password based pref file.");
            messageTV.setHint("Enter new password");

            builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String pwd = passwordET.getText().toString();
                    if(TextUtils.isEmpty(pwd)){
                        toast("Please enter a password");
                    }else {
                        if(App.get().changeUserPrefPassword(pwd)){
                            toast("User prefs password changed");
                        }else {
                            toast("Password change failed");
                        }
                    }
                    dialog.dismiss();
                }
            });

        } else {
            builder.setTitle("Load User prefs file");
            messageTV.setText("Load or create a user password based pref file, based on password entered here.");
            messageTV.setHint("Enter password");

            builder.setPositiveButton("Create/Load", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String pwd = passwordET.getText().toString();
                    if(TextUtils.isEmpty(pwd)){
                        toast("Please enter a password");
                    }else {
                        App.get().getUserPinBasedSharedPreferences(pwd);
                        toast("User prefs loaded");
                    }
                    dialog.dismiss();
                }
            });
        }

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


        builder.create().show();
    }


}
