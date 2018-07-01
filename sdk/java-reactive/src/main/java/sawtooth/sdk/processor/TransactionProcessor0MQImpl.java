package sawtooth.sdk.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.messaging.MessagesStream;
import sawtooth.sdk.messaging.Stream0MQImpl;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.PingResponse;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TpProcessResponse;
import sawtooth.sdk.protobuf.TpRegisterRequest;
import sawtooth.sdk.protobuf.TpUnregisterRequest;
import sawtooth.sdk.protobuf.TransactionHeader;
import sawtooth.sdk.reactive.factory.MessageFactory;

public class TransactionProcessor0MQImpl implements TransactionProcessor {

	private MessagesStream stream;
	private static MessageFactory mesgFact;
	private ArrayList<TransactionHandler> handlers;
	private Message currentMessage;
	private boolean registered;
	private final static Logger LOGGER = LoggerFactory.getLogger(TransactionProcessor0MQImpl.class);

	class Shutdown extends Thread {
		@Override
		public void run() {
			LOGGER.info("Start Shutdown of Transaction Processor.");
			if (!registered) {
				return;
			}
			if (getCurrentMessage() != null) {
				LOGGER.info(getCurrentMessage().toString());
			}
			try {
				Message unregisterRequest = mesgFact.getUnregisterRequest();
				LOGGER.info("Send TpUnregisterRequest");
				Future<Message> fut = stream.send(unregisterRequest);
				Message response = fut.get(1, TimeUnit.SECONDS);
				Message message = getCurrentMessage();
				if (message == null) {
					message = stream.receive(1).get();
				}
				LOGGER.info("Finish processing any left over messages.");
				while (message != null) {
					TransactionHandler handler = findHandler(message);
					process(message, stream, handler);
					message = stream.receive(1).get();
				}
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			catch (TimeoutException ter) {
				LOGGER.info("TimeoutException on shutdown");
			}
			catch (ExecutionException e) {
				LOGGER.info(e.toString());
				e.printStackTrace();
			}
		}
	}

	/**
	 * constructor.
	 * @param address the zmq address
	 */
	public TransactionProcessor0MQImpl(String address) {
		this.stream = new Stream0MQImpl(address);
		this.handlers = new ArrayList<TransactionHandler>();
		this.currentMessage = null;
		this.registered = false;
		Runtime.getRuntime().addShutdownHook(new Shutdown());
	}

	/**
	 * add a handler that will be run from within the run method.
	 * @param handler implements that TransactionHandler interface
	 */
	public void addHandler(TransactionHandler handler) {

		try {
			Future<Message> fut = this.stream.send(handler.getMessageFactory().getRegisterRequest());
			fut.get();
			this.registered = true;
			this.handlers.add(handler);
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get the current message that is being processed.
	 */
	private Message getCurrentMessage() {
		return this.currentMessage;
	}

	/**
	 * Used to process a message.
	 * @param message The Message to process.
	 * @param stream The Stream to use to send back responses.
	 * @param handler The handler that should be used to process the message.
	 */
	private static void process(Message message, MessagesStream stream, TransactionHandler handler) {
		try {
			TpProcessRequest transactionRequest = TpProcessRequest.parseFrom(message.getContent());
			SawtoothState state = new State0MQImpl(stream, transactionRequest.getContextId());
			BatchList batchToProcess = mesgFact.createBatch(Arrays.asList(message));
			TpProcessResponse.Builder builder = TpProcessResponse.newBuilder();
			try {
				handler.apply(batchToProcess, state);
				builder.setStatus(TpProcessResponse.Status.OK);
			}
			catch (InvalidTransactionException ite) {
				LOGGER.warn("Invalid Transaction: " + ite.toString());
				builder.setStatus(TpProcessResponse.Status.INVALID_TRANSACTION);
				builder.setMessage(ite.getMessage());
				if (ite.getExtendedData() != null) {
					builder.setExtendedData(ByteString.copyFrom(ite.getExtendedData()));
				}
			}
			catch (InternalError ie) {
				LOGGER.warn("State Exception!: " + ie.toString());
				builder.setStatus(TpProcessResponse.Status.INTERNAL_ERROR);
				builder.setMessage(ie.getMessage());
				if (ie.getExtendedData() != null) {
					builder.setExtendedData(ByteString.copyFrom(ie.getExtendedData()));
				}
			}
			
			stream.sendBack(message.getCorrelationId(), mesgFact.getProcessResponse(message.getCorrelationId(), builder.build()));

		}
		catch (InvalidProtocolBufferException ipbe) {
			LOGGER.info("Received Bytestring that wasn't requested that isn't TransactionProcessRequest");
		}
	}

	/**
	 * Find the handler that should be used to process the given message.
	 * @param message The message that has the TpProcessRequest that the header that will
	 * be checked against the handler.
	 */
	private TransactionHandler findHandler(Message message) {
		try {
			final TransactionHeader header = TpProcessRequest.parseFrom(message.getContent()).getHeader();
			Optional<TransactionHandler> result = this.handlers.stream().filter(eh -> {
				return (eh.transactionFamilyName().equalsIgnoreCase(header.getFamilyName())
						&& (eh.getVersion().equalsIgnoreCase(header.getFamilyVersion())));
			}).findFirst();
			if (result.isPresent())
				return result.get();
			LOGGER.info("Missing handler for header: " + header.toString());
		}
		catch (InvalidProtocolBufferException ipbe) {
			LOGGER.info("Received Message that isn't a TransactionProcessRequest");
			ipbe.printStackTrace();
		}
		return null;
	}

	@Override
	public void run() {
		while (true) {
			if (!this.handlers.isEmpty()) {
				try {
					this.currentMessage = this.stream.receive().get();
				}
				catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				catch (ExecutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (this.currentMessage != null) {
					if (this.currentMessage.getMessageType() == Message.MessageType.PING_REQUEST) {
						LOGGER.info("Received Ping Message.");
						this.stream.sendBack(this.currentMessage.getCorrelationId(), mesgFact.getPingResponse(this.currentMessage.getCorrelationId()));
						this.currentMessage = null;
					}
					else if (this.currentMessage.getMessageType() == Message.MessageType.TP_PROCESS_REQUEST) {
						TransactionHandler handler = this.findHandler(this.currentMessage);
						if (handler == null) {
							break;
						}
						process(this.currentMessage, this.stream, handler);
						this.currentMessage = null;
					}
					else {
						LOGGER.info("Unknown Message Type: " + this.currentMessage.getMessageType());
						this.currentMessage = null;
					}
				}
				else {
					// Disconnect
					LOGGER.info("The Validator disconnected, trying to register.");
					this.registered = false;
					for (int i = 0; i < this.handlers.size(); i++) {
						TransactionHandler handler = this.handlers.get(i);
						
						try {
							Future<Message> fut = this.stream.send(handler.getMessageFactory().getRegisterRequest());
							fut.get();
							this.registered = true;
						}
						catch (InterruptedException ie) {
							LOGGER.warn(ie.toString());
						}
						catch (ExecutionException e) {
							LOGGER.error(e.toString());
							e.printStackTrace();
						}
						
					}
				}
			}
		}
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTransactionProcessorId() {
		// TODO Auto-generated method stub
		return null;
	}
}