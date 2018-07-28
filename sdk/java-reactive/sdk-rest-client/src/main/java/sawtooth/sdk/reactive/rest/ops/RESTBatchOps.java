package sawtooth.sdk.reactive.rest.ops;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.reactive.config.RESTClientConfig;
import sawtooth.sdk.reactive.rest.model.RESTBatchListing;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * 
 * 
 */
@Resource
public class RESTBatchOps {

  @Inject
  @Any
  Client globalClient;

  private WebTarget webTarget;
  private static final String REQPATH = "batches";

  @PostConstruct
  public void setUp() {
    webTarget = globalClient.target(RESTClientConfig.getRESTURL());
    webTarget.register(globalClient.getConfiguration().getClasses());
  }

  /**
   * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#post--batches
   * 
   */
  public Future<Response> submitBatches(List<Batch> batches,MediaType mediaType) {
    BatchList.Builder rbl = BatchList.newBuilder();
    rbl.addAllBatches(batches);
    WebTarget thisTarget = webTarget.path(REQPATH);
    Invocation.Builder thisBuilder = thisTarget.request();
    
    return CompletableFuture
        .supplyAsync(() -> thisBuilder.accept(mediaType == null ?  MediaType.APPLICATION_OCTET_STREAM: mediaType.toString())
            .post(Entity.entity(rbl.build(), mediaType == null ?  MediaType.APPLICATION_OCTET_STREAM: mediaType.toString())));
  }

  /**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--batches
	 * 
// @formatter:off
	 * Query Parameters:
	 *     head (string) – Index or id of head block
	 *     start (string) – Id to start paging (inclusive)
	 *     limit (integer) – Number of items to return
	 *     reverse (string) – If the list should be reversed
	 *     
// @formatter:on
	 */

  public Future<RESTBatchListing> listBatches(String head, String start, Integer limit,
      String reverse) {
    WebTarget thisTarget = webTarget.path(REQPATH);
    thisTarget.queryParam("head", head).queryParam("head", head).queryParam("start", start)
        .queryParam("limit", limit).queryParam("reverse", reverse);

    Invocation.Builder thisBuilder = thisTarget.request();

    return CompletableFuture.supplyAsync(
        () -> thisBuilder.accept(MediaType.APPLICATION_JSON).get(RESTBatchListing.class));

  }

}
