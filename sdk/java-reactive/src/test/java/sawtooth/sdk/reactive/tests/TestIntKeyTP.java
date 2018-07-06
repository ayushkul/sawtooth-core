package sawtooth.sdk.reactive.tests;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Response;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.factory.MessageFactory;
import sawtooth.sdk.reactive.rest.ops.RESTBatchOps;

@Test
public class TestIntKeyTP extends BaseTest {

	RESTBatchOps underTest;
	MessageFactory intMessageFactory = null;

	@BeforeClass
	public void getCDIBeans() throws ClassNotFoundException, NoSuchAlgorithmException {
		intMessageFactory = new MessageFactory("intkey", "1.0", null, "1cf126");
		underTest = (RESTBatchOps) CDI.current().select(RESTBatchOps.class).get();
		
	}

	@Test
	public void testState() {
		assertNotNull(underTest);
	}

	@Test(dependsOnMethods = { "testState" })
	public void testSendTransaction() throws InterruptedException, ExecutionException, CborException, NoSuchAlgorithmException {
		IntKeyPayload body = new IntKeyPayload("set", "aaaaaaaaaaaaaaaaaaaa", 0);
		StringBuffer payload = new StringBuffer();
		payload.append(body.getPayload());
		Message intTX = intMessageFactory.getProcessRequest("", payload, null, null, null,
				intMessageFactory.getPublicKeyString());
		Batch toSend = intMessageFactory.createBatch(Arrays.asList(intTX));
		Future<Response> result = underTest.submitBatches(Arrays.asList(toSend));
		assertNotNull(result);
		assertFalse(((CompletableFuture<Response>)result).isCompletedExceptionally());
		assertNotNull(result.get());
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
