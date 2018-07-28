package sawtooth.sdk.reactive.tp.stress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZLoop;
import org.zeromq.ZLoop.IZLoopHandler;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import com.google.protobuf.InvalidProtocolBufferException;
import reactor.core.publisher.Flux;
import reactor.adapter.JdkFlowAdapter;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.TpRegisterRequest;
import sawtooth.sdk.protobuf.TpRegisterResponse;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;
import sawtooth.sdk.reactive.tp.stress.MessageGenerator;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 *         This class intends to mimic a validator, but sending a LOT of messages.
 *
 */
public class StressValidator implements Runnable {
  private final static Logger LOGGER = LoggerFactory.getLogger(StressValidator.class);

  MessageFactory internfalMF;
  MessageGenerator internalMGenerator;
  ZContext context = new ZContext();
  Socket serverSocket;
  String mqServerAddress;
  private static Flux<Message> responseFlux;
  MessageFactory internalMF; 
  Flow.Publisher<Message> echoListener;


  public StressValidator(MessageFactory source, String mqAddress) {
    LOGGER.debug("Registering Message Factory of family " + source.getFamilyName());
    this.internalMF = source;
    this.mqServerAddress = mqAddress;
    echoListener = new Publisher<Message>() {
      private List<ReceiverSubscription> subscriptions = Collections.synchronizedList(new ArrayList<ReceiverSubscription>());  
      
      @Override
      public void subscribe(Subscriber<? super Message> subscriber) {
        //subscriptions.add(subscriber);
        
      }
      
    };
    responseFlux = Flux.create(null);
  }

  private void receiveRegisterRequest(TpRegisterRequest req) throws InvalidProtocolBufferException {
    LOGGER.debug("Registering Message Factory of family " + req.getFamily());
    if (!this.internalMF.getFamilyName().equalsIgnoreCase(req.getFamily())
        && this.internalMF.getFamilyVersion().equalsIgnoreCase(req.getVersion())) {
      throw new InvalidProtocolBufferException("Wrong TP version received !");
    }
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
        receiveRegisterRequest(TpRegisterRequest.parseFrom(request.getContent()));
        answer = internalMF.getRegisterResponse(TpRegisterResponse.Status.OK_VALUE,
            request.getCorrelationId());
        break;
    }

    return answer;
  }

  @Override
  public void run() {
    serverSocket = context.createSocket(ZMQ.ROUTER);

    if (serverSocket.bind(mqServerAddress))
      LOGGER.debug("Bound to " + mqServerAddress);

    this.internalMGenerator = new MessageGenerator(internalMF);
    ZLoop looper = new ZLoop(context);
    PollItem pooler = new PollItem(serverSocket, ZMQ.Poller.POLLIN);
    looper.addPoller(pooler, new InternalHandler(), null);

    LOGGER.debug("Starting to pool... ");

    looper.start();

  }

  private class InternalHandler implements IZLoopHandler {

    private boolean registered = false;
    ZMsg responseMessage;
    @Override
    public int handle(ZLoop loop, PollItem item, Object arg) {
      LOGGER.debug("HANDLE :: ");
      ZMsg firstMessage = ZMsg.recvMsg(item.getSocket());
      LOGGER.debug("RECEIVE :: ");
      ZFrame identity = firstMessage.pop();
      LOGGER.debug("Received the Validator ID " + identity.toString());

      Message sawtoothMessage;
      try {
        sawtoothMessage = Message.parseFrom(firstMessage.poll().getData());
        LOGGER.debug("Received the Message " + sawtoothMessage.toString());
        Message returned = getResponse(sawtoothMessage);
        if (returned != null) {
          responseMessage = new ZMsg();
          ZFrame responseFrame = new ZFrame(returned.toByteArray());
          responseMessage.add(responseFrame);
          responseMessage.wrap(identity);
        } else {
          return 0;
        }
        if (registered) {
          LOGGER.debug("Trying to answer... ");
          if (responseMessage.send(item.getSocket())) {
            LOGGER.debug("Answer sent!");
          }
        } else {
          registered = true; 
        }
      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
        return -1;
      }
      return 0;
    }

  }

  private class ReceiverWorker implements Subscriber<Message>{

    private String messageId = "";
    private Subscription subscription;
    
    public ReceiverWorker(String messageId) {
      super();
      this.messageId = messageId;
    }


    @Override
    public void onSubscribe(Subscription subscription) {

      this.subscription = subscription;
    }

    @Override
    public void onNext(Message item) {
      if (item.getCorrelationId().equalsIgnoreCase(messageId)) {
        LOGGER.debug("Answer received!");
        subscription.cancel();
      }else {
        LOGGER.debug("not yet...");
        subscription.request(1);
      }
    }

    @Override
    public void onError(Throwable throwable) {
      LOGGER.error(throwable.getMessage());
      throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
      LOGGER.debug("Ended for messageid "+messageId);
      
    }
  }
  
  class ReceiverSubscription implements Subscription {

    @Override
    public void request(long n) {
    
      
    }

    @Override
    public void cancel() {
    
      
    }
    
    
    
  }
}
