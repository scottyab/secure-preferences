package com.securepreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

public class Utils {

    /**
     * This method is here for backwards compatibility reasons. Recommend supplying your own Salt
     *
     * @param context
     * @return Consistent between app restarts, device restarts, factory resets,
     * however cannot be guaranteed on OS updates.
     */
    @SuppressLint("MissingPermission")
    static String getDefaultSalt(Context context) {

        //Android Q removes all access to Serial, fallback to Settings.Secure.ANDROID_ID
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            return getSecureDeviceId(context);
        } else {
            return getDeviceSerialNumber(context);
        }
    }

    @SuppressLint("HardwareIds")
    private static String getSecureDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    /**
     * Gets the hardware serial number of this device. This only for backwards compatibility
     *
     * @return serial number or Settings.Secure.ANDROID_ID if not available.
     */
    @SuppressLint("MissingPermission")
    private static String getDeviceSerialNumber(Context context) {
        try {
            String deviceSerial = "";
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                deviceSerial = Build.getSerial();
            } else {
                deviceSerial = Build.SERIAL;
            }

            if (TextUtils.isEmpty(deviceSerial)) {
                return getSecureDeviceId(context);
            } else {
                return deviceSerial;
            }
        } catch (Exception ignored) {
            // Fall back to Android_ID
            return getSecureDeviceId(context);
        }
    }
}
