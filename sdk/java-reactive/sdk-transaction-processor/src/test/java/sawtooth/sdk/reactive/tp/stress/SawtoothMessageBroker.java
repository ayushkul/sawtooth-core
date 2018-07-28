package sawtooth.sdk.reactive.tp.stress;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZLoop;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZLoop.IZLoopHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.reactive.tp.fake.FakeValidator;
import sawtooth.sdk.reactive.tp.fake.SimpleTestTransactionHandler;
import sawtooth.sdk.reactive.tp.processor.DefaultTransactionProcessorImpl;
import sawtooth.sdk.reactive.tp.processor.TransactionHandler;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 *
 */
public class SawtoothMessageBroker {
  private static final int TIMERUNNIG = 5;
  private static String ADDRESS = "ipc://backend.ipc";
  static Random rand = new Random(System.nanoTime());
  static TransactionHandler thStress = new SimpleTestTransactionHandler();

  private final static Logger LOGGER = LoggerFactory.getLogger(SawtoothMessageBroker.class);
  static {
    // async logging
    System.setProperty(org.apache.logging.log4j.core.util.Constants.LOG4J_CONTEXT_SELECTOR,
        org.apache.logging.log4j.core.async.AsyncLoggerContextSelector.class.getName());
  }


  public static void main(String[] args) throws InterruptedException {
    ExecutorService tpe = Executors.newFixedThreadPool(4);
    DefaultTransactionProcessorImpl underTest = new DefaultTransactionProcessorImpl(ADDRESS);
    
    FakeValidator validator = new FakeValidator(thStress.getMessageFactory(), ADDRESS);
    
    tpe.execute(() ->{
      validator.run();
    });
    Thread.sleep(2000);
    
    tpe.execute(() ->{
      underTest.run();
    });
    
    underTest.addHandler(thStress);
    
    Thread.sleep(5000);
    
    tpe.shutdown();
    long totalMessages = MessageGenerator.messagesSizes.stream().count();
    LOGGER.error(
        "Messages sent : " + totalMessages + " -- aprox " + (totalMessages / TIMERUNNIG) + "/s");
    LOGGER.error(
        "Minor size : " + MessageGenerator.messagesSizes.stream().min(Integer::compare).get());
    LOGGER.error("Mean size : " + MessageGenerator.messagesSizes.stream().mapToDouble(i -> {
      return Integer.valueOf(i).doubleValue();
    }).average().getAsDouble());
    LOGGER.error(
        "Biggest size : " + MessageGenerator.messagesSizes.stream().max(Integer::compare).get());
  }

}
