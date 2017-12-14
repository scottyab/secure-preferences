package com.securepreferences;

import android.util.Log;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

// TODO: 11/12/2017 add to seperate module so AesCbcWithIntegrity is optional extra??
public class AesCbcWithIntegrityPrefValueEncrypter implements PrefValueEncrypter {

    private static final String TAG = "AesPrefEncrypter";

    private AesCbcWithIntegrity.SecretKeys keys;
    private final Encoder encoder;
    private final SecretKeyDatasource secretKeyDatasource;

    private boolean sLoggingEnabled = true;

    private AesCbcWithIntegrityPrefValueEncrypter(Builder builder) {
        keys = builder.keys;
        this.encoder = builder.encoder;
        this.secretKeyDatasource = builder.secretKeyDatasource;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void reset() {

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
        private Encoder encoder = new Encoder();
        private SecretKeyDatasource secretKeyDatasource;

        private Builder() {
        }

        public Builder withEncoder(Encoder encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder witKeyDataSource(SecretKeyDatasource secretKeyDatasource) {
            this.secretKeyDatasource = secretKeyDatasource;
            return this;
        }

        public Builder withKey(AesCbcWithIntegrity.SecretKeys keys) {
            this.keys = keys;
            return this;
        }

        public Builder withPasswordSaltAndIterationsToGenerateKey(String password, byte[] salt, int iterationCount) throws GeneralSecurityException {
            keys = AesCbcWithIntegrity.generateKeyFromPassword(password, salt, iterationCount);
            return this;
        }

        public Builder withStringEncodedKey(String keyAsString) throws GeneralSecurityException {
            try {
                keys = AesCbcWithIntegrity.keys(keyAsString);
                return this;
            } catch (InvalidKeyException e) {
                throw new GeneralSecurityException(e);
            }
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
