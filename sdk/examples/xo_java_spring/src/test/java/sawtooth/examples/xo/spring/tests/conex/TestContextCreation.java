package sawtooth.examples.xo.spring.tests.conex;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.protobuf.ByteString;

import sawtooth.examples.xo.spring.services.XOMessageBrokerService;
import sawtooth.examples.xo.spring.services.ZMQProxyService;
import sawtooth.examples.xo.spring.tests.BaseXOTest;
import sawtooth.sdk.messaging.Stream;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.protobuf.PingRequest;

@Test
public class TestContextCreation extends BaseXOTest {

	@Autowired
	XOMessageBrokerService mbService;
	
	Stream connectionToValidator = new Stream(ZMQProxyService.INTERNALZMSOCKETNAME);
	
	@Test
	public void testPing() throws InterruptedException, ValidatorConnectionError {
		
		PingRequest pingMessage = PingRequest.newBuilder().build();
		ByteString result = connectionToValidator.send(MessageType.PING_REQUEST, pingMessage.toByteString()).getResult();
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isEmpty());
		
	}
	

}
