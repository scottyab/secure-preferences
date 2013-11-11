package com.securepreferences.util;


public class Utils {

	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String encode(byte[] input) {
		// Android Base64 caused issues
		return new String(org.spongycastle.util.encoders.Base64.encode(input));

	}

	public static byte[] decode(String input) {
		// Android Base64 caused issues
		return org.spongycastle.util.encoders.Base64.decode(input);
	}
}
