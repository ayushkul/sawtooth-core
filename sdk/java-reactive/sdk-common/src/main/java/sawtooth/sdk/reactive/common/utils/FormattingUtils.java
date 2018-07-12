package sawtooth.sdk.reactive.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class FormattingUtils {

  /**
   * This digester is relatively expensive, let's cache it on a per thread basis.
   */
  private static ThreadLocal<MessageDigest> MESSAGEDIGESTER_512 = new ThreadLocal<>();
  private final static Logger LOGGER = LoggerFactory.getLogger(FormattingUtils.class);

  static {
    try {
      MESSAGEDIGESTER_512.set(MessageDigest.getInstance("SHA-512"));
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("No SHA 512 provider found... Problems ahead.");
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
    MESSAGEDIGESTER_512.get().reset();
    MESSAGEDIGESTER_512.get().update(data);
    byte[] digest = MESSAGEDIGESTER_512.get().digest();
    return bytesToHex(digest).toLowerCase();
  }


  /**
   * Helper function. for dealing with Strings that come in via protobuf ByteString encoded cbor.
   *
   * @param fromCbor byte array from a String that came in via cbor
   * @return a UTF-8 representation of the byte array
   */
  public static String cborByteArrayToString(byte[] fromCbor) {
    return new String(fromCbor, StandardCharsets.US_ASCII);
  }

  /**
   * Helper, to concentrate the parsing of Bytes to String over the project.
   * 
   * @param bytes
   * @return result a lowercase Hex representation from a byte[]
   */
  public static String bytesToHex(final byte[] bytes) {
    return Hex.toHexString(bytes);
  }

  public static String bytesToHexASCII(final byte[] bytes) {
    return new String(bytes, StandardCharsets.US_ASCII);
  }

  /**
   * Helper, to concentrate the parsing of String to Bytes over the project.
   * 
   * @param s - Hexadecimal string
   * @return Bytes of the representation
   */
  public static byte[] hexStringToByteArray(final String s) {
    return Hex.decode(s);
  }

  /**
   * Helper, to concentrate the parsing of Bytes to String over the project.
   * 
   * @param bytes
   * @return result a lowercase Hex representation from a byte[]
   */
  public static String bytesToHexBase64(final byte[] bytes) {
    return DatatypeConverter.printBase64Binary(bytes);
  }

  /**
   * Helper, to concentrate the parsing of String to Bytes over the project.
   * 
   * @param s - Hexadecimal string
   * @return Bytes of the representation
   */
  public static byte[] hexStringBase64ToByteArray(final String s) {
    return DatatypeConverter.parseBase64Binary(s);
  }


}
