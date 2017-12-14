package com.securepreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

public class Aes128PrefValueEncrypterTest {

    private Aes128PrefValueEncrypter mSut;

    @Mock
    Encoder mockEncoder;

    @Mock
    SecretKeyDatasource mockSecretKeyDatasource;


    @Before
    public void setup() {
        mSut = new Aes128PrefValueEncrypter(mockEncoder, mockSecretKeyDatasource);
    }

    @Test
    public void testIVIsCorrectLength() {
        byte[] iv = mSut.generateIV();
        assertEquals(16, iv.length);
    }

    @Test
    public void testMultipleCalledToGenerateIvAreDifferent() {
        byte[] iv = mSut.generateIV();
        byte[] iv2 = mSut.generateIV();
        assertNotSame(bytesToHex(iv), bytesToHex(iv2));
    }

    @Test
    public void testIvIsAppended() {
        byte[] iv = "myIV".getBytes();
        byte[] cipher = "shhhhhSecret".getBytes();

        int expectedLength = iv.length + cipher.length;

        byte[] combiened = mSut.appendIV(iv, cipher);

        assertEquals(expectedLength, combiened.length);
    }


    @Test
    public void testIvIsExtracted() {
        byte[] expectedIvBytes =
                {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14,
                        0x15, 0x16};
        byte[] cipher = "shhhhhSecret".getBytes();
        byte[] combined = combine(expectedIvBytes, cipher);

        byte[] ivBytes = mSut.getIv(combined);

        assertEquals(bytesToHex(expectedIvBytes), bytesToHex(ivBytes));
    }

    @Test
    public void testCipherTextIsExtracted() {
        byte[] ivBytes =
                {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14,
                        0x15, 0x16};
        byte[] expectedEncryptedValue = "shhhhhSecret".getBytes();
        byte[] combined = combine(ivBytes, expectedEncryptedValue);

        byte[] encryptedValue = mSut.getEncryptedValue(combined);

        assertEquals(bytesToHex(expectedEncryptedValue), bytesToHex(encryptedValue));
    }


    private byte[] combine(byte[] array1, byte[] array2) {
        byte[] combined = new byte[array1.length + array2.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < array1.length ? array1[i] : array2[i - array1.length];
        }
        return combined;
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return "[" + bytes.length + "]" + new String(hexChars);
    }
}
