package sawtooth.examples.xo.spring.demo.config;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import sawtooth.examples.xo.spring.demo.SpringAsyncMQProxy;
import sawtooth.examples.xo.spring.events.MessageSentEvent;

@Component
public class OperationsListener {
	public static final AtomicLong MESSAGECOUNTER = new AtomicLong(0L);

	@Async
	@EventListener(classes = { MessageSentEvent.class })
	public void addOne(MessageSentEvent event) {
		MESSAGECOUNTER.incrementAndGet();
	}

}
