package sawtooth.sdk.reactive.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SawtoothConfiguration {


	private static final Properties configProperties = new Properties();
	private static final ClassLoader loader = SawtoothConfiguration.class.getClassLoader();
	private final static Logger LOGGER = LoggerFactory.getLogger(SawtoothConfiguration.class);
	static {
		
        try (InputStream in = loader.getResourceAsStream("config.properties")) {
        	configProperties.load( in );

        }
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String validatorURL = configProperties.getProperty("sawtooth.validator.url");
	
	private static String apiRESTURL= configProperties.getProperty("sawtooth.rest.url");

	public static String getValidatorURL() {
		return validatorURL;
	}

	public static String getRESTURL() {
		return apiRESTURL;
	}


}
