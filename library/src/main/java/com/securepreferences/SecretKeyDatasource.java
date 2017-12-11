package com.securepreferences;

public interface SecretKeyDatasource<T> {

    T getKey();

    boolean checkKeyIsPresent();

    void saveKey(T secretkey);

}
