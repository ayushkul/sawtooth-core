package sawtooth.examples.xo.tests.conex;

import java.nio.charset.Charset;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.protobuf.ByteString;

import sawtooth.sdk.messaging.Stream0MQImpl;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.protobuf.PingRequest;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TpRegisterRequest;

@Test
public class TestContextCreation {

	Stream0MQImpl connectionToValidator = new Stream0MQImpl("tcp://192.168.1.101:8800");
// sawset proposal create sawtooth.validator.transaction_families='[{"family": "xo", "version": "1.0"}]'
	@Test
	public void testPing() throws InterruptedException, ValidatorConnectionError {
		TpRegisterRequest pingMessage = TpRegisterRequest.newBuilder().setFamily("xo").setVersion("1.0").build();
		Message mesg = Message.newBuilder().setCorrelationId("AAAAAAAAAAA").setMessageType(MessageType.PING_REQUEST)
				.setContent(pingMessage.toByteString()).build();
		ByteString result = connectionToValidator.send(MessageType.PING_REQUEST, mesg.toByteString()).getResult();
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isEmpty());

	}

}
