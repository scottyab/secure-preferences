package com.securepreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

public class Utils {

    public static boolean isEmpty(String text) {
        return TextUtils.isEmpty(text);
    }

    /**
     * Gets the hardware serial number of this device. Requires the READ_PHONE_STATE permission
     *
     * @return serial number or Settings.Secure.ANDROID_ID if not available.
     */
    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getDeviceSerialNumber(Context context) {
        // We're using the Reflection API because Build.SERIAL is only available
        // since API Level 9 (Gingerbread, Android 2.3).
        try {
            String deviceSerial = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                deviceSerial = (String) Build.class.getField("SERIAL").get(
                        null);
            } else {
                deviceSerial = Build.getSerial();
            }

            if (TextUtils.isEmpty(deviceSerial)) {
                return deviceId(context);
            } else {
                return deviceSerial;
            }
        } catch (Exception ignored) {
            // Fall back  to Android_ID
            return deviceId(context);
        }
    }

    /**
     * The Apk Signature is consistent for the life of the app.
     *
     * @return Signature of the first APK Signature
     */
    public static byte[] getAppSignature(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                return signature.toByteArray();
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }



    @SuppressLint("HardwareIds")
    /**
     *  @return Secure.ANDROID_ID (from API26 this is scoped per app sig, user and device.
     */
    public static String deviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }
}
