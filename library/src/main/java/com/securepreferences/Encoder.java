package com.securepreferences;

import android.util.Base64;

/**
 * Wraps the Android Base64 class with fixed ENCODING_FLAGS so we can ensure we're using the same
 */
class Encoder {

    private static final int ENCODING_FLAGS = Base64.NO_PADDING | Base64.NO_WRAP;

    String encode(byte[] value) {
        return Base64.encodeToString(value, ENCODING_FLAGS);
    }

    byte[] decode(String value) {
        return Base64.decode(value, ENCODING_FLAGS);
    }

}
