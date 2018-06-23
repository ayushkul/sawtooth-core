package sawtooth.examples.xo.spring.demo.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Component;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import sawtooth.examples.xo.spring.demo.SpringAsyncMQProxy;

// This is our server task.
// It uses the multithreaded server model to deal requests out to a pool
// of workers and route replies back to clients. One worker can handle
// one request at a time but one client can talk to multiple workers at
// once.

@Component
public class MQProxyRunner extends BasicControllableTask {

	ZContext ctx = new ZContext();

	@Autowired
	ConcurrentTaskExecutor workerExecutor;

	@Autowired
	ApplicationContext appctx;

	private final static Logger LOGGER = LoggerFactory.getLogger(MQProxyRunner.class);

	protected Socket frontend, backend;
	public  List<EchoWorker> allEchoWorkers = new ArrayList<>();

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
	
	@Override
	public void run() {

		// Frontend socket talks to clients over TCP
		frontend = ctx.createSocket(ZMQ.ROUTER);
		frontend.setLinger(0);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Binding on " + SpringAsyncMQProxy.frontend_address);
		frontend.bind(SpringAsyncMQProxy.frontend_address);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Bound on " + SpringAsyncMQProxy.frontend_address);

		// Backend socket talks to workers over inproc
		backend = ctx.createSocket(ZMQ.DEALER);
		backend.setLinger(0);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Binding on " + SpringAsyncMQProxy.backend_address);
		backend.bind(SpringAsyncMQProxy.backend_address);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Bound on " + SpringAsyncMQProxy.frontend_address);

		for (int i = 0; i < 5; i++) {
			EchoWorker toRun = appctx.getBean(EchoWorker.class);
			toRun.setZContext(ctx);
			allEchoWorkers.add(toRun);
			workerExecutor.execute(toRun);
		}

		ZMQ.proxy(frontend, backend, null);
		allEchoWorkers.stream().map( aew -> {
			aew.flagToStop();
			aew.setZContext(null);
			return null;
		});
		frontend.close();
		backend.close();
	}

	@Override
	public void destroy() throws Exception {
		ctx.close();
		ctx.destroy();
		super.destroy();

	}
}
