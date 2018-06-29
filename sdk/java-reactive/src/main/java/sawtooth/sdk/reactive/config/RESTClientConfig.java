package sawtooth.sdk.reactive.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import sawtooth.sdk.reactive.rest.api.DefaultApi;
import sawtooth.sdk.reactive.rest.invoker.ApiClient;

public class RESTClientConfig {

	int readTimeout = 5000;
	int connectTimeout = 5000;
	int buffersize = 4096;
	
	@Inject
	@Any
	SawtoothConfiguration swMainConfig;

	public final static String DEFAULTCLIENTBEANNAME = "DefaultClientBean";

	public ConnectorProvider getBasicConectorProvider() {
		HttpUrlConnectorProvider providerBean = new HttpUrlConnectorProvider();
		providerBean.chunkSize(buffersize);
		return providerBean;
	}

	public Client getCustomClient() {

		ClientConfig baseConfig = new ClientConfig();

		baseConfig.register(new JacksonFeature());
		baseConfig.register(new JacksonJaxbJsonProvider());

		baseConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);
		baseConfig.property(ClientProperties.USE_ENCODING, "UTF-8");
		baseConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);

		baseConfig.connectorProvider(getBasicConectorProvider());

		Client clientBean = ClientBuilder.newClient(baseConfig);

		return clientBean;
	}

	@Produces
	@ApplicationScoped
	@Named(value = RESTClientConfig.DEFAULTCLIENTBEANNAME)
	public DefaultApi getBasicClient() {

		ApiClient baseConfig = new ApiClient();
		baseConfig.setBasePath(SawtoothConfiguration.getRESTURL());
		baseConfig.setHttpClient(getCustomClient());
		baseConfig.setDebugging(true);

		DefaultApi globalAPI = new DefaultApi(baseConfig);
		return globalAPI;
	}

}
