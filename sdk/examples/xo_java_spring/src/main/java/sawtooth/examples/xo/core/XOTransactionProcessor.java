package sawtooth.examples.xo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.examples.xo.XoHandler;
import sawtooth.examples.xo.spring.events.RegisterTransactionHandlerEvent;
import sawtooth.examples.xo.spring.services.ZMQProxyService;
import sawtooth.sdk.events.ControlEvent;
import sawtooth.sdk.messaging.Future;
import sawtooth.sdk.messaging.Stream;
import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.TransactionProcessorInterface;
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

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 * <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 * <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * This will be a singleton
 *
 */
@Component(XOTransactionProcessor.XOPROCESSORBEANNAME)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
@DependsOn(value = { ZMQProxyService.ZMQPROXYSERVICEBEANNAME, "testconfig" })
public class XOTransactionProcessor extends Thread implements TransactionProcessorInterface {

	private final static Logger LOGGER = LoggerFactory.getLogger(XOTransactionProcessor.class);
	public final static String MQBINDADDRESSCONFIGNAME = "tpmqadd";
	public final static String XOPROCESSORIDCONFIGNAME = "xprocid";
	public final static String XOPROCESSORBEANNAME = "XOTransactionProcessor";

	private final ThreadLocal<Stream> stream = new ThreadLocal<Stream>();
	private final ThreadLocal<ArrayList<TransactionHandler>> handlers = new ThreadLocal<ArrayList<TransactionHandler>>();
	private final ThreadLocal<Message> currentMessage = new ThreadLocal<Message>();
	private final ThreadLocal<Boolean> registered = new ThreadLocal<Boolean>();
	private final ThreadLocal<Boolean> keepRunning = new ThreadLocal<Boolean>();
	private final ThreadLocal<String> address = new ThreadLocal<String>();
	private final ThreadLocal<String> myId = new ThreadLocal<String>();

    @Autowired(required=true)
    @Qualifier(value="testconfig")
    private Properties configProperties;
    
	public XOTransactionProcessor() {
		setDaemon(true);
	}
	

	@EventListener(classes = { RegisterTransactionHandlerEvent.class })
	public void registerNewHandler(ControlEvent event) {
		RegisterTransactionHandlerEvent eventInstance = ((RegisterTransactionHandlerEvent) event);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Received RegisterTransactionHandlerEvent for id " + eventInstance.getIdProcessor()
					+ " and class " + eventInstance.getHandlerClassName());
		}
		if (!eventInstance.getIdProcessor().equalsIgnoreCase(myId.get())) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Ignoring RegisterTransactionHandlerEvent for id " + eventInstance.getIdProcessor()
						+ " and class " + eventInstance.getHandlerClassName());
			}
			LOGGER.info("RegisterTransactionHandlerEvent for id " + eventInstance.getIdProcessor() + " received "
					+ myId.get());
			return;
		}
		TransactionHandler newTH = null;
		try {
			newTH = (TransactionHandler) Class
					.forName(eventInstance.getHandlerClassName(), false, this.getClass().getClassLoader())
					.newInstance();
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			LOGGER.error("Error registering handler to class " + eventInstance.getHandlerClassName());
			e.printStackTrace();
			return;
		}

		addHandler(newTH);
	}

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
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Handler registered -- " + handler.transactionFamilyName());
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
	private void process(Message message, Stream stream, TransactionHandler handler) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("process() " + handler.transactionFamilyName());
		try {
			TpProcessRequest transactionRequest = TpProcessRequest.parseFrom(message.getContent());
			sawtooth.sdk.processor.State state = new sawtooth.sdk.processor.State(stream,
					transactionRequest.getContextId());

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
	public void run() {
		this.registered.set(false);
		this.handlers.set(new ArrayList<TransactionHandler>());
		this.address.set(configProperties.getProperty(MQBINDADDRESSCONFIGNAME,"tcp://*:4444"));
		this.myId.set(configProperties.getProperty(XOPROCESSORIDCONFIGNAME,"XOTransactionProcessor-1"));
		final Thread mainThread = Thread.currentThread();
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(Thread.currentThread().getName() + " -- start() for id " + this.getTransactionHandlerId()
					+ " on URI " + this.address.get());
		this.stream.set(new Stream(this.address.get()));
		this.currentMessage.set(null);
		keepRunning.set(true);
		addHandler(new XoHandler());
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(Thread.currentThread().getName() + " binding shutdown hook for id "
					+ this.getTransactionHandlerId());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				keepRunning.set(false);
				shutdown();
				try {
					mainThread.join();
				}
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(
					Thread.currentThread().getName() + " starting main loop for id " + this.getTransactionHandlerId());
		while (keepRunning.get() && !Thread.currentThread().isInterrupted()) {
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
			} else {
				LOGGER.info("Waiting 1 sec to a Handler to be registered...");
				try {
					sleep(1000);
				}
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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

	public void shutdown() {
		LOGGER.info("Starting Shutdown of Transaction Processor.");
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

	public String getTransactionHandlerId() {
		return myId.get();
	}

}
