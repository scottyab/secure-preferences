package com.securepreferences.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.securepreferences.SecurePreferences;
import com.securepreferences.Utils;
import com.securepreferences.sample.common.ErrorView;
import com.securepreferences.sample.common.LoadingView;

import java.security.GeneralSecurityException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class MainActivity extends AppCompatActivity {

    private static final int PHONE_STATE_PERM_RC = 99;

    @BindView(R.id.changeUserPrefsPasswordButton)
    Button changeUserPrefsPasswordButton;
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.errorView)
    ErrorView errorView;
    @BindView(R.id.PageContainer)
    LinearLayout PageContainer;

    private SecurePreferences secPrefs;

    private static final String SAMPLE_PREF_FOO_KEY = "Foo";
    private static final String SAMPLE_PREF_FOO_VALUE = "Bar";
    private static String githubLink = "https://github.com/scottyab/secure-preferences";
    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        secPrefs = App.get().getSecurePreferences();
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @DebugLog
    public void onGetButtonClick(View v) {
        final String value = secPrefs.getString(MainActivity.SAMPLE_PREF_FOO_KEY, null);
        toast("key:" + MainActivity.SAMPLE_PREF_FOO_KEY + ", value= " + value);
    }

    @SuppressLint("ApplySharedPref")
    @DebugLog
    public void onSetButtonClick(View v) {
        secPrefs.edit().putString(MainActivity.SAMPLE_PREF_FOO_KEY, MainActivity.SAMPLE_PREF_FOO_VALUE)
                .commit();
        String cipherText = secPrefs.getEncryptedString(MainActivity.SAMPLE_PREF_FOO_KEY, "");
        toast("Saved key:" + MainActivity.SAMPLE_PREF_FOO_KEY + " with enc value:" + cipherText);
    }

    @DebugLog
    public void onRemoveButtonClick(View v) {
        secPrefs.edit().remove(MainActivity.SAMPLE_PREF_FOO_KEY).apply();
        toast("key:" + MainActivity.SAMPLE_PREF_FOO_KEY + " removed from secure prefs");
    }

    @DebugLog
    public void onClearAllButtonClick(View v) {
        secPrefs.edit().clear().apply();
        toast("All secure prefs cleared");
    }

    private void toast(String msg) {
        Snackbar.make(PageContainer, msg, Snackbar.LENGTH_SHORT).show();
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
            showInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLoading(boolean show) {
        loadingView.show(show);
    }


    @OnClick({R.id.printFooButton, R.id.addFooButton, R.id.removeFooButton, R.id.clearSecurePrefsButton, R.id.createUserPrefsButton, R.id.changeUserPrefsPasswordButton})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.printFooButton:
                onGetButtonClick(view);
                break;
            case R.id.addFooButton:
                onSetButtonClick(view);
                break;
            case R.id.removeFooButton:
                onRemoveButtonClick(view);
                break;
            case R.id.clearSecurePrefsButton:
                onClearAllButtonClick(view);
                break;
            case R.id.createUserPrefsButton:
                showCreatePasswordDialog();
                break;
            case R.id.changeUserPrefsPasswordButton:
                showChangePasswordDialog();
                break;
        }
    }


    private void showCreatePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater li = LayoutInflater.from(this);
        View dialogContent = li.inflate(R.layout.dialog_password, null);

        final TextView messageTV = dialogContent.findViewById(R.id.message);
        final EditText passwordET = dialogContent.findViewById(R.id.passwordET);
        final TextInputLayout passwordInputLayout = dialogContent.findViewById(R.id.passwordInputLayout);

        builder.setView(dialogContent);

        builder.setTitle(R.string.set_pwd_dialog_title);

        messageTV.setText(R.string.set_pwd_dialog_msg);
        passwordET.setHint(R.string.set_pwd_dialog_password_hint);

        builder.setPositiveButton(android.R.string.ok, null); //Set to null. We override the onclick
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String pwd = passwordET.getText().toString();
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_PHONE_STATE);
                if (TextUtils.isEmpty(pwd)) {
                    passwordInputLayout.setError(getString(R.string.password_validation_error));
                } else if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    new SetupUserPrefsPasswordTask(pwd).execute();
                    passwordInputLayout.setError(null);
                    dialog.dismiss();
                } else {
                    requestReadPhoneStatePermission();
                    passwordInputLayout.setError(null);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }


    private void showChangePasswordDialog() {
        if (App.get().getPasswordBasedPrefs() == null) {
            toast("Please create password based user prefs before password change");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater li = LayoutInflater.from(this);
        View dialogContent = li.inflate(R.layout.dialog_password, null);

        final TextView messageTV = dialogContent.findViewById(R.id.message);
        final EditText passwordET = dialogContent.findViewById(R.id.passwordET);
        final TextInputLayout passwordInputLayout = dialogContent.findViewById(R.id.passwordInputLayout);

        builder.setView(dialogContent);

        builder.setTitle(R.string.change_pwd_dialog_title);
        messageTV.setText(R.string.change_pwd_dialog_msg);
        passwordET.setHint(R.string.change_pwd_dialog_new_password_hint);

        builder.setPositiveButton(R.string.change_pwd_dialog_pos, null); //Set to null. We override the onclick
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String pwd = passwordET.getText().toString();
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_PHONE_STATE);
                if (TextUtils.isEmpty(pwd)) {
                    passwordInputLayout.setError(getString(R.string.password_validation_error));
                } else if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    new ChangePasswordTask(pwd).execute();
                    passwordInputLayout.setError(null);
                    dialog.dismiss();
                } else {
                    requestReadPhoneStatePermission();
                    passwordInputLayout.setError(null);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void requestReadPhoneStatePermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERM_RC);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PHONE_STATE_PERM_RC: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("Permission granted");
                } else {
                    // permission denied, boo! but this is sample so we aren't gonna handle
                    toast("Permission denied");
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class ChangePasswordTask extends AsyncTask<String, Void, Boolean> {

        private String newPassword;

        ChangePasswordTask(String newPassword) {
            this.newPassword = newPassword;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading(true);
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                App.get().changeUserPrefPassword(newPassword, Utils.getDeviceSerialNumber(MainActivity.this));
                return true;
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            showLoading(false);
            if (result != null && result) {
                toast("User prefs password changed");
            } else {
                toast("Password change failed");
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    class SetupUserPrefsPasswordTask extends AsyncTask<String, Void, Boolean> {

        private String newPassword;

        SetupUserPrefsPasswordTask(String newPassword) {
            this.newPassword = newPassword;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading(true);
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                App.get().initPasswordBasedSecurePrefs(newPassword, Utils.getDeviceSerialNumber(MainActivity.this));
                return true;
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            showLoading(false);
            if (result != null && result) {
                toast("User prefs created");
                changeUserPrefsPasswordButton.setEnabled(true);
            } else {
                toast("User prefs was not created due to error.");
            }
        }

    }

    private void showInfoDialog() {
        AlertDialog infoDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.info_dialog_details)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setNegativeButton(R.string.info_dialog_github_button, (dialog, which) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(githubLink));
                    startActivity(i);
                    dialog.dismiss();
                })
                .create();
        infoDialog.show();
    }

}