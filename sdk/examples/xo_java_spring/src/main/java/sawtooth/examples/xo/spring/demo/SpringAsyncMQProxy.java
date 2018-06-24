package sawtooth.examples.xo.spring.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import com.google.common.eventbus.EventBus;

import sawtooth.examples.xo.spring.demo.config.CoreConfig;
import sawtooth.examples.xo.spring.demo.config.OperationsListener;
import sawtooth.examples.xo.spring.demo.tasks.EchoWorker;
import sawtooth.examples.xo.spring.demo.tasks.MQProxyRunner;
import sawtooth.examples.xo.spring.demo.tasks.ReceiveWorker;

//
//Asynchronous client-to-server (DEALER to ROUTER)
//
//While this example runs in a single process, that is just to make
//it easier to start and stop the example. Each task has its own
//context and conceptually acts as a separate process.

@SpringBootApplication(scanBasePackageClasses = { CoreConfig.class, EchoWorker.class })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class SpringAsyncMQProxy {

	public static final String frontend_address = "tcp://*:5558";
	public static final String backend_address = "inproc://backend";
	private final static Logger LOGGER = LoggerFactory.getLogger(SpringAsyncMQProxy.class);
	private int time = 5;

	static {
		// async logging
		System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
	}

	public static void main(String[] args) throws Exception {
		SpringApplicationBuilder spBuilder = new SpringApplicationBuilder().headless(true)
				.properties("logging.config=classpath:log4j2.properties").main(SpringAsyncMQProxy.class)
				.sources(SpringAsyncMQProxy.class);

		spBuilder.build().run(args);

	}

	@Bean
	public ApplicationRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			ConcurrentTaskExecutor workerExecutor = ctx.getBean(ConcurrentTaskExecutor.class);

			MQProxyRunner mainRunner = ctx.getBean(MQProxyRunner.class);
			workerExecutor.execute(mainRunner);

			List<ReceiveWorker> allReceiveWorkers = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				ReceiveWorker rWorker = ctx.getBean(ReceiveWorker.class);
				allReceiveWorkers.add(rWorker);
				workerExecutor.execute(rWorker);
			}

			Thread.sleep(TimeUnit.SECONDS.toMillis(time));
			/*
			 * for (ReceiveWorker eachWorker : allReceiveWorkers) {
			 * eachWorker.flagToStop(); //eachWorker.destroy(); } for (EchoWorker
			 * eachWorker : mainRunner.allEchoWorkers) { eachWorker.flagToStop();
			 * //eachWorker.destroy(); } mainRunner.flagToStop();
			 */// mainRunner.destroy();

			LOGGER.info("We got a total of " + OperationsListener.MESSAGECOUNTER.get() + " messages in " + time + " seconds ("
					+ (OperationsListener.MESSAGECOUNTER.get() / time) + " msgs/s)");
			((ConfigurableApplicationContext) ctx).stop();
			Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			SpringApplication.exit(ctx, () -> 0);
		};
	}

}
