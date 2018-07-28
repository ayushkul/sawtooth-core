package sawtooth.sdk.reactive.tp.evaluation;


import static java.lang.Thread.currentThread;
import java.util.Random;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.InvalidProtocolBufferException;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.TpRegisterResponse;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;

public class MySubscriber implements Subscriber<Message> {

  private final static Logger LOGGER = LoggerFactory.getLogger(MySubscriber.class);

  private static final String LOG_MESSAGE_FORMAT = "Subscriber %s >> [%s] %s%n";

  private static final int DEMAND = 3;
  private static final Random RANDOM = new Random();

  private String name;
  private Subscription subscription;
  private MessageFactory internalMF;

  public MySubscriber(MessageFactory internalMF) {
    super();
    this.internalMF = internalMF;
  }

  private int count;

  @Override
  public void onSubscribe(Subscription subscription) {
    log("Subscribed");
    this.subscription = subscription;

    count = DEMAND;
    requestItems(DEMAND);
  }

  private void requestItems(int n) {
    log("Requesting %d new items...", n);
    subscription.request(n);
  }

  private Message getResponse(Message request) throws InvalidProtocolBufferException {
    Message answer = null;
    switch (request.getMessageTypeValue()) {
      case Message.MessageType.PING_REQUEST_VALUE:
        LOGGER.debug("Answering PING_REQUEST ");
        answer = internalMF.getPingResponse(request.getCorrelationId());
        break;
      case Message.MessageType.PING_RESPONSE_VALUE:
        LOGGER.debug("Receiving PING_RESPONSE");
        return null;
      case Message.MessageType.TP_REGISTER_REQUEST_VALUE:
        LOGGER
            .debug("Answering REGISTER_REQUEST with correlation ID " + request.getCorrelationId());
        answer = internalMF.getRegisterResponse(TpRegisterResponse.Status.OK_VALUE,
            request.getCorrelationId());
        break;
    }

    return answer;
  }

  @Override
  public void onNext(Message item) {
    if (item != null) {
      log(item.toString());

      synchronized (this) {
        count--;

        if (count == 0) {
          if (RANDOM.nextBoolean()) {
            count = DEMAND;
            requestItems(count);
          } else {
            count = 0;
            log("Cancelling subscription...");
            subscription.cancel();
          }
        }
      }
    } else {
      log("Null Item!");
    }
  }

  @Override
  public void onComplete() {
    log("Complete!");
  }

  @Override
  public void onError(Throwable t) {
    log("Subscriber Error >> %s", t);
  }

  private void log(String message, Object... args) {
    String fullMessage =
        String.format(LOG_MESSAGE_FORMAT, this.name, currentThread().getName(), message, args);

    LOGGER.debug(fullMessage);
  }

}
