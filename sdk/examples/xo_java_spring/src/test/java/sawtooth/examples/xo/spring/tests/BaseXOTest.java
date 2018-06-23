package sawtooth.examples.xo.spring.tests;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;


import sawtooth.examples.xo.spring.config.XOMainConfiguration;
import sawtooth.examples.xo.spring.services.ZMQProxyService;
import sawtooth.examples.xo.spring.tests.config.TestMQConfig;

@ContextConfiguration(classes = { TestMQConfig.class ,XOMainConfiguration.class})
public class BaseXOTest extends AbstractTestNGSpringContextTests {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	ZMQProxyService localMQService;
	
	@Autowired
	protected ApplicationEventPublisher publisher;
	
	@PostConstruct
	protected void prepare() {
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("prepare() "+localMQService);
		}
	}
}
