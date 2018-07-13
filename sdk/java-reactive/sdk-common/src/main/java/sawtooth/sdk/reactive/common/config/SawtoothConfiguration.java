package sawtooth.sdk.reactive.common.config;

import java.security.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 * <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 * <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * 
// @formatter:off
 *	
 *  Basic bootstrap of some global properties of the packages:
 *
 *	- Asynchronous Logging
 *	- Spongy Castle registration on JCE
 *
// @formatter:on
 */
public class SawtoothConfiguration {


  private final static Logger LOGGER = LoggerFactory.getLogger(SawtoothConfiguration.class);
  static {
    if (LOGGER.isInfoEnabled())
      LOGGER.info("Registering Async logging...");
    System.setProperty(org.apache.logging.log4j.core.util.Constants.LOG4J_CONTEXT_SELECTOR,
        org.apache.logging.log4j.core.async.AsyncLoggerContextSelector.class.getName());
    if (LOGGER.isInfoEnabled())
      LOGGER.info("Async logging enabled.");


    if (LOGGER.isInfoEnabled())
      LOGGER.info("Registering Spongy Castle...");
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    if (LOGGER.isInfoEnabled())
      LOGGER.info("Spongy Castle registered.");
  }



}
