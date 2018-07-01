package sawtooth.examples.xo.tests.slim;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import sawtooth.sdk.messaging.Future;
import sawtooth.sdk.messaging.Stream0MQImpl;
import sawtooth.sdk.processor.State0MQImpl;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.PingResponse;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TpProcessResponse;
import sawtooth.sdk.protobuf.TpRegisterRequest;
import sawtooth.sdk.protobuf.TpUnregisterRequest;
import sawtooth.sdk.protobuf.TransactionHeader;

public class XOTransactionProcessor extends AbstractVerticle {

	public final static Logger LOGGER = LoggerFactory.getLogger(XOTransactionProcessor.class);
	public final static String MQBINDADDRESSCONFIGNAME = "tpmqadd";

	private final ThreadLocal<Stream0MQImpl> stream = new ThreadLocal<Stream0MQImpl>();
	private final ThreadLocal<ArrayList<TransactionHandler>> handlers = new ThreadLocal<ArrayList<TransactionHandler>>();
	private final ThreadLocal<Message> currentMessage = new ThreadLocal<Message>();
	private final ThreadLocal<Boolean> registered = new ThreadLocal<Boolean>();
	private final ThreadLocal<String> address = new ThreadLocal<String>();

	/**
	 * add a handler that will be run from within the run method.
	 * @param handler implements that TransactionHandler interface
	 */
	public void addHandler(TransactionHandler handler) {
		TpRegisterRequest registerRequest = TpRegisterRequest.newBuilder().setFamily(handler.transactionFamilyName())
				.addAllNamespaces(handler.getNameSpaces()).setVersion(handler.getVersion()).build();
		try {
			Future fut = stream.get().send(Message.MessageType.TP_REGISTER_REQUEST, registerRequest.toByteString());
			fut.getResult();
			this.registered.set(true);
			this.handlers.get().add(handler);
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		catch (ValidatorConnectionError vce) {
			LOGGER.info(vce.toString());
		}
	}

	/**
	 * Find the handler that should be used to process the given message.
	 * @param message The message that has the TpProcessRequest that the header that will
	 * be checked against the handler.
	 */
	private TransactionHandler findHandler(Message message) {
		try {
			TpProcessRequest transactionRequest = TpProcessRequest.parseFrom(this.currentMessage.get().getContent());
			TransactionHeader header = transactionRequest.getHeader();
			for (int i = 0; i < this.handlers.get().size(); i++) {
				TransactionHandler handler = this.handlers.get().get(i);
				if (header.getFamilyName().equals(handler.transactionFamilyName())
						&& header.getFamilyVersion().equals(handler.getVersion())) {
					return handler;
				}
			}
			LOGGER.info("Missing handler for header: " + header.toString());
		}
		catch (InvalidProtocolBufferException ipbe) {
			LOGGER.info("Received Message that isn't a TransactionProcessRequest");
			ipbe.printStackTrace();
		}
		return null;
	}

	/**
	 * Used to process a message.
	 * @param message The Message to process.
	 * @param stream The Stream to use to send back responses.
	 * @param handler The handler that should be used to process the message.
	 */
	private void process(Message message, Stream0MQImpl stream, TransactionHandler handler) {
		LOGGER.debug("process() " + handler.transactionFamilyName());
		try {
			TpProcessRequest transactionRequest = TpProcessRequest.parseFrom(message.getContent());
			State0MQImpl state = new State0MQImpl(stream, transactionRequest.getContextId());

			TpProcessResponse.Builder builder = TpProcessResponse.newBuilder();
			try {
				handler.apply(transactionRequest, state);
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
			stream.sendBack(Message.MessageType.TP_PROCESS_RESPONSE, message.getCorrelationId(),
					builder.build().toByteString());

		}
		catch (InvalidProtocolBufferException ipbe) {
			LOGGER.info("Received Bytestring that wasn't requested that isn't TransactionProcessRequest");
		}
	}

	@Override
	public void start() {

		this.address.set(config().getString(MQBINDADDRESSCONFIGNAME));
		MessageConsumer<String> commandConsumer = vertx.eventBus().consumer(this.address.get());
		commandConsumer.handler(command ->{
			LOGGER.debug(Thread.currentThread().getName() + " received command : " + command.body());
			command.reply(" received command : " + command.body(), rpt ->{
				if (rpt.succeeded()) {
					LOGGER.debug(Thread.currentThread().getName() + " -- replied to " + command.replyAddress());
				}
				else {
					LOGGER.error(rpt.cause().getMessage());
					rpt.cause().printStackTrace();
				}
			});
		}).endHandler(eh -> {
			LOGGER.debug(Thread.currentThread().getName() + " -- ENDED  "+eh.toString());
		}).exceptionHandler(xh -> {
			LOGGER.debug(Thread.currentThread().getName() + " -- FAILED "+xh.getMessage());
			xh.printStackTrace();
		});
		vertx.eventBus().send(this.address.get(),"LALA");
		LOGGER.debug(Thread.currentThread().getName() + " -- start() for URI " + this.address.get());
		this.stream.set(new Stream0MQImpl(this.address.get()));
		this.handlers.set(new ArrayList<TransactionHandler>());
		this.currentMessage.set(null);
		while (true) {
			if (!this.handlers.get().isEmpty()) {
				this.currentMessage.set(this.stream.get().receive());
				if (this.currentMessage != null) {
					if (this.currentMessage.get().getMessageType() == Message.MessageType.PING_REQUEST) {
						LOGGER.info("Recieved Ping Message.");
						PingResponse pingResponse = PingResponse.newBuilder().build();
						this.stream.get().sendBack(Message.MessageType.PING_RESPONSE,
								currentMessage.get().getCorrelationId(), pingResponse.toByteString());
						this.currentMessage.set(null);
					}
					else if (this.currentMessage.get().getMessageType() == Message.MessageType.TP_PROCESS_REQUEST) {
						TransactionHandler handler = findHandler(this.currentMessage.get());
						if (handler == null) {
							break;
						}
						process(this.currentMessage.get(), this.stream.get(), handler);
						this.currentMessage.set(null);
					}
					else {
						LOGGER.info("Unknown Message Type: " + this.currentMessage.get().getMessageType());
						this.currentMessage.set(null);
					}
				}
				else {
					// Disconnect
					LOGGER.info("The Validator disconnected, trying to register.");
					this.registered.set(false);
					for (int i = 0; i < this.handlers.get().size(); i++) {
						TransactionHandler handler = this.handlers.get().get(i);
						TpRegisterRequest registerRequest = TpRegisterRequest.newBuilder()
								.setFamily(handler.transactionFamilyName()).addAllNamespaces(handler.getNameSpaces())
								.setVersion(handler.getVersion()).build();

						try {
							Future fut = stream.get().send(Message.MessageType.TP_REGISTER_REQUEST,
									registerRequest.toByteString());
							fut.getResult();
							this.registered.set(true);
						}
						catch (InterruptedException ie) {
							LOGGER.warn(ie.toString());
						}
						catch (ValidatorConnectionError vce) {
							LOGGER.warn(vce.toString());
						}
					}
				}
			}
		}
	}

	/**
	 * Get the current message that is being processed.
	 */
	private Message getCurrentMessage() {
		return this.currentMessage.get();
	}

	@Override
	public void stop() {
		LOGGER.info("Start Shutdown of Transaction Processor.");
		if (!registered.get()) {
			return;
		}
		if (getCurrentMessage() != null) {
			LOGGER.info(getCurrentMessage().toString());
		}
		try {
			TpUnregisterRequest unregisterRequest = TpUnregisterRequest.newBuilder().build();
			LOGGER.info("Send TpUnregisterRequest");
			Future fut = stream.get().send(Message.MessageType.TP_UNREGISTER_REQUEST, unregisterRequest.toByteString());
			ByteString response = fut.getResult(1);
			Message message = getCurrentMessage();
			if (message == null) {
				message = stream.get().receive(1);
			}
			LOGGER.info("Finish processing any left over messages.");
			while (message != null) {
				TransactionHandler handler = findHandler(message);
				process(message, stream.get(), handler);
				message = stream.get().receive(1);
			}
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		catch (TimeoutException ter) {
			LOGGER.info("TimeoutException on shutdown");
		}
		catch (ValidatorConnectionError vce) {
			LOGGER.info(vce.toString());
		}
	}
}
