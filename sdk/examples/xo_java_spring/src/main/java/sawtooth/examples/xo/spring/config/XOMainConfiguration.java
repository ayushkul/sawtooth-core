package sawtooth.examples.xo.spring.config;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import sawtooth.examples.xo.core.XOTransactionProcessor;
import sawtooth.examples.xo.spring.services.ZMQProxyService;

@Configuration
@EnableAsync
@ComponentScan(basePackageClasses = { XOTransactionProcessor.class, ZMQProxyService.class })
public class XOMainConfiguration extends AsyncConfigurerSupport {

	private final static Logger LOGGER = LoggerFactory.getLogger(XOMainConfiguration.class);
	
	public static final String MQADDRESSCONFIGKEY = "mqaddress";

	@Bean
	public static PropertyPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertyPlaceholderConfigurer();
	}

	@Bean
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("xoconfig-");
		executor.initialize();
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Thread Pool Task Executor initialized.");
		return executor;
	}

	@Bean(name = "applicationEventMulticaster")
	public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
		SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

		eventMulticaster.setTaskExecutor(getAsyncExecutor());
		return eventMulticaster;
	}

	@Bean
	public ExecutorService getDefaultExecutorService() {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 5, 5, TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
        return pool;
		
	}
	
	@Bean
	public Properties getConfigProperties() {
		Properties currentConfig = new Properties();
		
		currentConfig.setProperty(XOMainConfiguration.MQADDRESSCONFIGKEY, "tcp://*:4567");
		
		return currentConfig;
	}
	
	
}
