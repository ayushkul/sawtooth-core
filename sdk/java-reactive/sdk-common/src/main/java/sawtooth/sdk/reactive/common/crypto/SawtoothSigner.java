package sawtooth.sdk.reactive.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bitcoin.NativeSecp256k1;
import org.bitcoin.NativeSecp256k1Util.AssertFailException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import sawtooth.sdk.reactive.common.utils.FormattingUtils;

public class SawtoothSigner {

	public static ECKey generatePrivateKey(SecureRandom random) {
		return new ECKey(random);
	}

	public static String getPublicKey(ECKey privateKey) {
		return ECKey.fromPrivate(privateKey.getPrivKey(), true).getPublicKeyAsHex();
	}

	/**
	 * Returns a bitcoin-style 64-byte compact signature.
	 * @param privateKey the private key with which to sign
	 * @param data the data to sign
	 * @return String the signature
// @formatter:off
	  def sign(self, message, private_key):
        try:
            signature = private_key.secp256k1_private_key.ecdsa_sign(message)
            signature = private_key.secp256k1_private_key \
                .ecdsa_serialize_compact(signature)

            return signature.hex()
            
// @formatter:on
	 */
	public static String signHexSequence(ECKey privateKey, byte[] data) {

		return FormattingUtils.bytesToHex(generateCompactSig(privateKey, data)).toLowerCase();
	}

	/**
	 * Returns a bitcoin-style 64-byte compact signature.
	 * @param privateKey the private key with which to sign
	 * @param data the data to sign
	 * @return String the signature
	 */
	public static String signASCII(ECKey privateKey, byte[] data) {

		return new String(generateCompactSig(privateKey, data), StandardCharsets.US_ASCII);
	}

	public static byte[] generateCompactSig(ECKey privateKey, byte[] data) {
		Sha256Hash hash = Sha256Hash.of(data);
		ECKey.ECDSASignature sig = privateKey.sign(hash);

		byte[] csig = new byte[64];

		System.arraycopy(Utils.bigIntegerToBytes(sig.r, 32), 0, csig, 0, 32);
		System.arraycopy(Utils.bigIntegerToBytes(sig.s, 32), 0, csig, 32, 32);
		return csig;
	}

	public static byte[] signWithNativeSecp256k1(ECKey privateKey, Sha256Hash hashedData) throws AssertFailException {
		return NativeSecp256k1.sign(hashedData.getBytes(), privateKey.getPrivKeyBytes());
	}
}
