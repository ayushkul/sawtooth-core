package sawtooth.sdk.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZLoop;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message;

public class ReactorStream implements Runnable {
	private String url;
	private ZMQ.Socket socket;
	private ConcurrentHashMap<String, Future> futures;
	private LinkedBlockingQueue<Message> receiveQueue;
	private ZContext context;

	public ReactorStream(String address) {
		this.receiveQueue = new LinkedBlockingQueue<Message>();
		this.url = address;

	}

	@Override
	public void run() {
		this.context = new ZContext();
		socket = this.context.createSocket(ZMQ.DEALER);
		socket.monitor("inproc://monitor.s", ZMQ.EVENT_DISCONNECTED);
		final ZMQ.Socket monitor = this.context.createSocket(ZMQ.PAIR);
		monitor.connect("inproc://monitor.s");
		ZLoop eventLoop = new ZLoop(this.context);
		ZMQ.PollItem pollItem = new ZMQ.PollItem(socket, ZMQ.Poller.POLLIN);
		eventLoop.addPoller(pollItem, new Receiver(futures, receiveQueue), new Object());
		eventLoop.start();
	}

	private class Receiver implements ZLoop.IZLoopHandler {
		private ConcurrentHashMap<String, Future> futures;
		private LinkedBlockingQueue<Message> receiveQueue;

		Receiver(ConcurrentHashMap<String, Future> futures, LinkedBlockingQueue<Message> receiveQueue) {
			this.futures = futures;
			this.receiveQueue = receiveQueue;
		}

		@Override
		public int handle(ZLoop loop, ZMQ.PollItem item, Object arg) {
			ZMsg msg = ZMsg.recvMsg(item.getSocket());
			Iterator<ZFrame> multiPartMessage = msg.iterator();

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			while (multiPartMessage.hasNext()) {
				ZFrame frame = multiPartMessage.next();
				try {
					byteArrayOutputStream.write(frame.getData());
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			try {
				Message message = Message.parseFrom(byteArrayOutputStream.toByteArray());
				if (this.futures.containsKey(message.getCorrelationId())) {
					Future future = this.futures.get(message.getCorrelationId());
					future.setResult(message.getContent());
					this.futures.put(message.getCorrelationId(), future);
				}
				else {
					this.receiveQueue.put(message);
				}
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			catch (InvalidProtocolBufferException ipe) {
				ipe.printStackTrace();
			}
			catch (ValidatorConnectionError vce) {
				vce.printStackTrace();
			}

			return 0;
		}
	}

}
