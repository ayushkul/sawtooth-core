package sawtooth.sdk.reactive.common.messaging;

import java.nio.ByteBuffer;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 *         Since each Message Factory follows a particular Adress schema, this interface defines a
 *         common API to do so.
 *
 */
public interface SawtoothAddressFactory {

  public String generateAddress(String... names);
  
  public String generateAddress(ByteBuffer data);
  
  
}
