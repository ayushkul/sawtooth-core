package sawtooth.sdk.reactive.tp.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZLoop;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import com.google.protobuf.InvalidProtocolBufferException;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.reactive.common.messages.MessageFactory;

public class ReactorStream extends MessagesStream implements Runnable {

  private final static Logger LOGGER = LoggerFactory.getLogger(ReactorStream.class);

  private String url;
  private String streamIdentity;
  private ZMQ.Socket socket;
  private SubmissionPublisher<Message> msgPublisher = new SubmissionPublisher<>();
  private ZContext context;
  private MessageFactory mesgFact;

  public ReactorStream(String address) {
    this.url = address;
    streamIdentity =
        (this.getClass().getSimpleName() + ":" + address + ":" + UUID.randomUUID().toString());
    LOGGER.debug("Creating ");

  }

  @Override
  public void run() {
    this.context = new ZContext();
    socket = this.context.createSocket(ZMQ.DEALER);
    socket.monitor("inproc://monitor.s", ZMQ.EVENT_DISCONNECTED);
    final ZMQ.Socket monitor = this.context.createSocket(ZMQ.PAIR);
    monitor.connect("inproc://monitor.s");
    ZLoop eventLoop = new ZLoop(this.context);
    socket.setIdentity(getStreamIdentity().getBytes());
    socket.connect(url);
    ZMQ.PollItem pollItem = new ZMQ.PollItem(socket, ZMQ.Poller.POLLIN);
    eventLoop.addPoller(pollItem, new Receiver(msgPublisher), new Object());
    eventLoop.start();
  }

  private class Receiver implements ZLoop.IZLoopHandler {

    private SubmissionPublisher<Message> receiverPublisher;

    public Receiver(SubmissionPublisher<Message> internalMesgPub) {
      this.receiverPublisher = internalMesgPub;
    }

    @Override
    public int handle(ZLoop loop, ZMQ.PollItem item, Object arg) {
      ZMsg msg = ZMsg.recvMsg(item.getSocket());
      Iterator<ZFrame> multiPartMessage = msg.iterator();

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      multiPartMessage.forEachRemaining((partial) -> {
        try {
          byteArrayOutputStream.write(partial.getData());
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });

      try {
        Message message = Message.parseFrom(byteArrayOutputStream.toByteArray());
        this.receiverPublisher.submit(message);
      } catch (InvalidProtocolBufferException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      return 0;
    }
  }

  @Override
  public Future<Message> send(Message payload) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void sendBack(String correlationId, Message payload) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setMessageFactory(MessageFactory mFactory) {
    this.mesgFact = mFactory;

  }

  @Override
  public Future<Message> receive() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Future<Message> receive(long timeout) throws TimeoutException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  public String getStreamIdentity() {
    return streamIdentity;
  }

}
