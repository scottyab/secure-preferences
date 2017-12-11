package com.securepreferences;

public interface PrefKeyObfuscator {

    /**
     * Creates a hash of the keyname providing some security through obscurity
     *
     * @param keyname preference key name
     * @return encoded one way (hash)
     */
    String obfuscate(String keyname);

}
