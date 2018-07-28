package sawtooth.sdk.reactive.tp.fake;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.exceptions.InternalError;
import sawtooth.sdk.reactive.common.exceptions.InvalidTransactionException;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;
import sawtooth.sdk.reactive.tp.processor.SawtoothState;
import sawtooth.sdk.reactive.tp.processor.TransactionHandler;

/**
 * 
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 *         This implementation is intended to answer simple requests, like PING and
 *         REGISTRATION_REQUEST
 *
 */
public class SimpleTestTransactionHandler implements TransactionHandler {

  private final static Logger LOGGER = LoggerFactory.getLogger(SimpleTestTransactionHandler.class);
  public final static MessageFactory TEST_MESSAGE_FACTORY;
  static {
    MessageFactory tmpMF = null;
    try {
      tmpMF = new MessageFactory("ping", "0.0.0", null, null, "ping");
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    TEST_MESSAGE_FACTORY = tmpMF;
  }


  @Override
  public void onSubscribe(Subscription subscription) {
    LOGGER.debug("onSubscribe -- "+subscription.toString());
  }

  @Override
  public void onNext(Message item) {
    LOGGER.debug("onNext() -- "+item.toString());
  }

  @Override
  public void onError(Throwable throwable) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onComplete() {
    LOGGER.debug("onComplete()");

  }

  @Override
  public String transactionFamilyName() {
    return TEST_MESSAGE_FACTORY.getFamilyName();
  }

  @Override
  public String getVersion() {
    return TEST_MESSAGE_FACTORY.getFamilyVersion();
  }

  @Override
  public Collection<String> getNameSpaces() {
    return Arrays.asList(TEST_MESSAGE_FACTORY.getNameSpaces());
  }

  @Override
  public void apply(BatchList batchRequest, SawtoothState state)
      throws InvalidTransactionException, InternalError {
    // TODO Auto-generated method stub

  }

  @Override
  public MessageFactory getMessageFactory() {
    return TEST_MESSAGE_FACTORY;
  }



}
