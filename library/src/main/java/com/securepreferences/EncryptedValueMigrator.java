package com.securepreferences;

public interface EncryptedValueMigrator {

    void migrateValues(PrefValueEncrypter toEncrypter);
}
