package sawtooth.examples.xo.tests.conex;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.protobuf.ByteString;

import sawtooth.sdk.messaging.Stream;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.protobuf.PingRequest;

@Test
public class TestContextCreation {

	Stream connectionToValidator = new Stream("tcp://192.168.1.101:4004");
		
	@Test
	public void testPing() throws InterruptedException, ValidatorConnectionError {
		PingRequest pingMessage = PingRequest.newBuilder().build();
		ByteString result = connectionToValidator.send(MessageType.PING_REQUEST, pingMessage.toByteString()).getResult();
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isEmpty());
		
	}
	
}
