package sawtooth.examples.xo.spring.demo.tasks;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import sawtooth.examples.xo.spring.demo.SpringAsyncMQProxy;

import org.zeromq.ZMQ.Socket;

@Component
public class EchoWorker extends BasicControllableTask {
	private static final Random rand = new Random(System.nanoTime());
	private final static Logger LOGGER = LoggerFactory.getLogger(EchoWorker.class);

	
	private ZContext privctx;

	public void setZContext(ZContext externalctx) {
		this.privctx = externalctx;
	}
	
	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
	
	@Override
	public void run() {
		keepRunning.set(true);
		Socket worker = privctx.createSocket(ZMQ.DEALER);
		worker.setLinger(0);
		worker.connect(SpringAsyncMQProxy.backend_address);
		while (keepRunning.get()) {
			// The DEALER socket gives us the address envelope and message
			ZMsg msg = ZMsg.recvMsg(worker);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Receiving message..."+msg);
			if (! keepRunning.get() || msg == null) {
				break;
			}
			ZFrame address = msg.pop();
			ZFrame content = msg.pop();
			assert (content != null);
			msg.destroy();

			// Send 0..4 replies back
			int replies = rand.nextInt(5);
			for (int reply = 0; reply < replies; reply++) {
				if (! keepRunning.get()) {
					break;
				}
				address.send(worker, ZFrame.REUSE + ZFrame.MORE);
				content.send(worker, ZFrame.REUSE);
			}
			address.destroy();
			content.destroy();
		}
		worker.unbind(SpringAsyncMQProxy.backend_address);
		worker.close();
	}

}
