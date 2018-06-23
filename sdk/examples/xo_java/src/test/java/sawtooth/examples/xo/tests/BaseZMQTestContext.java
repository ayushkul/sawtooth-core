package sawtooth.examples.xo.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.AbstractMessage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import sawtooth.examples.xo.XoHandler;
import sawtooth.examples.xo.tests.slim.XOTransactionProcessor;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 * <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 * <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * This class will replicate a ZeroMQ server, answering the messages as the under test
 * class needs.
 * 
 * Based on <a href="http://zguide.zeromq.org/java:asyncsrv">some</a> <a href=
 * "https://github.com/zeromq/jeromq/blob/master/src/test/java/org/zeromq/TestZMQ.java">code</a>
 * and the <a href=
 * "https://sawtooth.hyperledger.org/docs/core/releases/1.0/app_developers_guide/testing.html#message-factory-and-mock-validator">tutorial</a>;
 *
 */
public abstract class BaseZMQTestContext implements IHookable {

	public final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	/*
	 * Each thread will get a local Echo Worker, so multithreaded tests will work as
	 * usual.
	 */

	private static Vertx contextvertx;
	final List<String> handlerIds = new ArrayList<String>();
	static boolean allStarted = Boolean.FALSE;
	private final static List<Future> deploys = new ArrayList<Future>();
	Socket frontend;

	protected abstract String getMQURI();

	private final Handler<AsyncResult<String>> generateLocalHandler(String vertName,
			io.vertx.core.Future<String> verFuture) {

		return new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				LOGGER.debug("AsyncResult<>" + vertName);
				if (event.succeeded()) {
					LOGGER.debug("Deployed " + vertName + " with id " + event.result());
					handlerIds.add(0, event.result());
					LOGGER.debug("Completing " + vertName);
					verFuture.complete(event.result());
				}
				else {
					LOGGER.error("Deploying " + vertName + " failed " + event.cause().getMessage());
					event.cause().printStackTrace();
					verFuture.fail(event.cause());
				}

			}
		};
	}

	@BeforeSuite(alwaysRun = true)
	public void init() throws Exception {

		if (allStarted) {
			LOGGER.debug("Context already started, ignoring...");
			return;
		}

		Future<Vertx> clusterDeploy = Future.future();
		clusterDeploy.setHandler(rh -> {
			LOGGER.debug("CLUSTER STATUS : "+rh.succeeded());
			BaseZMQTestContext.contextvertx = rh.result();
			clusterDeploy.complete(rh.result());
			LOGGER.debug("INIT");

			Future<String> mqServerDeploy = Future.future();
			mqServerDeploy.setHandler(generateLocalHandler("ContextVerticle", mqServerDeploy));
			BaseZMQTestContext.contextvertx.deployVerticle(new ContextVerticle(), mqServerDeploy.completer());
			final XOTransactionProcessor tp = new XOTransactionProcessor();
			Future<String> transactionServerDeploy = Future.future();
			transactionServerDeploy.setHandler(generateLocalHandler("TransactionProcessor", transactionServerDeploy));
			DeploymentOptions tpOpts = new DeploymentOptions();
			tpOpts.setConfig(new JsonObject().put(XOTransactionProcessor.MQBINDADDRESSCONFIGNAME, getMQURI()));
			BaseZMQTestContext.contextvertx.deployVerticle(tp, tpOpts, transactionServerDeploy.completer());

			clusterDeploy.compose(w -> {
				LOGGER.debug("Chained handler()");
			}, mqServerDeploy);

			mqServerDeploy.compose(v -> {
				LOGGER.debug("Chained handler()");
			}, transactionServerDeploy);

			deploys.add(clusterDeploy);
			
						
			MessageConsumer<String> consumer = BaseZMQTestContext.contextvertx.eventBus().consumer("AAAAA", mh -> {
				mh.reply("FIODAPUTA");
			});
			consumer.handler(message -> {
				LOGGER.debug("I have received a message: " + message.body());
			});

		});
		
		Vertx.clusteredVertx(new VertxOptions().setMaxEventLoopExecuteTime(TimeUnit.SECONDS.toNanos(1000000000))
				.setBlockedThreadCheckInterval(5000).setInternalBlockingPoolSize(10).setWorkerPoolSize(10)
				.setMaxWorkerExecuteTime(5000).setClusterHost("127.0.0.1")
				.setEventBusOptions(new EventBusOptions().setClustered(true).setSsl(false)), clusterDeploy.completer());

		CompositeFuture dh = CompositeFuture.all(deploys);
		dh.setHandler(ar -> {
			LOGGER.debug("All.handler()");
			if (ar.succeeded()) {
				BaseZMQTestContext.allStarted = Boolean.TRUE;
			}
			else {
				ar.cause().printStackTrace();
			}
		});
		//dh.complete(dh.result());
		

	}

	@AfterSuite(alwaysRun = true)
	public void stop() throws InterruptedException {
		LOGGER.debug("STOP");
		BaseZMQTestContext.contextvertx.deploymentIDs().forEach(contextvertx::undeploy);
		LOGGER.debug("STOPPED");
	}

	/**
	 * 
	 * The "owner" of the short-circuit sockets.
	 *
	 */
	private class ContextVerticle extends AbstractVerticle {

		@Override
		public void start() {
			LOGGER.debug("ContextVerticle -- start()");

			ZContext ctx = new ZContext(1);

			// Frontend socket talks to clients over TCP
			frontend = ctx.createSocket(ZMQ.ROUTER);
			frontend.bind(getMQURI());

			// Backend socket talks to workers over inproc
			Socket backend = ctx.createSocket(ZMQ.DEALER);
			backend.bind("inproc://backend");
			ZMQ.proxy(frontend, backend, null);
			ctx.close();
			ctx.destroy();
		}

		@Override
		public void stop() {
			LOGGER.debug("stop()");
			for (String eeachId : handlerIds) {
				LOGGER.debug("Undeploying id " + eeachId);
				BaseZMQTestContext.contextvertx.undeploy(eeachId);
			}
			BaseZMQTestContext.contextvertx.close();
		}
	}

	protected boolean sendMessage(AbstractMessage message) {
		return frontend.send(message.toByteArray());
	}

	protected byte[] receiveMessage() {
		return frontend.recv();
	}

	protected Future<?> sendBusEvent(String mesgPayload) {
		LOGGER.debug("Sending event to " + getMQURI());
		Future<Message<String>> result = Future.future();

		result.setHandler(h -> {
			if (h.succeeded()) {
				LOGGER.debug("Ok -> " + h.result());
				result.complete(h.result());
			}
			else {
				LOGGER.error("Can't send message to address " + getMQURI(), h.cause());
				result.fail(h.cause());
			}

		});

		BaseZMQTestContext.contextvertx.eventBus().send(getMQURI(), mesgPayload,
				new DeliveryOptions().setSendTimeout(5000), result.completer());
		return result;

	}

	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		callBack.runTestMethod(testResult);

	}

	/**
	 * 
	 * Deploy auxiliary verticles, if needed.
	 * 
	 * @param vert - The Verticle Instance.
	 * @param name - Name, to identify and register.
	 */
	protected Future<String> deployVerticle(AbstractVerticle vert, String name) {
		Future<String> auxDeploy = Future.future();
		auxDeploy.setHandler(generateLocalHandler(name, auxDeploy));
		BaseZMQTestContext.contextvertx.deployVerticle(new ContextVerticle(), auxDeploy.completer());
		return auxDeploy;
	}

}
