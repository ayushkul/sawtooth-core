package sawtooth.sdk.reactive.config;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

import sawtooth.sdk.reactive.config.SawtoothConfiguration;

public class RESTClientConfig {

	int readTimeout = 5000;
	int connectTimeout = 5000;
	int buffersize = 4096;
	
	public final static String DEFAULTCLIENTBEANNAME = "DefaultClientBean";
	
	public ConnectorProvider getBasicConectorProvider() {
		HttpUrlConnectorProvider providerBean = new HttpUrlConnectorProvider();
		providerBean.chunkSize(buffersize);
		return providerBean;
	}
	
	@Produces
	@ApplicationScoped
	@Named(value=RESTClientConfig.DEFAULTCLIENTBEANNAME)
	public Client getBasicClient() {

		ClientConfig baseConfig = new ClientConfig();
		

		baseConfig.register(JacksonFeature.class);
		baseConfig.register(JacksonJaxbJsonProvider.class);
		baseConfig.register(MoxyJsonFeature.class);

		baseConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);
		baseConfig.property(ClientProperties.USE_ENCODING, "UTF-8");
		baseConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);

		baseConfig.connectorProvider(getBasicConectorProvider());

		Client clientBean = ClientBuilder.newClient(baseConfig);

		return clientBean;
	}

}
