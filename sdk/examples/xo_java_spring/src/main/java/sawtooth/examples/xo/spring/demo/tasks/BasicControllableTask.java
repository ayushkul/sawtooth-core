package sawtooth.examples.xo.spring.demo.tasks;

import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;

public abstract class BasicControllableTask implements Runnable, DisposableBean {

	ThreadLocal<Boolean> keepRunning = new ThreadLocal<Boolean>();
	ThreadLocal<Integer> messageCounter = new ThreadLocal<Integer>();
	
	abstract protected Logger getLogger();

	public BasicControllableTask() {
		keepRunning.set(false);
		messageCounter.set(0);
	}

	
	public int getMessageCount() {
		return messageCounter.get();
	}
	
	public void flagToStop() {
		keepRunning.set(false);
	}
	
	@Override
	public void destroy() throws Exception {
		if (getLogger().isInfoEnabled())
			getLogger().info("Destroying  "+this.getClass().getSimpleName() +" instance " + this.toString());
			
	}
	
}
