package sawtooth.examples.xo.spring.services;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import sawtooth.examples.xo.spring.config.XOMainConfiguration;

@Component(value = ZMQProxyService.ZMQPROXYSERVICEBEANNAME)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ZMQProxyService {
	private final static Logger LOGGER = LoggerFactory.getLogger(ZMQProxyService.class);
	static boolean allStarted = Boolean.FALSE;
	Socket frontend;
	Socket backend;
	private ThreadGroup mqproxy = new ThreadGroup("MqProxyThread");
	public static final String ZMQPROXYSERVICEBEANNAME = "zmqproxyservice";

	public static final String INTERNALZMSOCKETNAME = "inproc://backend";

	private static Thread serverExecutor;

	@Autowired(required = true)
	@Qualifier(value = "testconfig")
	private Properties configProperties;

	public boolean isStarted() {
		return allStarted;
	}

	@PostConstruct
	public void startMQServer() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("startMQServer()...");
		}

		serverExecutor = new Thread(mqproxy, new Runnable() {
			@Override
			public void run() {
				ZContext ctx = new ZContext(2);
				frontend = ctx.createSocket(ZMQ.ROUTER);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Binding on "
							+ configProperties.getProperty(XOMainConfiguration.MQADDRESSCONFIGKEY, "tcp://*:4444"));
				}
				frontend.bind("tcp://*:4567");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("BOUND on "
							+ configProperties.getProperty(XOMainConfiguration.MQADDRESSCONFIGKEY, "tcp://*:4444"));
				}
				backend = ctx.createSocket(ZMQ.DEALER);
				backend.bind(INTERNALZMSOCKETNAME);
				ZMQ.proxy(backend,frontend , null);
				
				ctx.close();
				ctx.destroy();
			}

		});
		serverExecutor.setDaemon(true);
		serverExecutor.start();
	}

	@PreDestroy
	public void stopServer() throws InterruptedException {
		serverExecutor.join();
	}

}
