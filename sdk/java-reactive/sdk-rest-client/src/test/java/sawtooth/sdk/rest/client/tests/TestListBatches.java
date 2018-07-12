package sawtooth.sdk.rest.client.tests;

import static org.testng.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;

import javax.enterprise.inject.spi.CDI;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import sawtooth.sdk.reactive.rest.model.RESTBatchListing;
import sawtooth.sdk.reactive.rest.ops.RESTBatchOps;

@Test
public class TestListBatches extends BaseTest{

	RESTBatchOps underTest;
	
	@BeforeClass
	public void getCDIBeans() throws ClassNotFoundException {
		underTest = (RESTBatchOps) CDI.current().select(RESTBatchOps.class).get();
	}
	
	
	@Test
	public void testState() {
		assertNotNull(underTest);
	}
	
	@Test(dependsOnMethods= {"testState"})
	public void testListAll() throws InterruptedException, ExecutionException {
		RESTBatchListing result = underTest.listBatches(null, null, 100, "false").get();
		assertNotNull(result);
	}

}
