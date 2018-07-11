package sawtooth.sdk.rest.client.tests;

import javax.enterprise.context.control.ActivateRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;

@Listeners(TestsListener.class)
@ActivateRequestContext
public class BaseTest {

	protected final Logger LOGGER = LoggerFactory.getLogger(getClass().getClass());

}
