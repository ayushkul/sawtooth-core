package sawtooth.sdk.processor;

import java.util.List;
import java.util.Map;

import sawtooth.sdk.events.ControlEvent;

public interface TransactionProcessorInterface extends Runnable {

	/**
	 * add a handler that will be run from within the run method.
	 * @param handler implements that TransactionHandler interface
	 */
	public void addHandler(TransactionHandler handler);

	public void run();
	public void shutdown();
	
	public void registerNewHandler(ControlEvent event);
	public String getTransactionHandlerId();
	

}
