package sawtooth.sdk.reactive.tp.processor;

import java.util.Collection;
import java.util.concurrent.Flow.Subscriber;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.exceptions.InternalError;
import sawtooth.sdk.reactive.common.exceptions.InvalidTransactionException;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;

public interface TransactionHandler extends Subscriber<Message> {

  public String transactionFamilyName();

  public String getVersion();

  public Collection<String> getNameSpaces();

  public void apply(BatchList batchRequest, SawtoothState state)
      throws InvalidTransactionException, InternalError;

  public MessageFactory getMessageFactory();

}
