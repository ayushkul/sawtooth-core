package sawtooth.sdk.reactive.tp.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.tp.stress.MessageGenerator;

public class MyPublisher implements Publisher<Message> {

  public MyPublisher(MessageGenerator generator) {
    super();
    this.generator = generator;
  }

  private final static Logger LOGGER = LoggerFactory.getLogger(MyPublisher.class);
  private MessageGenerator generator;

  private List<MySubscription> subscriptions =
      Collections.synchronizedList(new ArrayList<MySubscription>());

  private final CompletableFuture<Void> terminated = new CompletableFuture<>();

  @Override
  public void subscribe(Subscriber<? super Message> subscriber) {
    MySubscription subscription = new MySubscription(subscriber, generator);

    subscriptions.add(subscription);
    subscriber.onSubscribe(subscription);
    
  }

  public void waitUntilTerminated() throws InterruptedException {
    try {
      terminated.get();
    } catch (ExecutionException e) {
      System.out.println(e);
    }
  }

  private class MySubscription implements Subscription {

    private MessageGenerator internalGenerator;
    private Subscriber<? super Message> subscriber;
    private AtomicBoolean isCanceled;

    public MySubscription(Subscriber<? super Message> subscriber,
        final MessageGenerator generator) {
      this.subscriber = subscriber;

      isCanceled = new AtomicBoolean(false);
    }

    @Override
    public void request(long n) {
      if (isCanceled.get())
        return;
      internalGenerator.getMessagesflux().take(n).toStream().map(m -> {
        publishMessage(m);
        return true;
      });
    }

    @Override
    public void cancel() {
      isCanceled.set(true);

      synchronized (subscriptions) {
        subscriptions.remove(this);
        if (subscriptions.size() == 0)
          subscriber.onComplete();
      }
    }

    private void publishMessage(Message n) {
      subscriber.onNext(n);
    }
  }


}
