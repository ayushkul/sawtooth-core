package sawtooth.examples.xo.spring.demo.tasks;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import sawtooth.examples.xo.spring.demo.SpringAsyncMQProxy;
import sawtooth.examples.xo.spring.events.MessageSentEvent;

import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

@Component
public class ReceiveWorker extends BasicControllableTask {

	static final Random rand = new Random(System.nanoTime());
	private final static Logger LOGGER = LoggerFactory.getLogger(ReceiveWorker.class);
	ThreadLocal<String> identity = new ThreadLocal<String>();
	
	@Autowired
	protected ApplicationEventPublisher publisher;
	
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
	
	@Override
	public void run() {
		LOGGER.info(Thread.currentThread().getName()+ " : run() ");
		messageCounter.set(0);
		keepRunning.set(true);
		ZContext ctx = new ZContext();
		ctx.setLinger(0);
		ctx.setMain(false);
		Socket client = ctx.createSocket(ZMQ.DEALER);
		client.setLinger(0);

		// Set random Identity to make tracing easier
		identity.set(String.format("%04X-%04X", rand.nextInt(), rand.nextInt()));
		if (LOGGER.isInfoEnabled())
			LOGGER.info("Identity of Thread "+Thread.currentThread().getName()+ " : " + identity.get());
		client.setIdentity(identity.get().getBytes());
		client.connect(SpringAsyncMQProxy.frontend_address);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("registering as " + identity);

		PollItem[] items = new PollItem[] { new PollItem(client, Poller.POLLIN) };

		while (keepRunning.get()) {
			// Tick once per 1/100 second, pulling in arriving messages
			ZMQ.poll(items, 10);
			if (items[0].isReadable()) {
				ZMsg msg = ZMsg.recvMsg(client);
				msg.getLast().print(identity.get());
				msg.destroy();
			}
			messageCounter.set(messageCounter.get() + 1);
			publisher.publishEvent(new MessageSentEvent());
			client.send(String.format("request #%d", messageCounter.get()), 0);
		}
		client.unbind(SpringAsyncMQProxy.frontend_address);
		client.close();
		ctx.close();
		ctx.destroy();
	}

}
