package sawtooth.sdk.reactive.tp.messaging;

import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeoutException;

import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.messages.MessageFactory;

public abstract class MessagesStream extends SubmissionPublisher<Message> {

	/**
	 * Send a message and return a Future that will later have the Bytestring.
	 * @param payload - the Message being sent
	 * @return future a future that will hold the answer
	 */
	public abstract Future<Message> send(Message payload);

	/**
	 * Send a message without getting a future back. Useful for sending a response message
	 * to, for example, a transaction
	 * @param correlationId a random string generated on the server for the client to send
	 * back
	 * @param payload - the Message the server is expecting
	 */
	public abstract void sendBack(String correlationId, Message payload);

	/**
	 * Set the message factory to use in this Stream. It will manage the semantics of the
	 * generated and received messages.
	 * 
	 * @param mFactory - the *INITIALIZED* Message Factory
	 */

	public abstract void setMessageFactory(MessageFactory mFactory);

	/**
	 * Get a message that has been received.
	 * @return result, a protobuf Message
	 */
	public abstract Future<Message> receive();

	/**
	 * Get a message that has been received. If the timeout is expired, throws
	 * TimeoutException.
	 * @param timeout time to wait for a message.
	 * @return result, a protobuf Message
	 */
	public abstract Future<Message> receive(long timeout) throws TimeoutException;

	/**
	 * close the Stream.
	 */
	public abstract void close();

}
