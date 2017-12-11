package com.securepreferences;

public class NoOpPrefKeyObfuscator implements PrefKeyObfuscator {

    /**
     * doesn't hash the key
     *
     * @param keyname preference key name
     * @return keyname
     */
    @Override
    public String obfuscate(String keyname) {
        return keyname;
    }
}
