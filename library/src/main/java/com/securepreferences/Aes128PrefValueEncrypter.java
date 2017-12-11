package com.securepreferences;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.annotations.Nullable;

public class Aes128PrefValueEncrypter implements PrefValueEncrypter<SecretKeySpec> {

    private static final int AES_KEY_LENGTH_BITS = 128;
    private static final String ALGORTHRM = "AES";

    private static final int IV_LENGTH = 16;
    private static final String METHOD = "AES/CBC/PKCS7Padding";

    private final SecureRandom mSecureRandom;
    private final Encoder mEncoder;
    private SecretKeySpec mKey;


    Aes128PrefValueEncrypter(Encoder encoder) {
        mSecureRandom = new SecureRandom();
        mEncoder = encoder;

    }

    private SecretKeySpec generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORTHRM);
            keyGen.init(AES_KEY_LENGTH_BITS);
            SecretKey key = keyGen.generateKey();

            return new SecretKeySpec(key.getEncoded(), ALGORTHRM);

        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }

    @Override
    public SecretKeySpec getKey() {
        return null;
    }

    /**
     * Use a securely random IV per value encrypted
     */
    byte[] generateIV() {
        byte iv[] = new byte[IV_LENGTH];
        mSecureRandom.nextBytes(iv);
        return iv;
    }


    /**
     * Append the IV and cipherText to same byte array so they can be transmitted/stored together
     */
    byte[] appendIV(byte[] iv, byte[] cipherText) {
        byte[] combined = new byte[iv.length + cipherText.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < iv.length ? iv[i] : cipherText[i - iv.length];
        }
        return combined;
    }

    byte[] getIv(byte[] ivAndCipherText) {
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(ivAndCipherText, 0, iv, 0, IV_LENGTH);

        return iv;
    }

    byte[] getEncryptedValue(byte[] ivAndCipherText) {
        int length = ivAndCipherText.length - IV_LENGTH;
        byte[] encryptedValueBytes = new byte[length];
        System.arraycopy(ivAndCipherText, IV_LENGTH, encryptedValueBytes, 0, length);
        return encryptedValueBytes;
    }


    @Override
    public String encrypt(String plainText) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(METHOD);

        byte[] iv = generateIV();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, mKey, ivSpec);

        byte[] plainTextAsBytes = plainText.getBytes();

        byte[] cipherText = cipher.doFinal(plainTextAsBytes);

        byte[] cipherTextWithIV = appendIV(iv, cipherText);

        return mEncoder.encode(cipherTextWithIV);

    }

    @Override
    public String decrypt(String base64EncodedCipherTextWithIv) throws GeneralSecurityException {
        byte[] decodedCipherTextWithIV = mEncoder.decode(base64EncodedCipherTextWithIv);

        byte[] iv = getIv(decodedCipherTextWithIV);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        final Cipher cipher = Cipher.getInstance(METHOD);
        cipher.init(Cipher.DECRYPT_MODE, mKey, ivSpec);

        byte[] cipherText = getEncryptedValue(decodedCipherTextWithIV);

        byte[] decryptedBytes = cipher.doFinal(cipherText);

        return new String(decryptedBytes);

    }

    @Override
    public void clearKeys() {
        mKey = null;
    }
}
