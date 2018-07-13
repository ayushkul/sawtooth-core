package sawtooth.sdk.reactive.rest.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;

@Produces({ "application/octet-stream" })
@Provider
@Singleton
public final class MessageBodyWriter implements javax.ws.rs.ext.MessageBodyWriter<Object> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.getSuperclass().equals(GeneratedMessageV3.class);
	}

	@Override
	public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		((Message)t).writeTo(entityStream);
		
	}

}
