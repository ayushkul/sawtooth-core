package sawtooth.examples.xo.spring.demo.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CoreConfig extends AsyncConfigurerSupport {

	private final static Logger LOGGER = LoggerFactory.getLogger(CoreConfig.class);

	public static final String MQADDRESSCONFIGKEY = "mqaddress";
	
	@Bean
	ExecutorService workersExecutor() {
		ExecutorService workersExecutor = Executors.newFixedThreadPool(10);
		return workersExecutor;
	}
	
	@Bean
	ThreadPoolTaskExecutor messageProcesor() {
		ThreadPoolTaskExecutor messageProcesor =  new ThreadPoolTaskExecutor();
		messageProcesor.setCorePoolSize(5);
		messageProcesor.initialize();
		messageProcesor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		messageProcesor.setWaitForTasksToCompleteOnShutdown(true);
		messageProcesor.setAwaitTerminationSeconds(10);
		return messageProcesor;
	}
	
	@Bean
	@Override
	public ConcurrentTaskExecutor getAsyncExecutor() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(workersExecutor());
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Thread Pool Task Executor initialized.");
		return executor;
	}


	@Bean(name = "applicationEventMulticaster")
	public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
		SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
		eventMulticaster.setTaskExecutor(messageProcesor());
		return eventMulticaster;
	}
	

}
