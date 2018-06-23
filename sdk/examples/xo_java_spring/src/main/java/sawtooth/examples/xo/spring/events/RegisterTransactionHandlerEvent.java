package sawtooth.examples.xo.spring.events;

public class RegisterTransactionHandlerEvent implements XOEvents {

	private final String idProcessor;
	private final String handlerClassName;
	private final String handlerBeanName;

	public RegisterTransactionHandlerEvent(String idProcessor, String handlerClassName, String handlerBeanName) {
		super();
		this.idProcessor = idProcessor;
		this.handlerClassName = handlerClassName;
		this.handlerBeanName = handlerBeanName;
	}

	public String getIdProcessor() {
		return idProcessor;
	}

	public String getHandlerClassName() {
		return handlerClassName;
	}

	public String getHandlerBeanName() {
		return handlerBeanName;
	}

}
