package com.securepreferences;

import android.util.Log;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

public class AesCbcWithIntegrityPrefValueEncrypter implements PrefValueEncrypter<AesCbcWithIntegrity.SecretKeys> {

    private static final String TAG = "AesPrefEncrypter";

    private AesCbcWithIntegrity.SecretKeys keys;

    private boolean sLoggingEnabled = true;

    private AesCbcWithIntegrityPrefValueEncrypter(Builder builder) {
        keys = builder.keys;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public AesCbcWithIntegrity.SecretKeys getKey() {
        return keys;
    }


    @Override
    public String encrypt(String cleartext) throws GeneralSecurityException {
        if (Utils.isEmpty(cleartext)) {
            return cleartext;
        }
        try {
            return AesCbcWithIntegrity.encrypt(cleartext, keys).toString();
        } catch (UnsupportedEncodingException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "encrypt", e);
            }
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public String decrypt(String ciphertext) throws GeneralSecurityException {
        if (Utils.isEmpty(ciphertext)) {
            return ciphertext;
        }
        try {
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(ciphertext);

            return AesCbcWithIntegrity.decryptString(cipherTextIvMac, keys);

        } catch (UnsupportedEncodingException e) {
            if (sLoggingEnabled) {
                Log.w(TAG, "decrypt", e);
            }
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public void clearKeys() {
        if (keys != null) {
            keys = null;
        }
    }

    public static final class Builder {
        private AesCbcWithIntegrity.SecretKeys keys;

        private Builder() {
        }

        public Builder withKey(AesCbcWithIntegrity.SecretKeys val) {
            keys = val;
            return this;
        }

        public Builder withPasswordSaltAndIterationsToGenerateKey(String password, byte[] salt, int iterationCount) throws GeneralSecurityException {
            keys = AesCbcWithIntegrity.generateKeyFromPassword(password, salt, iterationCount);
            return this;
        }

        public Builder withStringEncodedKey(String keyAsString) throws GeneralSecurityException {
            try {
                keys = AesCbcWithIntegrity.keys(keyAsString);
            } catch (InvalidKeyException e) {
                throw new GeneralSecurityException(e);
            }
            return this;
        }

        public AesCbcWithIntegrityPrefValueEncrypter build() throws GeneralSecurityException {
            if (keys == null) {
                keys = generateKey();
            }
            return new AesCbcWithIntegrityPrefValueEncrypter(this);
        }

        AesCbcWithIntegrity.SecretKeys generateKey() throws GeneralSecurityException {
            return AesCbcWithIntegrity.generateKey();
        }
    }
}
