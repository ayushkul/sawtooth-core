package sawtooth.sdk.reactive.tp.evaluation;

import java.security.NoSuchAlgorithmException;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;
import sawtooth.sdk.reactive.tp.stress.MessageGenerator;

public class MainApp {

  public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {

    MessageFactory echo = new MessageFactory("echo", "echo", null, null, "echo");
    MyPublisher publisher = new MyPublisher(new MessageGenerator(echo));
    MySubscriber subscriberA = new MySubscriber(echo);
    MySubscriber subscriberB = new MySubscriber(echo);

    publisher.subscribe(subscriberA);
    publisher.subscribe(subscriberB);

    publisher.waitUntilTerminated();
  }
}
