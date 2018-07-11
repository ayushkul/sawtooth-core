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
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sawtooth.sdk.reactive.common.config.SawtoothConfiguration;
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
		try (InputStream in = RESTClientConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
			configProperties.load(in);
			validatorURL = configProperties.getProperty("sawtooth.validator.url");
			if (validatorURL == null || validatorURL.isEmpty() || validatorURL.startsWith("${")) {
				validatorURL = System.getProperty("sawtooth.validator.url");
			}
			apiRESTURL = configProperties.getProperty("sawtooth.rest.url");
			if (apiRESTURL == null || apiRESTURL.isEmpty() || apiRESTURL.startsWith("${")) {
				apiRESTURL = System.getProperty("sawtooth.rest.url");
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getValidatorURL() {
		return "http://192.168.1.101:4004";
	}

	public static String getRESTURL() {
		return "http://192.168.1.101:8080";
	}

	public ConnectorProvider getBasicConectorProvider() {
		HttpUrlConnectorProvider providerBean = new HttpUrlConnectorProvider();
		providerBean.chunkSize(buffersize);
		return providerBean;
	}

	@Produces
	@ApplicationScoped
	@Named(value = RESTClientConfig.DEFAULTCLIENTBEANNAME)
	public Client getBasicClient() {

		ClientConfig baseConfig = new ClientConfig();

		baseConfig.register(JacksonFeature.class);
		baseConfig.register(JacksonJaxbJsonProvider.class);
		baseConfig.register(MoxyJsonFeature.class);
		baseConfig.register(MessageBodyWriter.class);

		baseConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);
		baseConfig.property(ClientProperties.USE_ENCODING, StandardCharsets.UTF_8.name());
		baseConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);

		baseConfig.connectorProvider(getBasicConectorProvider());

		Client clientBean = ClientBuilder.newClient(baseConfig);

		return clientBean;
	}

}
