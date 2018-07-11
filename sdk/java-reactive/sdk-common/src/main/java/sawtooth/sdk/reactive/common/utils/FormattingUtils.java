package sawtooth.sdk.reactive.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.TextFormat;

public class FormattingUtils {

	
	private static ThreadLocal<MessageDigest> MESSAGEDIGESTER_512 = new ThreadLocal<>();

	static {

		try {
			MESSAGEDIGESTER_512.set(MessageDigest.getInstance("SHA-512"));
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create a sha-512 hash of a byte array.
	 *
	 * @param data a byte array which the hash is created from
	 * @return result a lowercase HexDigest of a sha-512 hash
	 */
	public static String hash512(byte[] data) {
		String result = null;
		MESSAGEDIGESTER_512.get().update(data);
		byte[] digest = MESSAGEDIGESTER_512.get().digest();
		result = bytesToHex(digest).toLowerCase();
		MESSAGEDIGESTER_512.get().reset();
		return result;
	}


	/**
	 * Helper function. for dealing with Strings that come in via protobuf ByteString
	 * encoded cbor.
	 *
	 * @param fromCbor byte array from a String that came in via cbor
	 * @return a UTF-8 representation of the byte array
	 */
	public static String cborByteArrayToString(byte[] fromCbor) {
		return new String(fromCbor, StandardCharsets.US_ASCII);
	}

	/**
	 * 	Helper, to concentrate the parsing of Bytes to String over the project.
	 * 
	 * @param bytes
	 * @return result a lowercase Hex representation from a byte[]
	 */
	public static String bytesToHex(byte[] bytes) {
		return Hex.toHexString(bytes);
	}
	
	public static String bytesToHexASCII(byte[] bytes) {
		return new String(bytes,StandardCharsets.US_ASCII);
	}
	
	/**
	 * Helper, to concentrate the parsing of String to Bytes over the project.
	 * @param s - Hexadecimal string
	 * @return Bytes of the representation
	 */
	public static byte[] hexStringToByteArray(String s) {
		return Hex.decode(s);
	}
	
	/**
	 * 	Helper, to concentrate the parsing of Bytes to String over the project.
	 * 
	 * @param bytes
	 * @return result a lowercase Hex representation from a byte[]
	 */
	public static String bytesToHexBase64(byte[] bytes) {
		return DatatypeConverter.printBase64Binary(bytes);
	}
	
	/**
	 * Helper, to concentrate the parsing of String to Bytes over the project.
	 * @param s - Hexadecimal string
	 * @return Bytes of the representation
	 */
	public static byte[] hexStringBase64ToByteArray(String s) {
		return DatatypeConverter.parseBase64Binary(s);
	}
	
	
}
