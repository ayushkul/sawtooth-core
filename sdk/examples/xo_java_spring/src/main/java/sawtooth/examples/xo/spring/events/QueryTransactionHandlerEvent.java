package sawtooth.examples.xo.spring.events;

public class QueryTransactionHandlerEvent implements XOEvents {

	private final String mqAddress;
	private final String idProcessor;
	
	public QueryTransactionHandlerEvent(String mqAddress, String idProcessor) {
		super();
		this.mqAddress = mqAddress;
		this.idProcessor = idProcessor;
	}
	public String getMqAddress() {
		return mqAddress;
	}
	public String getIdProcessor() {
		return idProcessor;
	}
	
	

}
