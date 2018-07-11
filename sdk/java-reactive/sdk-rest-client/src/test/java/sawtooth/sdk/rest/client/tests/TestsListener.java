package sawtooth.sdk.rest.client.tests;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import sawtooth.sdk.reactive.config.RESTClientConfig;
import sawtooth.sdk.reactive.rest.ops.RESTStateOps;

public class TestsListener implements ISuiteListener, ITestListener {

	private final static Logger LOGGER = LoggerFactory.getLogger(TestsListener.class);
	private boolean started = false;
	private SeContainer container;
	static final SeContainerInitializer initializer;
	static {
		initializer = SeContainerInitializer.newInstance().disableDiscovery();
	}

	private void startCDI() {
		LOGGER.info("Starting CDI ...");
		container = initializer.addPackages(true, RESTClientConfig.class, RESTStateOps.class).initialize();
		container.select(RESTClientConfig.class);
		started = container.isRunning();
		LOGGER.info("Started : "+started);
	}

	private void stopCDI() {
		LOGGER.info("Stopping CDI ...");
		if (started) {
			container.close();
		}
		started = container.isRunning();
		LOGGER.info("Stopped : "+! started);
	}

	@Override
	public void onStart(ISuite suite) {
		if (!started) {
			startCDI();
		}
	}

	@Override
	public void onFinish(ISuite suite) {
		if (started) {
			//stopCDI();
		}
	}

	@Override
	public void onTestStart(ITestResult result) {
		if (!started) {
			startCDI();
		}

	}

	@Override
	public void onTestSuccess(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestFailure(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestSkipped(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart(ITestContext context) {
		if (!started) {
			startCDI();
		}

	}

	@Override
	public void onFinish(ITestContext context) {
		if (started) {
			//stopCDI();
		}

	}

}
