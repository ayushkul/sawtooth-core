package sawtooth.sdk.reactive.rest.writer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.GeneratedMessageV3;
import sawtooth.sdk.protobuf.Message;


/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 *
 */

@Produces({"application/octet-stream"})
@Singleton
public final class MessageBodyTransformer implements javax.ws.rs.ext.MessageBodyWriter<Object>,
    javax.ws.rs.ext.MessageBodyReader<Object> {

  private final static Logger LOGGER = LoggerFactory.getLogger(MessageBodyTransformer.class);

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("Testing parseability for class of type " + type.getName() + " and generic type "
          + genericType);
    return type.getSuperclass().equals(GeneratedMessageV3.class);
  }

  @Override
  public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
      throws IOException, WebApplicationException {

    if (LOGGER.isDebugEnabled())
      LOGGER.debug("Writing class of type " + type.getName() + " and generic type " + genericType);

    if (LOGGER.isTraceEnabled())
      LOGGER
          .trace("Writing Object " + t.toString() + "of type " + type.getName() + ", generic type "
              + genericType + ", annotations " + Arrays.deepToString(annotations) + ", media type "
              + mediaType + ", HTTP Headers  " + httpHeaders != null ? httpHeaders.toString()
                  : "[]");

    ((Message) t).writeTo(entityStream);

  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType) {
    return isWriteable(type, genericType, annotations, mediaType);
  }

  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
      throws IOException, WebApplicationException {

    if (LOGGER.isDebugEnabled())
      LOGGER.debug("Writing class of type " + type.getName() + " and generic type " + genericType);

    if (LOGGER.isTraceEnabled())
      LOGGER.trace("Reading class of type " + type.getName() + ", generic type " + genericType
          + ", annotations " + Arrays.deepToString(annotations) + ", media type " + mediaType
          + ", HTTP Headers  " + httpHeaders != null ? httpHeaders.toString() : "[]");
    return Message.parseDelimitedFrom(entityStream);
  }

}
