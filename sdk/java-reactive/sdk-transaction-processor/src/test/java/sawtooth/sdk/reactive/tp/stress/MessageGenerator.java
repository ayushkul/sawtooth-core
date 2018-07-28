package sawtooth.sdk.reactive.tp.stress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import com.google.protobuf.InvalidProtocolBufferException;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.messaging.MessageFactory;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 *         The class will generate messages, sendo to the TP, and wait for a response.
 *
 */
public class MessageGenerator {


  private static final int fileSize;
  static Random rand = new Random(System.nanoTime());
  private final static Logger LOGGER = LoggerFactory.getLogger(MessageGenerator.class);
  private static ByteBuffer sourceData;
  public static ArrayList<Integer> messagesSizes = new ArrayList<>();
  public static MessageFactory mf;
  private static ByteBuffer myDataSource;
  private final static Flux<Message> messagesFlux = Flux.create(messageSink -> {
    Message pingMessage = null;
    try {
      pingMessage = mf.getPingRequest(
          null);/*
                 * ByteBuffer.wrap(getRandomData(MessageGenerator.minLength +
                 * rand.nextInt(MessageGenerator.maxLength - MessageGenerator.minLength))));
                 */
    } catch (InvalidProtocolBufferException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    messageSink.next(pingMessage);
  });


  static {
    File randomDataFile =
        new File(MessageGenerator.class.getClassLoader().getResource("seed.bin").getFile());
    fileSize = Long.valueOf(randomDataFile.length()).intValue();
    try {
      InputStream input = new BufferedInputStream(new FileInputStream(randomDataFile));
      sourceData = ByteBuffer.allocate(fileSize);
      sourceData.put(input.readAllBytes());
      input.close();
      myDataSource = sourceData.asReadOnlyBuffer();

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


  }

  public MessageGenerator(MessageFactory mf) {
    LOGGER.debug("MessageGenerator() File Size : " + fileSize);
    MessageGenerator.mf = mf;
  }

  public final Flux<Message> getMessagesflux() {
    return messagesFlux.publish();
  }

  private static byte[] getRandomData(int lenght) {
    byte[] result = new byte[lenght];
    if (LOGGER.isDebugEnabled()) {
      int offset = rand.nextInt(fileSize - lenght);
      myDataSource.position(offset);
      LOGGER.debug(" Reading " + lenght + " from position " + offset + " on size " + fileSize);
      myDataSource.get(result);
    } else {
      myDataSource.position(rand.nextInt(fileSize - lenght));
      myDataSource.get(result);
    }
    messagesSizes.add(result.length);
    return result;
  }


}
