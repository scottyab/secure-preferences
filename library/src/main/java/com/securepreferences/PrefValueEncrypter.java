package com.securepreferences;

import java.security.GeneralSecurityException;

/**
 * handles the key generation, encrypt, decrypt operations
 */
public interface PrefValueEncrypter {

    /**
     * @param plainText
     * @return base64 encoded string
     * @throws GeneralSecurityException if something in the encrypt fails
     */
    String encrypt(String plainText) throws GeneralSecurityException;

    /**
     * @param base64EncodedCipherText - Encoded CipherText
     * @return plain text
     * @throws GeneralSecurityException if something in the decrypt fails
     */
    String decrypt(String base64EncodedCipherText) throws GeneralSecurityException;

    /**
     * wipes key from memory
     */
    void clearKeys();

    /**
     * generates a new key
     */
    void reset();
}
