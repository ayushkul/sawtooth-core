package sawtooth.examples.xo.spring.tests.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sawtooth.examples.xo.spring.config.XOMainConfiguration;

@Configuration
public class TestMQConfig {

	@Bean(name="testconfig")
	public Properties getConfigProperties() {
		Properties currentConfig = new Properties();
		
		currentConfig.setProperty(XOMainConfiguration.MQADDRESSCONFIGKEY, "tcp://*:4567");
		
		return currentConfig;
	}

}
