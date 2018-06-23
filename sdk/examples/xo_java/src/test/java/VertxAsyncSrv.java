
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import sawtooth.examples.xo.tests.BaseZMQTestContext;

//
//Asynchronous client-to-server (DEALER to ROUTER)
//
//While this example runs in a single process, that is just to make
//it easier to start and stop the example. Each task has its own
//context and conceptually acts as a separate process.

public class VertxAsyncSrv {
	static Vertx vertx = null;
	static final String address = "tcp://*:5558";
	static final String channel = "xotestHandler";
	static Random rand = new Random(System.nanoTime());
	private final static Logger LOGGER = LoggerFactory.getLogger(VertxAsyncSrv.class);

	// This is our server task.
	// It uses the multithreaded server model to deal requests out to a pool
	// of workers and route replies back to clients. One worker can handle
	// one request at a time but one client can talk to multiple workers at
	// once.

	public class ServerVert extends AbstractVerticle {

		@Override
		public void start() {
			ZContext ctx = new ZContext();

			// Frontend socket talks to clients over TCP
			Socket frontend = ctx.createSocket(ZMQ.ROUTER);
			frontend.bind("tcp://*:5570");

			// Backend socket talks to workers over inproc
			Socket backend = ctx.createSocket(ZMQ.DEALER);
			backend.bind("inproc://backend");

			DeploymentOptions options = new DeploymentOptions().setWorker(true).setMaxWorkerExecuteTime(5000)
					.setMultiThreaded(true);

			// Launch pool of worker threads, precise number is not critical
			for (int i = 0; i < 5; i++) {
				vertx.deployVerticle(new ServerWorkerVert(ctx), options, handler -> {
					if (handler.succeeded()) {
						LOGGER.debug("Deployed ServerWorkerVert with id " + handler.result());
					}
					else {
						handler.cause().printStackTrace();
					}

				});

			}

			ZMQ.proxy(frontend, backend, null);
			ctx.destroy();
		}
	}

	// Each worker task works on one request at a time and sends a random number
	// of replies back, with random delays between replies:

	public class ServerWorkerVert extends AbstractVerticle {

		private ZContext privctx;
		private boolean keepRunning = false;

		public ServerWorkerVert(ZContext ctx) {
			this.privctx = ctx;
		}

		@Override
		public void start() {
			keepRunning = true;
			Socket worker = privctx.createSocket(ZMQ.DEALER);
			worker.connect("inproc://backend");

			while (keepRunning && !Thread.currentThread().isInterrupted()) {
				// The DEALER socket gives us the address envelope and message
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame address = msg.pop();
				ZFrame content = msg.pop();
				assert (content != null);
				msg.destroy();

				LOGGER.debug("sending message to " + address.toString());
				vertx.eventBus().send(address.toString(), "LALA", rh -> {
					if (rh.succeeded()) {
						LOGGER.debug("Registered " + rh.toString());
					}
					else {
						LOGGER.error("FAILED TO REGISTER  " + rh.cause());
					}
				});
				// Send 0..4 replies back
				int replies = rand.nextInt(5);
				for (int reply = 0; reply < replies; reply++) {
					// Sleep for some fraction of a second
					try {
						Thread.sleep(rand.nextInt(1000) + 1);
					}
					catch (InterruptedException e) {
					}
					address.send(worker, ZFrame.REUSE + ZFrame.MORE);
					content.send(worker, ZFrame.REUSE);
				}
				address.destroy();
				content.destroy();
			}
		}

		@Override
		public void stop() throws Exception {
			LOGGER.debug("Stopping ServerWorkerVert on thread " + Thread.currentThread().getName());
			keepRunning = false;
		}
	}

	public class ClientTaskVert extends AbstractVerticle {

		private boolean keepRunning = false;

		MessageConsumer<String> commandConsumer = null;

		@Override
		public void start() {
			keepRunning = true;
			ZContext ctx = new ZContext();
			Socket client = ctx.createSocket(ZMQ.DEALER);

			// Set random Identity to make tracing easier
			String identity = String.format("%04X-%04X", rand.nextInt(), rand.nextInt());
			client.setIdentity(identity.getBytes());
			client.connect("tcp://localhost:5570");
			LOGGER.debug("registering to " + identity);
			commandConsumer = vertx.eventBus().consumer(identity, mh -> {
				LOGGER.debug("Message > " + Thread.currentThread().getName() + " - " + mh.body().toString());
				mh.reply(mh.address());

			});
			commandConsumer.completionHandler(ch -> {
				if (ch.succeeded()) {
					LOGGER.debug("Registered " + identity);
				}
				else {
					LOGGER.error("FAILED TO REGISTER  " + ch.cause());
				}
			});
			commandConsumer.exceptionHandler(eh -> {
				LOGGER.error("FAILED TO REGISTER  ");
				eh.printStackTrace();
			});

			PollItem[] items = new PollItem[] { new PollItem(client, Poller.POLLIN) };

			int requestNbr = 0;
			while (keepRunning && !Thread.currentThread().isInterrupted()) {
				// Tick once per 1/10 second, pulling in arriving messages
				ZMQ.poll(items, 100);
				if (items[0].isReadable()) {
					ZMsg msg = ZMsg.recvMsg(client);
					msg.getLast().print(identity);
					msg.destroy();
				}
				client.send(String.format("request #%d", ++requestNbr), 0);
			}
			client.close();
			ctx.destroy();
		}

		@Override
		public void stop() throws Exception {
			LOGGER.debug("Stopping ClientTaskVert on thread " + Thread.currentThread().getName());
			keepRunning = false;
		}
	}

	public static void main(String[] args) throws Exception {
		ZContext ctx = new ZContext();

		vertx = Vertx.vertx(new VertxOptions().setMaxEventLoopExecuteTime(5000).setBlockedThreadCheckInterval(5000));

		vertx.setTimer(2000, mhandler -> {
			// Run for 5 seconds then quit
			LOGGER.debug("DIE DIE DIE ");
			System.exit(0);
			vertx.close(handler ->{
				if (handler.succeeded()) {
					LOGGER.debug("Stopped Vertx " + handler.result());
					ctx.close();
					ctx.destroy();
				}
				else {
					handler.cause().printStackTrace();
				}
			});

		});

		VertxAsyncSrv mySelf = new VertxAsyncSrv();

		vertx.deployVerticle(mySelf.new ServerVert(), handler -> {
			if (handler.succeeded()) {
				LOGGER.debug("Deployed ServerVerticle with id " + handler.result());
			}
			else {
				handler.cause().printStackTrace();
			}
		});

		for (int i = 0; i < 5; i++) {
			vertx.deployVerticle(mySelf.new ClientTaskVert(), handler -> {
				if (handler.succeeded()) {
					LOGGER.debug("Deployed ClientVerticle with id " + handler.result());
				}
				else {
					handler.cause().printStackTrace();
				}
			});
		}
	}
}
