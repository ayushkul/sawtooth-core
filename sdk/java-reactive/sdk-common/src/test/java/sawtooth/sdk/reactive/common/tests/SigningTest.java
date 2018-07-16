package sawtooth.sdk.reactive.common.tests;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import org.bitcoin.NativeSecp256k1;
import org.bitcoin.NativeSecp256k1Util.AssertFailException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.BatchHeader;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.TransactionHeader;
import sawtooth.sdk.reactive.common.config.SawtoothConfiguration;
import sawtooth.sdk.reactive.common.crypto.SawtoothSigner;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;
import sawtooth.sdk.reactive.common.utils.FormattingUtils;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 *
 */
@Test
public class SigningTest {

  private static ECKey signerPrivateKey = null;
  private static MessageFactory mFact = null;
  SawtoothConfiguration config = new SawtoothConfiguration();
  ThreadLocal<MessageDigest> MESSAGEDIGESTER_512 = new ThreadLocal<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(SigningTest.class);

  @BeforeClass
  public void setUp() throws FileNotFoundException, IOException {
    try (
        BufferedReader privateKeyReader = new BufferedReader(
            new FileReader(SigningTest.class.getClassLoader().getResource("jack.priv").getFile()));
        BufferedReader publicKeyReader = new BufferedReader(
            new FileReader(SigningTest.class.getClassLoader().getResource("jack.pub").getFile()))) {
      String linePrivate = privateKeyReader.readLine();
      BigInteger privkey = new BigInteger(1, Hex.decode(linePrivate));
      signerPrivateKey = ECKey.fromPrivate(privkey, false);

      String linePublic = publicKeyReader.readLine();
      privkey = new BigInteger(1, Hex.decode(linePublic));
      mFact = new MessageFactory("testmessagefactory", "0.0.0", signerPrivateKey, null,
          "testmessagefactory");
      MESSAGEDIGESTER_512.set(MessageDigest.getInstance("SHA-512"));
    } catch (IOException e) {
      LOGGER.error("IO Exception : " + e.getMessage());
      fail(e.getMessage());
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      fail(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * This test is for signature validations on the Sawtooth Validator at
   * (https://github.com/hyperledger/sawtooth-core/blob/master/validator/sawtooth_validator/gossip/signature_verifier.py)
   * 
   * 
   * @throws InvalidProtocolBufferException
   * @throws AssertFailException
   * 
   */
  @Test
  public void testPythonCompability() throws InvalidProtocolBufferException, AssertFailException {
    byte[] headerSignature = FormattingUtils.hexStringToByteArray(
        "dda4bef2b09232471b518fe66bbb8c6214b8ffe68fe90fab07c793d95d64f83f05ab32e6c4c75a12c1b9c45b3667da2395e74ec9d4b1886bdd9cc760dc9673a6");
    byte[] header = FormattingUtils.hexStringToByteArray(
        "0a423032626434663763646433666332353237333532343935303938343665663166353234383165613862363332303035346239613537313536333464316139643165361280016366346465326265373533636236636538646230336662633135303533353662366232316234306563613064666532666666666237626231393933663966323937643732383566326637343339653830653836623636303563336432653862613966626337346532303833663233306464326232363639623663336662363036");
    byte[] publicKey = FormattingUtils
        .hexStringToByteArray("02bd4f7cdd3fc252735249509846ef1f52481ea8b6320054b9a5715634d1a9d1e6");

    byte[] derSignature = decompressSignature(headerSignature);

    BatchHeader constructedHeader = BatchHeader.parseFrom(ByteString.copyFrom(header));
    Sha256Hash batchHeaderHashed = Sha256Hash.of(constructedHeader.toByteArray());

    ECKey pubKey = ECKey.fromPublicOnly(publicKey);

    assertTrue(NativeSecp256k1.verify(batchHeaderHashed.getBytes(), derSignature, publicKey),
        "Native signature verification failed.");

    Assert.assertTrue(ECKey.decompressPoint(pubKey.getPubKeyPoint()).isValid(),
        "Generated Pub key failed the decompressed validation.");

    assertTrue(pubKey.verify(batchHeaderHashed.getBytes(), derSignature),
        "Generated Pub Key signature verification failed.");

  }

  /**
   * 
   * This method verifies the libraries minimal adherence to the operations that will be performed.
   * 
   * @throws AssertFailException
   */

  @Test(dependsOnMethods = {"testPythonCompability"})
  public void testBasicSigning() throws AssertFailException {

    assertTrue(NativeSecp256k1.secKeyVerify(signerPrivateKey.getPrivKeyBytes()),
        "Private key is invalid.");

    ECKey pubKey2 = ECKey.fromPublicOnly(mFact.getSignerPublicKeyEncodedPointByte());

    Assert.assertTrue(ECKey.decompressPoint(pubKey2.getPubKeyPoint()).isValid(),
        "Message Factory Public key failed the decompressed validation.");

    byte[] dataToSign = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes();
    Sha256Hash dataHashed = Sha256Hash.of(dataToSign);

    byte[] compactSignature = SawtoothSigner.generateCompactSig(signerPrivateKey, dataToSign);
    byte[] nativeSignature =
        NativeSecp256k1.sign(dataHashed.getBytes(), signerPrivateKey.getPrivKeyBytes());

    byte[] decompactedSignature = decompressSignature(compactSignature);

    assertTrue(signerPrivateKey.verify(dataHashed.getBytes(), decompactedSignature));
    assertTrue(signerPrivateKey.verify(dataHashed.getBytes(), nativeSignature));

    assertTrue(NativeSecp256k1.verify(dataHashed.getBytes(), decompactedSignature,
        signerPrivateKey.getPubKey()));
    assertTrue(NativeSecp256k1.verify(dataHashed.getBytes(), nativeSignature,
        signerPrivateKey.getPubKey()));
  }

  /**
   * 
   * Tests the manual steps defined in the <a href=
   * "https://sawtooth.hyperledger.org/docs/core/releases/1.0/_autogen/txn_submit_tutorial.html#building-the-transaction">Documentation</a>.
   * 
   * @throws AssertFailException
   */
  @Test(dependsOnMethods = {"testBasicSigning"})
  public void testManualTransactioHeaderSigning() throws AssertFailException {

    byte[] publicKey = Utils.bigIntegerToBytes(new BigInteger(1, signerPrivateKey.getPubKey()), 32);
    String publicKeyHex = FormattingUtils.bytesToHex(publicKey);

    TransactionHeader.Builder thBuilder = TransactionHeader.newBuilder();
    byte[] payload = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes();
    thBuilder.setFamilyName("aaaaaaa");
    thBuilder.setFamilyVersion("0.0");
    thBuilder.setSignerPublicKey(FormattingUtils.bytesToHex(signerPrivateKey.getPubKey()));
    thBuilder.setBatcherPublicKey(FormattingUtils.bytesToHex(signerPrivateKey.getPubKey()));
    thBuilder.setNonce(String.valueOf(Calendar.getInstance().getTimeInMillis()));
    MESSAGEDIGESTER_512.get().reset();
    MESSAGEDIGESTER_512.get().update(payload, 0, payload.length);
    thBuilder.setPayloadSha512(FormattingUtils.bytesToHex(MESSAGEDIGESTER_512.get().digest()));

    TransactionHeader theHeader = thBuilder.build();
    byte[] transactionHeaderByte = theHeader.toByteArray();
    Sha256Hash transactionHeaderHashed = Sha256Hash.of(transactionHeaderByte);

    ECKey.ECDSASignature signature = signerPrivateKey.sign(transactionHeaderHashed);
    byte[] nativeSignature =
        SawtoothSigner.signWithNativeSecp256k1(signerPrivateKey, transactionHeaderHashed);

    // Validation of the keys based on the Native library
    assertTrue(NativeSecp256k1.secKeyVerify(publicKey));
    assertTrue(NativeSecp256k1.secKeyVerify(FormattingUtils.hexStringToByteArray(publicKeyHex)));

    // Verifying signature from the Key
    assertTrue(signerPrivateKey.verify(transactionHeaderHashed, signature));
    assertTrue(
        signerPrivateKey.verify(transactionHeaderHashed.getBytes(), signature.encodeToDER()));

    // Verifying signature from the Native Library (the Validator will run this code
    // locally)
    assertTrue(NativeSecp256k1.verify(transactionHeaderHashed.getBytes(), nativeSignature,
        signerPrivateKey.getPubKey()));

    assertTrue(NativeSecp256k1.verify(transactionHeaderHashed.getBytes(), signature.encodeToDER(),
        FormattingUtils.hexStringToByteArray(thBuilder.getSignerPublicKey())));

  }
  
  /**
   * 
   *    Tests the validity of the Transaction Header created by the MessageFactory.
   * 
   * @throws AssertFailException
   * @throws NoSuchAlgorithmException
   */

  @Test(dependsOnMethods = {"testBasicSigning"})
  public void testTransactioHeaderSigning() throws AssertFailException, NoSuchAlgorithmException {

    MESSAGEDIGESTER_512.get().reset();
    byte[] payloadBin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAa".getBytes();
    String hexFormattedDigest =
        FormattingUtils.bytesToHex(MESSAGEDIGESTER_512.get().digest(payloadBin));

    TransactionHeader tHeader = mFact.createTransactionHeader(hexFormattedDigest,
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, null);

    byte[] transactionHeaderByte = tHeader.toString().getBytes(StandardCharsets.US_ASCII);
    Sha256Hash transactionHeaderHashed = Sha256Hash.of(transactionHeaderByte);

    ECKey.ECDSASignature signature = signerPrivateKey.sign(transactionHeaderHashed);
    byte[] nativeSignature =
        SawtoothSigner.signWithNativeSecp256k1(signerPrivateKey, transactionHeaderHashed);


    // Verifying signature from the Key
    assertTrue(signerPrivateKey.verify(transactionHeaderHashed, signature));
    assertTrue(
        signerPrivateKey.verify(transactionHeaderHashed.getBytes(), signature.encodeToDER()));

    // Verifying signature from the Native Library (the Validator will run this code
    // locally)
    assertTrue(NativeSecp256k1.verify(transactionHeaderHashed.getBytes(), nativeSignature,
        signerPrivateKey.getPubKey()));

    assertTrue(NativeSecp256k1.verify(transactionHeaderHashed.getBytes(), signature.encodeToDER(),
        FormattingUtils.hexStringToByteArray(tHeader.getSignerPublicKey())));

  }

  /**
   * 
   * Tests the validity of the Batch created by the MessageFactory.
   * 
   * @throws NoSuchAlgorithmException
   * @throws AssertFailException
   * @throws IOException
   */
  @Test(dependsOnMethods = {"testBasicSigning"})
  public void testBatchSigning() throws NoSuchAlgorithmException, AssertFailException, IOException {
    ByteBuffer payload = ByteBuffer.wrap("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes());
    Message intTX = mFact.getProcessRequest("", payload, null, null, null, null);
    Batch toSend = mFact.createBatch(Arrays.asList(intTX), true);
    BatchHeader constructedHeader = BatchHeader.parseFrom(toSend.getHeader());
    Assert.assertTrue(testPythonServerBatchValidation(constructedHeader,
        FormattingUtils.hexStringToByteArray(toSend.getHeaderSignature())));

  }


  /**
   * 
   * Test helpers.
   *
   *
   * This 2 methods help in the test of this validation:
   * 
   * 
// @formatter:off    
   def verify(self, signature, message, public_key):
          try:
              sig_bytes = bytes.fromhex(signature)
  
              sig = public_key.secp256k1_public_key.ecdsa_deserialize_compact(
                  sig_bytes)
              return public_key.secp256k1_public_key.ecdsa_verify(message, sig)
          # pylint: disable=broad-except
          except Exception:
              return False

// @formatter:on
   * 
   * 
   * Present in sec256k1.py.
   * 
   *    This method makes the exact same operation that will happen on the server.
   * 
   **/
  private static boolean testPythonServerBatchValidation(BatchHeader header,
      byte[] headerSignatureGenerated) throws AssertFailException, IOException {

    byte[] derSignature = decompressSignature(headerSignatureGenerated);

    Sha256Hash batchHeaderHashed = Sha256Hash.of(header.toByteArray());

    ECKey pubKey = ECKey.fromPublicOnly(
        new BigInteger(FormattingUtils.hexStringToByteArray(header.getSignerPublicKey()))
            .toByteArray());

    Assert.assertTrue(ECKey.decompressPoint(pubKey.getPubKeyPoint()).isValid(),
        "Header Public key failed the decompressed validation.");

    assertTrue(pubKey.verify(batchHeaderHashed.getBytes(), derSignature),
        "Header Public key signature verification failed.");

    assertTrue(
        NativeSecp256k1.verify(batchHeaderHashed.getBytes(), derSignature, pubKey.getPubKey()),
        "Native signature verification failed.");

    return true;

  }

  /**
   * This method decompress the signature under test.
   * 
   * @param compressedSignature
   * @return Decompressed signature.
   */
  private static byte[] decompressSignature(byte[] compressedSignature) {
    byte[] r = Arrays.copyOfRange(compressedSignature, 0, compressedSignature.length / 2);
    byte[] s = Arrays.copyOfRange(compressedSignature, compressedSignature.length / 2,
        compressedSignature.length);
    byte[] output = null;
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      DEROutputStream derOutputStream = new DEROutputStream(byteArrayOutputStream);
      ASN1EncodableVector v = new ASN1EncodableVector();
      v.add(new ASN1Integer(new BigInteger(1, r)));
      v.add(new ASN1Integer(new BigInteger(1, s)));
      derOutputStream.writeObject(new DERSequence(v));
      output = byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return output;

  }

}
