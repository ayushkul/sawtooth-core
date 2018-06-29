package sawtooth.sdk.reactive.factory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.BatchHeader;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Event;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.protobuf.PingRequest;
import sawtooth.sdk.protobuf.PingResponse;
import sawtooth.sdk.protobuf.TpEventAddRequest;
import sawtooth.sdk.protobuf.TpEventAddResponse;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TpRegisterRequest;
import sawtooth.sdk.protobuf.TpRegisterResponse;
import sawtooth.sdk.protobuf.TpStateDeleteRequest;
import sawtooth.sdk.protobuf.TpStateDeleteResponse;
import sawtooth.sdk.protobuf.TpStateEntry;
import sawtooth.sdk.protobuf.TpStateGetRequest;
import sawtooth.sdk.protobuf.TpStateGetResponse;
import sawtooth.sdk.protobuf.TpStateSetRequest;
import sawtooth.sdk.protobuf.TpStateSetResponse;
import sawtooth.sdk.protobuf.Transaction;
import sawtooth.sdk.protobuf.TransactionHeader;
import sawtooth.sdk.reactive.client.Signing;

public class MessageFactory {

	final String[] nameSpaces;
	final String familyName;
	final String familyVersion;
	private final MessageDigest MESSAGEDIGESTER;

	private final static Logger LOGGER = LoggerFactory.getLogger(MessageFactory.class);

	ECKey signerPrivateKey;

	public MessageFactory(String familyName, String familyVersion, ECKey privateKey, String... nameSpaces)
			throws NoSuchAlgorithmException {
		this(familyName, "SHA-512", familyVersion, privateKey, nameSpaces);
	}

	public MessageFactory(String familyName, String digesterAlgo, String familyVersion, ECKey privateKey,
			String... nameSpaces) throws NoSuchAlgorithmException {
		MESSAGEDIGESTER = MessageDigest.getInstance(digesterAlgo);
		this.familyName = familyName;
		this.familyVersion = familyVersion;
		if (privateKey == null) {
			LOGGER.warn("Private Key null, creating a temporary one...");
			this.signerPrivateKey = Signing.generatePrivateKey(new SecureRandom(
					ByteBuffer.allocate(Long.BYTES).putLong(Calendar.getInstance().getTimeInMillis()).array()));
			LOGGER.warn("Created with encryption " + this.signerPrivateKey.getEncryptionType().toString()
					+ " and Key Crypter " + this.signerPrivateKey.getKeyCrypter().toString());
		}
		else {
			this.signerPrivateKey = privateKey;
		}
		List<String> binNameSpaces = new ArrayList<String>();
		for (String eachNS : nameSpaces) {
			binNameSpaces
					.add(MESSAGEDIGESTER.digest(eachNS.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 6));
		}
		this.nameSpaces = new String[nameSpaces.length];
		binNameSpaces.toArray(this.nameSpaces);

	}

	public TransactionHeader createTransactionHeader(StringBuffer payload, List<String> inputs, List<String> outputs,
			List<String> dependencies, boolean needsNonce, ECKey batcherPubKey) throws NoSuchAlgorithmException {
		TransactionHeader.Builder thBuilder = TransactionHeader.newBuilder();
		thBuilder.setFamilyName(familyName);
		thBuilder.setFamilyVersion(familyVersion);
		thBuilder.setSignerPublicKey(Signing.getPublicKey(signerPrivateKey));
		thBuilder.setBatcherPublicKey(
				batcherPubKey != null ? batcherPubKey.getPublicKeyAsHex() : thBuilder.getSignerPublicKey());
		thBuilder.setPayloadSha512(new String(
				MESSAGEDIGESTER.digest(payload.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		if (needsNonce) {
			thBuilder.setNonce(String.valueOf(Calendar.getInstance().getTimeInMillis()));
		}

		if (dependencies != null && !dependencies.isEmpty()) {
			thBuilder.addAllDependencies(dependencies);
		}

		if (inputs != null && !inputs.isEmpty()) {
			thBuilder.addAllInputs(inputs);
		}

		if (outputs != null && !outputs.isEmpty()) {
			thBuilder.addAllOutputs(outputs);
		}

		return thBuilder.build();
	}

	public String createSignature(TransactionHeader header) {
		return Signing.sign(signerPrivateKey, header.toByteArray());
	}

	public boolean isValidMerkleAddress(String merkleAddress) {
		return merkleAddress != null && !merkleAddress.isEmpty() && merkleAddress.length() == 70
				&& !merkleAddress.toLowerCase().chars().filter(c -> {
					return Character.digit(c, 16) == -1;
				}).findFirst().isPresent();
	}

	public Message getRegisterRequest() {
		Message newMessage = Message.newBuilder().setContent(createTpRegisterRequest().toByteString())
				.setCorrelationId(this.generateId()).setMessageType(MessageType.TP_REGISTER_REQUEST).build();

		return newMessage;
	}

	private TpRegisterRequest createTpRegisterRequest() {
		TpRegisterRequest.Builder reqBuilder = TpRegisterRequest.newBuilder();

		reqBuilder.setFamily(familyName);
		reqBuilder.addAllNamespaces(Arrays.asList(nameSpaces));
		reqBuilder.setVersion(familyVersion);

		return reqBuilder.build();
	}

	public Message getRegisterResponse(int status, String correlationId) {
		Message newMessage = Message.newBuilder().setContent(createTpRegisterResponse(status).toByteString())
				.setCorrelationId(correlationId).setMessageType(MessageType.TP_REGISTER_RESPONSE).build();

		return newMessage;
	}

	private TpRegisterResponse createTpRegisterResponse(int status) {
		TpRegisterResponse.Builder reqBuilder = TpRegisterResponse.newBuilder();

		reqBuilder.setStatusValue(status);

		return reqBuilder.build();
	}

	public Message getPingRequest() {
		Message newMessage = Message.newBuilder().setContent(createPingRequest().toByteString())
				.setCorrelationId(this.generateId()).setMessageType(MessageType.PING_REQUEST).build();

		return newMessage;
	}

	private PingRequest createPingRequest() {
		PingRequest ping = PingRequest.newBuilder().build();
		return ping;
	}

	public Message getPingResponse(String correlationId) {
		Message newMessage = Message.newBuilder().setContent(createPingResponse().toByteString())
				.setCorrelationId(correlationId).setMessageType(MessageType.PING_RESPONSE).build();

		return newMessage;
	}

	private PingResponse createPingResponse() {
		PingResponse pong = PingResponse.newBuilder().build();
		return pong;
	}

	private TpProcessRequest createTpProcessRequest(String contextId, StringBuffer payload, List<String> inputs,
			List<String> outputs, List<String> dependencies, ECKey batcherPubKey) throws NoSuchAlgorithmException {
		TpProcessRequest.Builder reqBuilder = TpProcessRequest.newBuilder();

		reqBuilder.setContextId(contextId);
		reqBuilder.setHeader(
				createTransactionHeader(payload, inputs, outputs, dependencies, Boolean.TRUE, batcherPubKey));
		reqBuilder.setPayload(
				ByteString.copyFrom(MESSAGEDIGESTER.digest(payload.toString().getBytes(StandardCharsets.UTF_8))));
		reqBuilder.setSignature(createSignature(reqBuilder.getHeader()));

		return reqBuilder.build();
	}

	private TpStateGetResponse createTpStateGetResponse(List<TpStateEntry> entries) {
		Optional<TpStateEntry> wrongAddressEntry = entries.stream()
				.filter(str -> !isValidMerkleAddress(str.getAddress())).findFirst();
		if (wrongAddressEntry.isPresent()) {
			LOGGER.error("Invalid Address for TpStateEntry : " + wrongAddressEntry.get().getAddress());
			return null;
		}

		TpStateGetResponse.Builder reqBuilder = TpStateGetResponse.newBuilder();
		reqBuilder.addAllEntries(entries);
		return reqBuilder.build();

	}

	private TpStateGetRequest createTpStateGetRequest(List<String> addresses) {
		Optional<String> wrongAddress = addresses.stream().filter(str -> !isValidMerkleAddress(str)).findFirst();
		if (wrongAddress.isPresent()) {
			LOGGER.error("Invalid Address " + wrongAddress.get());
			return null;
		}

		TpStateGetRequest.Builder reqBuilder = TpStateGetRequest.newBuilder();
		reqBuilder.addAllAddresses(addresses);
		return reqBuilder.build();

	}

	private TpStateSetRequest createTpStateSetRequest(Map<String, TpStateEntry> addressDataMap) {
		Optional<String> wrongAddress = addressDataMap.keySet().stream().filter(str -> !isValidMerkleAddress(str))
				.findFirst();
		if (wrongAddress.isPresent()) {
			LOGGER.error("Invalid Address " + wrongAddress.get());
			return null;
		}

		TpStateSetRequest.Builder reqBuilder = TpStateSetRequest.newBuilder();

		TpStateEntry.Builder stateBuilder = TpStateEntry.newBuilder();

		reqBuilder.addAllEntries(addressDataMap.entrySet().stream().sequential().map(es -> {
			stateBuilder.clear();
			stateBuilder.setAddress(es.getKey());
			stateBuilder.setData(es.getValue().getData());
			return stateBuilder.build();
		}).collect(Collectors.toList()));

		return reqBuilder.build();

	}

	private TpStateSetResponse createTpStateSetResponse(List<String> addresses) {
		Optional<String> wrongAddress = addresses.stream().filter(str -> !isValidMerkleAddress(str)).findFirst();
		if (wrongAddress.isPresent()) {
			LOGGER.error("Invalid Address " + wrongAddress.get());
			return null;
		}

		TpStateSetResponse.Builder reqBuilder = TpStateSetResponse.newBuilder();
		reqBuilder.addAllAddresses(addresses);
		return reqBuilder.build();

	}

	private TpStateDeleteRequest createTpStateDeleteRequest(List<String> addresses) {
		Optional<String> wrongAddress = addresses.stream().filter(str -> !isValidMerkleAddress(str)).findFirst();
		if (wrongAddress.isPresent()) {
			LOGGER.error("Invalid Address " + wrongAddress.get());
			return null;
		}

		TpStateDeleteRequest.Builder reqBuilder = TpStateDeleteRequest.newBuilder();

		reqBuilder.addAllAddresses(addresses);
		return reqBuilder.build();
	}

	private TpStateDeleteResponse createTpStateDeleteResponse(List<String> addresses) {
		Optional<String> wrongAddress = addresses.stream().filter(str -> !isValidMerkleAddress(str)).findFirst();
		if (wrongAddress.isPresent()) {
			LOGGER.error("Invalid Address " + wrongAddress.get());
			return null;
		}

		TpStateDeleteResponse.Builder reqBuilder = TpStateDeleteResponse.newBuilder();

		reqBuilder.addAllAddresses(addresses);
		return reqBuilder.build();
	}

	private TpEventAddRequest createTpEventAddRequest(String contextId, String eventType,
			List<Event.Attribute> attributes, ByteString data) {

		TpEventAddRequest.Builder reqBuilder = TpEventAddRequest.newBuilder();

		Event.Builder eventBuilder = Event.newBuilder();
		eventBuilder.setData(data);
		eventBuilder.setEventType(eventType);
		eventBuilder.addAllAttributes(attributes);

		reqBuilder.setContextId(contextId);
		reqBuilder.setEvent(eventBuilder.build());

		return reqBuilder.build();
	}

	private TpEventAddResponse createTpEventAddResponse() {
		return TpEventAddResponse.newBuilder().setStatus(TpEventAddResponse.Status.OK).build();
	}

	private Transaction createTransaction(StringBuffer payload, List<String> inputs, List<String> outputs,
			List<String> dependencies, ECKey batcherPubKey) throws NoSuchAlgorithmException {
		TransactionHeader header = createTransactionHeader(payload, inputs, outputs, dependencies, Boolean.TRUE,
				batcherPubKey);
		Transaction.Builder transactionBuilder = Transaction.newBuilder();

		transactionBuilder.setHeader(header.toByteString());
		transactionBuilder.setHeaderSignature(createSignature(header));
		transactionBuilder.setPayload(
				ByteString.copyFrom(MESSAGEDIGESTER.digest(payload.toString().getBytes(StandardCharsets.UTF_8))));

		return transactionBuilder.build();
	}

	public BatchList createBatch(List<Message> transactions) {

		List<Transaction> tempTransactionList = new ArrayList<Transaction>();
		Transaction.Builder transactionBuilder = Transaction.newBuilder();

		List<String> txnSignatures = transactions.stream().map(et -> {
			String result = "";
			try {
				if (et.getMessageType().equals(MessageType.TP_PROCESS_REQUEST)) {
					transactionBuilder.clear();
					TpProcessRequest theRequest = TpProcessRequest.parseFrom(et.getContent());
					transactionBuilder.setHeader(theRequest.getHeader().toByteString());
					transactionBuilder.setPayload(theRequest.getPayload());
					tempTransactionList.add(transactionBuilder.build());
					result = theRequest.getSignature();
				}
				else {
					Transaction theTransaction;

					theTransaction = Transaction.parseFrom(et.getContent());

					tempTransactionList.add(theTransaction);
					result = theTransaction.getHeaderSignature();
				}
			}
			catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
		}).collect(Collectors.toList());

		BatchHeader batHeader = BatchHeader.newBuilder().addAllTransactionIds(txnSignatures)
				.setSignerPublicKey(signerPrivateKey.getPublicKeyAsHex()).build();
		String headerSignature = signerPrivateKey.signMessage(batHeader.toString());
		Batch.Builder batchBuilder = Batch.newBuilder();
		Batch batch = batchBuilder.setHeader(batHeader.toByteString()).setHeaderSignature(headerSignature)
				.addAllTransactions(tempTransactionList).build();
		BatchList.Builder batchListBuilder = BatchList.newBuilder();
		batchListBuilder.addBatches(batch);

		return batchListBuilder.build();
	}

	/**
	 * generate a random String using the sha-256 algorithm, to correlate sent messages.
	 * with futures
	 *
	 * @return a random String
	 */
	private String generateId() {
		return new String(MESSAGEDIGESTER.digest(UUID.randomUUID().toString().getBytes()));
	}
}
