package com.securepreferences;

/**
 * handles storing and retrieving the key
 */
public interface SecretKeyDatasource {

    byte[] getKey();

    boolean checkKeyIsPresent();

    void saveKey(byte[] secretkey);

    void destroyKey();

}
