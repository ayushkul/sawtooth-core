package sawtooth.sdk.rest.client.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Response;
import org.bitcoin.NativeSecp256k1Util.AssertFailException;
import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;
import sawtooth.sdk.reactive.common.utils.FormattingUtils;
import sawtooth.sdk.reactive.rest.ops.RESTBatchOps;

@Test
public class TestIntKeyTP extends BaseTest {

  RESTBatchOps underTest;
  MessageFactory intMessageFactory = null;
  ECKey signerPrivateKey, signerPublicKey;
  String startAddressData = "";

  @BeforeClass
  public void getCDIBeans() throws ClassNotFoundException, NoSuchAlgorithmException {
    underTest = (RESTBatchOps) CDI.current().select(RESTBatchOps.class).get();
    try (
        BufferedReader privateKeyReader = new BufferedReader(
            new FileReader(TestIntKeyTP.class.getClassLoader().getResource("jack.priv").getFile()));
        BufferedReader publicKeyReader = new BufferedReader(new FileReader(
            TestIntKeyTP.class.getClassLoader().getResource("jack.pub").getFile()))) {
      String linePrivate = privateKeyReader.readLine();
      BigInteger privkey = new BigInteger(1, Hex.decode(linePrivate));
      signerPrivateKey = ECKey.fromPrivate(privkey, false);

      String linePublic = publicKeyReader.readLine();
      privkey = new BigInteger(1, Hex.decode(linePublic));
      signerPublicKey = ECKey.fromPublicOnly(ECKey.publicPointFromPrivate(privkey));

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    intMessageFactory = new MessageFactory("intkey", "1.0", signerPrivateKey, null, "intkey");
    startAddressData =
        FormattingUtils.hash512("intkey".getBytes(StandardCharsets.UTF_8)).substring(0, 6);

  }

  @Test
  public void testState() {
    assertNotNull(underTest);
  }


  /**
   * 
   * This test intends to use Integer Key TP, so, it must be running on the Sawtooth configured in
   * the POM
   * 
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws CborException
   * @throws NoSuchAlgorithmException
   * @throws AssertFailException
   * @throws IOException
   */
  @Test(dependsOnMethods = {"testState"})
  public void testSendTransaction() throws InterruptedException, ExecutionException,
      NoSuchAlgorithmException, AssertFailException, IOException {
    Random rnd = new Random();
    
    String randomAddress = UUID.randomUUID().toString();
    int position = rnd.nextInt(randomAddress.length()-10);
    randomAddress = randomAddress.substring(position, position+6);
    String addressToSet =
        FormattingUtils.hash512(randomAddress.getBytes());
    addressToSet = startAddressData + addressToSet.substring(addressToSet.length() - 64);
    IntKeyPayload body = new IntKeyPayload("set", randomAddress, rnd.nextInt(100));
    ByteBuffer payload = ByteBuffer.wrap(body.getPayload());
    Message intTX = intMessageFactory.getProcessRequest(null, payload,
        Arrays.asList(addressToSet), Arrays.asList(addressToSet), null, null);
    Batch toSend = intMessageFactory.createBatch(Arrays.asList(intTX), false);

    Future<Response> result = underTest.submitBatches(Arrays.asList(toSend), null);
    assertNotNull(result);
    assertFalse(((CompletableFuture<Response>) result).isCompletedExceptionally());
    Response serverResult = result.get();
    assertNotNull(serverResult);
    assertEquals(serverResult.getStatus(), HttpURLConnection.HTTP_ACCEPTED);
  }

  private class IntKeyPayload {

    private final HashMap<String, Object> data = new HashMap<String, Object>();
    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    public IntKeyPayload(String verb, String name, int value) throws JsonProcessingException {
      data.put("Verb", verb);
      data.put("Name", name);
      data.put("Value", value);
    }


    public byte[] getPayload() throws IOException {

      byte[] cborData = mapper.writeValueAsBytes(data);
      return cborData;
    }

  }

}
