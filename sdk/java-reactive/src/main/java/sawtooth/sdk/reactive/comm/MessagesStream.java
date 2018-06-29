package sawtooth.sdk.reactive.comm;

import java.util.concurrent.Future;

import com.google.protobuf.Message;

import sawtooth.sdk.reactive.factory.MessageFactory;

public abstract class MessagesStream {

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
	 * close the Stream.
	 */
	public abstract void close();

}
