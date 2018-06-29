package sawtooth.sdk.reactive.tests;

import static org.testng.Assert.assertNotNull;

import javax.enterprise.inject.spi.CDI;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import sawtooth.sdk.reactive.rest.api.DefaultApi;


@Test
public class TestRESTConnectivity extends BaseTest{

	
	DefaultApi	basicClient;
	
	@BeforeClass
	public void getCDIBeans() throws ClassNotFoundException {
		basicClient = (DefaultApi) CDI.current().select(DefaultApi.class).get();
	}
	
	
	@Test
	public void testClientInjection() {
		assertNotNull(basicClient);
	}
	
}
