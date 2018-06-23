package sawtooth.examples.xo.spring.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.zeromq.ZContext;

import sawtooth.examples.xo.spring.demo.config.CoreConfig;
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
public class SpringAsyncMQProxyCallers{

	public static final String frontend_address = "tcp://*:5558";
	public static final String backend_address = "inproc://backend";

	public static final AtomicLong MESSAGECOUNTER = new AtomicLong(0L);
	
	private final static Logger LOGGER = LoggerFactory.getLogger(SpringAsyncMQProxyCallers.class);
	
	private int time = 5;
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(SpringAsyncMQProxyCallers.class, args);
	}

	@Bean
	public ApplicationRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			ThreadPoolTaskExecutor workerExecutor = (ThreadPoolTaskExecutor)ctx.getBean(Executor.class);
			MQProxyRunner mainRunner = ctx.getBean(MQProxyRunner.class);
			Thread mainThread =  workerExecutor.createThread(mainRunner);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Starting the Proxy Runner...");
			mainThread.start();
			List<Thread> runners = new ArrayList<>();
			IntStream.range(0, 5).map( i-> {
				ReceiveWorker rWorker = ctx.getBean(ReceiveWorker.class);
				Thread subThread =  workerExecutor.createThread(rWorker);
				runners.add(subThread);
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Starting the receiver worker #"+i);
				subThread.start();
				return i;
			});
			
			Thread.sleep(TimeUnit.SECONDS.toMillis(time));
			runners.stream().map( rn -> {
				rn.interrupt();
				return null;
			});
			mainThread.interrupt();
			
			ZContext theZMQContext = ctx.getBean(ZContext.class);
			theZMQContext.close();
			theZMQContext.destroy();
			LOGGER.error("We got a total of "+MESSAGECOUNTER.get()+" messages in "+time+" seconds ("+(MESSAGECOUNTER.get()/time)+" msgs/s)");
			SpringApplication.exit(ctx, () -> 0);
		};
	}

}
