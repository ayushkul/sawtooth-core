/* Copyright 2016,  2017 Intel Corporation
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

package sawtooth.sdk.messaging;

import com.google.protobuf.ByteString;

import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.factory.MessageFactory;

import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The client networking class.
 */
public class Stream0MQImpl extends MessagesStream {
	private ConcurrentHashMap<String, Future<Message>> futureHashMap;
	private LinkedBlockingQueue<SendReceiveThread.MessageWrapper> receiveQueue;
	private SendReceiveThread sendReceiveThread;
	private Thread thread;
	private MessageFactory mesgFact;

	/**
	 * The constructor.
	 *
	 * @param address the zmq address.
	 *
	 */
	public Stream0MQImpl(String address) {
		this.futureHashMap = new ConcurrentHashMap<String, Future<Message>>();
		this.receiveQueue = new LinkedBlockingQueue<SendReceiveThread.MessageWrapper>();
		this.sendReceiveThread = new SendReceiveThread(address, futureHashMap, this.receiveQueue);
		this.thread = new Thread(sendReceiveThread);
		this.thread.start();
	}

	/**
	 * Send a message and return a Future that will later have the Bytestring.
	 * @param destination one of the Message.MessageType enum values defined in
	 * validator.proto
	 * @param contents the ByteString that has been serialized from a Protobuf class
	 * @return future a future that will have ByteString that can be deserialized into a,
	 * for example, GetResponse
	 */
	@Override
	public Future<Message> send(Message payload) {

		Future<Message>	future = this.sendReceiveThread.sendMessage(payload); 
		this.futureHashMap.put(payload.getCorrelationId(), future);
		return future;
	}

	/**
	 * Send a message without getting a future back. Useful for sending a response message
	 * to, for example, a transaction
	 * @param destination Message.MessageType defined in validator.proto
	 * @param correlationId a random string generated on the server for the client to send
	 * back
	 * @param contents ByteString serialized contents that the server is expecting
	 */
	@Override
	public void sendBack(String correlationId, Message payload) {
		this.sendReceiveThread.sendMessage(payload);
	}

	/**
	 * close the Stream.
	 */
	@Override
	public void close() {
		try {
			this.sendReceiveThread.stop();
			this.thread.join();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	/**
	 * Get a message that has been received.
	 * @return result, a protobuf Message
	 */
	@Override
	public Future<Message> receive() {
		SendReceiveThread.MessageWrapper result = null;
		try {
			result = this.receiveQueue.take();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return CompletableFuture.completedFuture(result.message);
	}

	/**
	 * Get a message that has been received. If the timeout is expired, throws
	 * TimeoutException.
	 * @param timeout time to wait for a message.
	 * @return result, a protobuf Message
	 */
	@Override
	public Future<Message> receive(long timeout) throws TimeoutException {
		SendReceiveThread.MessageWrapper result = null;
		try {
			result = this.receiveQueue.poll(timeout, TimeUnit.SECONDS);
			if (result == null) {
				throw new TimeoutException("The recieve queue timed out.");
			}
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return CompletableFuture.completedFuture(result.message);
	}

	@Override
	public void setMessageFactory(MessageFactory mFactory) {
		mesgFact = mFactory;

	}

}
