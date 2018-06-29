package sawtooth.sdk.reactive.factory;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.BatchHeader;
import sawtooth.sdk.protobuf.Transaction;
import sawtooth.sdk.protobuf.TransactionHeader;
import sawtooth.sdk.reactive.rest.model.RESTBatch;
import sawtooth.sdk.reactive.rest.model.RESTBatchHeader;
import sawtooth.sdk.reactive.rest.model.RESTTransaction;
import sawtooth.sdk.reactive.rest.model.RESTTransactionHeader;

public class RESTFactory {

	public static RESTBatch fromBatch(Batch protbufBatch) throws InvalidProtocolBufferException {
		RESTBatch manufact = new RESTBatch();
		manufact.setHeader(fromBatchHEader(BatchHeader.parseFrom(protbufBatch.getHeader())));
		manufact.setHeaderSignature(protbufBatch.getHeaderSignature());
		manufact.setTransactions(protbufBatch.getTransactionsList().stream().map(et -> {
			try {
				return fromTransaction(et);
			}
			catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}).collect(Collectors.toList()));
		return manufact;
	}

	public static RESTBatchHeader fromBatchHEader(BatchHeader protbufBatchHeader) {
		RESTBatchHeader manufact = new RESTBatchHeader();
		manufact.setSignerPublicKey(protbufBatchHeader.getSignerPublicKey());
		manufact.setTransactionIds(protbufBatchHeader.getTransactionIdsList());
		return manufact;
	}
	
	public static RESTTransactionHeader fromTransactionHeader(TransactionHeader protbufBatchHeader) {
		RESTTransactionHeader manufact = new RESTTransactionHeader();
		manufact.setBatcherPublicKey(protbufBatchHeader.getBatcherPublicKey());
		manufact.setDependencies(protbufBatchHeader.getDependenciesList());
		manufact.setFamilyName(protbufBatchHeader.getFamilyName());
		manufact.setFamilyVersion(protbufBatchHeader.getFamilyVersion());
		manufact.setInputs(protbufBatchHeader.getInputsList());
		manufact.setNonce(protbufBatchHeader.getNonce());
		manufact.setOutputs(protbufBatchHeader.getOutputsList());
		manufact.setPayloadSha512(protbufBatchHeader.getPayloadSha512());
		manufact.setSignerPublicKey(protbufBatchHeader.getSignerPublicKey());
		
		return manufact;
	}

	public static RESTTransaction fromTransaction(Transaction protbufTransaction) throws InvalidProtocolBufferException {
		RESTTransaction manufact = new RESTTransaction();
		manufact.setHeader(fromTransactionHeader(TransactionHeader.parseFrom(protbufTransaction.getHeader())));
		manufact.setHeaderSignature(protbufTransaction.getHeaderSignature());
		manufact.setPayload(protbufTransaction.getPayload().toString(StandardCharsets.UTF_8));
		return manufact;
	}

}
