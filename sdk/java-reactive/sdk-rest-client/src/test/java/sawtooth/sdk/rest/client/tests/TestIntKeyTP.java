package sawtooth.sdk.rest.client.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.bitcoin.NativeSecp256k1;
import org.bitcoin.NativeSecp256k1Util.AssertFailException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.crypto.SawtoothSigner;
import sawtooth.sdk.reactive.common.messages.MessageFactory;
import sawtooth.sdk.reactive.common.utils.FormattingUtils;
import sawtooth.sdk.reactive.rest.ops.RESTBatchOps;

import javax.annotation.Priority;

@Test
public class TestIntKeyTP extends BaseTest {

	RESTBatchOps underTest;
	MessageFactory intMessageFactory = null;
	ECKey signerPrivateKey, signerPublicKey;

	@BeforeClass
	public void getCDIBeans() throws ClassNotFoundException, NoSuchAlgorithmException {
		underTest = (RESTBatchOps) CDI.current().select(RESTBatchOps.class).get();
		try (BufferedReader privateKeyReader = new BufferedReader(
				new FileReader(TestIntKeyTP.class.getClassLoader().getResource("jack.priv").getFile()));
				BufferedReader publicKeyReader = new BufferedReader(
						new FileReader(TestIntKeyTP.class.getClassLoader().getResource("jack.pub").getFile()))) {
			String linePrivate = privateKeyReader.readLine();
			BigInteger privkey = new BigInteger(1, Hex.decode(linePrivate));
			signerPrivateKey = ECKey.fromPrivate(privkey, false);

			String linePublic = publicKeyReader.readLine();
			privkey = new BigInteger(1, Hex.decode(linePublic));
			signerPublicKey = ECKey.fromPublicOnly(ECKey.publicPointFromPrivate(privkey));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		intMessageFactory = new MessageFactory("intkey", "1.0", signerPrivateKey, null, "1cf126");

	}

	@Test
	public void testState() {
		assertNotNull(underTest);
	}

	
	/**
	 * 
	 * 	This test intends to use Integer Key TP, so, it must be running on the Sawtooth configured in the POM
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws CborException
	 * @throws NoSuchAlgorithmException
	 * @throws AssertFailException
	 */
	@Test(dependsOnMethods = { "testState" })
	public void testSendTransaction() throws InterruptedException, ExecutionException, CborException,
			NoSuchAlgorithmException, AssertFailException {
		IntKeyPayload body = new IntKeyPayload("set", "aaaaaaaaaaaaaaaaaaaa", 0);
		StringBuffer payload = new StringBuffer();
		payload.append(body.getPayload());
		Message intTX = intMessageFactory.getProcessRequest("----", payload, null, null, null, null);
		Batch toSend = intMessageFactory.createBatch(Arrays.asList(intTX), true);
		
		Future<Response> result = underTest.submitBatches(Arrays.asList(toSend));
		assertNotNull(result);
		assertFalse(((CompletableFuture<Response>) result).isCompletedExceptionally());
		Response serverResult = result.get();
		assertNotNull(serverResult);
		assertEquals(serverResult.getStatus(),HttpURLConnection.HTTP_ACCEPTED);
	}

	private class IntKeyPayload {

		private final Map<String, String> data = new HashMap<>();
		private ByteArrayOutputStream output = null;

		public IntKeyPayload(String verb, String name, int value) {
			data.put("verb", verb);
			data.put("name ", name);
			data.put("value", String.valueOf(value));
		}

		public byte[] getPayload() throws CborException {
			output = new ByteArrayOutputStream();
			new CborEncoder(output).encode(new CborBuilder().addMap().put("verb", data.get("verb"))
					.put("name", data.get("name")).put("value", data.get("value")).end().build());
			return output.toByteArray();
		}

	}

}
