package sawtooth.sdk.reactive.rest.ops;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import sawtooth.sdk.reactive.config.RESTClientConfig;
import sawtooth.sdk.reactive.rest.model.RESTEntry;
import sawtooth.sdk.reactive.rest.model.RESTState;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 * <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 * <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * 
 */
@Resource
public class RESTStateOps {

  @Inject
  @Any
  Client globalClient;

  protected Map<String, String> defaultParams = new HashMap<String, String>();
  private WebTarget webTarget;
  private static final String REQPATH = "state";

  @PostConstruct
  public void setUp() {
    webTarget = globalClient.target(RESTClientConfig.getRESTURL());
    webTarget.register(globalClient.getConfiguration().getClasses());
  }

  /**
   * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--state
   * 
   *Query Parameters:
   *   
// @formatter:off
   *
   * head (string) – Index or id of head block 
   * address (string) – A partial address to filter leaves by 
   * start (string) – Id to start paging (inclusive) 
   * limit (integer) – Number of items to return 
   * reverse (string) – If the list should be reversed (true/false)
 *
// @formatter:on
 *
   */
  public Future<RESTState> getAllStates(String head, String address, String start, int limit,
      boolean reverse) throws InterruptedException, ExecutionException {
    WebTarget thisTarget = webTarget.path(REQPATH);
    thisTarget.queryParam("head", head).queryParam("address", address).queryParam("start", start)
        .queryParam("limit", limit).queryParam("reverse", reverse);

    Invocation.Builder thisBuilder = thisTarget.request();

    return CompletableFuture
        .supplyAsync(() -> thisBuilder.accept(MediaType.APPLICATION_JSON).get(RESTState.class));
  }
  
  /**
   * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--state-address
   * 
   *Query Parameters:
   *   
// @formatter:off
   *
   * address (string) – Radix address of a leaf - URL
   * head (string) – Index or id of head blockhead (string) – Index or id of head block - GET
   *   
// @formatter:on
 *
   */
  public Future<RESTEntry> getOneState(String address , String head) throws InterruptedException, ExecutionException {
    WebTarget thisTarget = webTarget.path(REQPATH+"/"+address);
    if (head != null && ! head.isEmpty()) {
      thisTarget.queryParam("head", head);
    }
    Invocation.Builder thisBuilder = thisTarget.request();

    return CompletableFuture
        .supplyAsync(() -> thisBuilder.accept(MediaType.APPLICATION_JSON).get(RESTEntry.class));
  }

}
