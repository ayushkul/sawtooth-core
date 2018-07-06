/* Copyright 2016 Intel Corporation
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

package sawtooth.sdk.utils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FormattingUtils {

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static final MessageDigest messageDigest;

	static {
		MessageDigest temp = null;
		try {
			temp = MessageDigest.getInstance("SHA-512");
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		messageDigest = temp;
	}

	/**
	 * Create a sha-512 hash of a byte array.
	 *
	 * @param data a byte array which the hash is created from
	 * @return result a lowercase HexDigest of a sha-512 hash
	 */
	public static String hash512(byte[] data) {
		String result = null;

		messageDigest.update(data);
		byte[] digest = messageDigest.digest();
		result = bytesToHex(digest).toLowerCase();
		messageDigest.reset();
		return result;
	}

	/**
	 * Helper function. for dealing with Strings that come in via protobuf ByteString
	 * encoded cbor.
	 *
	 * @param fromCbor byte array from a String that came in via cbor
	 * @return a UTF-8 representation of the byte array
	 */
	public static String stringByteArrayToString(byte[] fromCbor) {
		return new String(fromCbor, Charset.forName("UTF-8"));
	}

	/**
	 * 	Helper, based on https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
	 * 
	 * @param bytes
	 * @return result a lowercase Hex representation from a byte[]
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return String.copyValueOf(hexChars);
	}
}
