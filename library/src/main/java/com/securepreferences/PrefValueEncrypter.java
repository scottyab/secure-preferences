package com.securepreferences;

import java.security.GeneralSecurityException;

public interface PrefValueEncrypter<T> {

    T getKey();

    /**
     * @param plainText
     * @return base64 encoded string
     * @throws GeneralSecurityException
     */
    String encrypt(String plainText) throws GeneralSecurityException;

    /**
     * base64 encoded base64
     *
     * @param base64EncodedCipherText
     * @return
     * @throws GeneralSecurityException
     */
    String decrypt(String base64EncodedCipherText) throws GeneralSecurityException;

    void clearKeys();
}
