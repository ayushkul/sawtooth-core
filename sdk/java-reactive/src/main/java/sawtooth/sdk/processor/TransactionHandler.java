package sawtooth.sdk.processor;

import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.reactive.factory.MessageFactory;

import java.util.Collection;
import java.util.concurrent.Flow.Subscriber;

public interface TransactionHandler extends Subscriber<Message>{

	public String transactionFamilyName();

	public String getVersion();

	public Collection<String> getNameSpaces();

	public void apply(BatchList batchRequest, SawtoothState state) throws InvalidTransactionException, InternalError;

	public MessageFactory getMessageFactory();

}
