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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.securepreferences.SecurePreferences;
import com.securepreferences.sample.R;

public class MainActivity extends Activity {

	private static final String KEY = "Foo";
	private static final String VALUE = "Bar";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onGetButtonClick(View v) {
		final String value = new SecurePreferences(this).getString(
				MainActivity.KEY, null);
		Toast.makeText(this, "Value = " + value, Toast.LENGTH_SHORT).show();
	}

	public void onSetButtonClick(View v) {
		new SecurePreferences(this).edit()
				.putString(MainActivity.KEY, MainActivity.VALUE).commit();
		Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
	}

	public void onRemoveButtonClick(View v) {
		new SecurePreferences(this).edit().remove(MainActivity.KEY).commit();
		Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
	}

	public void onActivityButtonClick(View v) {
		startActivity(new Intent(this, OldPreferenceActivity.class));
	}

	public void onFragmentButtonClick(View v) {
		startActivity(new Intent(this, OldPreferenceActivity.class));
	}
}
