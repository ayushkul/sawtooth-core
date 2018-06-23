package sawtooth.examples.xo.spring.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import sawtooth.examples.xo.XoHandler;
import sawtooth.examples.xo.core.XOTransactionProcessor;
import sawtooth.examples.xo.spring.events.RegisterTransactionHandlerEvent;

@Component
public class XOMessageBrokerService implements ApplicationContextAware {

	private final static Logger LOGGER = LoggerFactory.getLogger(XOMessageBrokerService.class);
	private final static List<XOTransactionProcessor> allTPs = new ArrayList<>();

	@Autowired
	ExecutorService executorService;

	private ApplicationContext currAppContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("initializing... ");
		}
		this.currAppContext = applicationContext;
	}

	@PostConstruct
	public void starTransactionProcessor() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting TransactionProcessor ...");
		}
		AutowireCapableBeanFactory myFactory = currAppContext.getAutowireCapableBeanFactory();
		XOTransactionProcessor newRunner = currAppContext.getBean(XOTransactionProcessor.class);
		myFactory.autowireBean(newRunner);
		myFactory.initializeBean(newRunner, "MOCKTRANSACTIONPROCESSOR");
		// myFactory.applyBeanPostProcessorsAfterInitialization(newRunner,
		// data.getIdProcessor());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Transaction Processor successfully created.");
		}
		
	}

}
