package sawtooth.sdk.reactive.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sawtooth.sdk.reactive.rest.writer.MessageBodyWriter;

public class RESTClientConfig {

  int readTimeout = 5000;
  int connectTimeout = 5000;
  int buffersize = 4096;

  public final static String DEFAULTCLIENTBEANNAME = "DefaultClientBean";
  private static final Properties configProperties = new Properties();
  private static String validatorURL = "";
  private static String apiRESTURL = "";
  private final static Logger LOGGER = LoggerFactory.getLogger(RESTClientConfig.class);

  static {
    try (InputStream in =
        RESTClientConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
      configProperties.load(in);


      validatorURL = configProperties.getProperty("sawtooth.validator.url");
      if (validatorURL == null || validatorURL.isEmpty() || validatorURL.startsWith("${")) {
        validatorURL = System.getenv("sw.validator.url");
      }
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("Validator URL setting : " + validatorURL);
      apiRESTURL = configProperties.getProperty("sawtooth.rest.url");
      if (apiRESTURL == null || apiRESTURL.isEmpty() || apiRESTURL.startsWith("${")) {
        apiRESTURL = System.getenv("sw.rest.url");
      }
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("REST URL setting : " + apiRESTURL);
    } catch (IOException e) {
      LOGGER.error("Configuration file not found, problems ahead : " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static final String getValidatorURL() {
    return validatorURL;
  }

  public static final String getRESTURL() {
    return apiRESTURL;
  }

  public ConnectorProvider getBasicConectorProvider() {
    HttpUrlConnectorProvider providerBean = new HttpUrlConnectorProvider();
    providerBean.chunkSize(buffersize);
    return providerBean;
  }

  @Produces
  @ApplicationScoped
  @Named(value = RESTClientConfig.DEFAULTCLIENTBEANNAME)
  public final Client getBasicClient() {

    ClientConfig baseConfig = new ClientConfig();

    baseConfig.register(JacksonFeature.class);
    baseConfig.register(JacksonJaxbJsonProvider.class);
    baseConfig.register(MessageBodyWriter.class);

    baseConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);
    baseConfig.property(ClientProperties.USE_ENCODING, StandardCharsets.UTF_8.name());
    baseConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);

    baseConfig.connectorProvider(getBasicConectorProvider());

    final Client clientBean = ClientBuilder.newClient(baseConfig);

    return clientBean;
  }

}
