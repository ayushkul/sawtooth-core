package sawtooth.sdk.reactive.tp.processor;

public interface TransactionProcessor extends Runnable {

	/**
	 * add a handler that will be run from within the run method.
	 * @param handler implements that TransactionHandler interface
	 */
	public void addHandler(TransactionHandler handler);

	public void run();
	public void shutdown();
	
	public String getTransactionProcessorId();
	

}
