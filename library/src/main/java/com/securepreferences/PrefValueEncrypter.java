package com.securepreferences;

import java.security.GeneralSecurityException;

public interface PrefValueEncrypter<T> {

    T getKey();

    String encrypt(String String) throws GeneralSecurityException;

    String decrypt(String String) throws GeneralSecurityException;

    void clearKeys();
}
