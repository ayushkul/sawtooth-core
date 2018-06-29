package sawtooth.sdk.reactive.rest.ops;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.reactive.rest.api.DefaultApi;
import sawtooth.sdk.reactive.rest.invoker.ApiClient;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 * <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 * <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 * Query Parameters:
 	

    head (string) – Index or id of head block
    address (string) – A partial address to filter leaves by
    start (string) – Id to start paging (inclusive)
    limit (integer) – Number of items to return
    reverse (string) – If the list should be reversed

 * 
 *
 */
@Resource
public class RESTStateOps {

	@Inject
	@Any
	DefaultApi globalClient;
	
	private static final String REQPATH = ""; 
	
	private static final Map<String, String> defaultParams = new HashMap<String, String>();
	static {
		defaultParams.put("head", "");
		defaultParams.put("address", "");
		defaultParams.put("start", "");
		defaultParams.put("limit", "");
		defaultParams.put("reverse", "");
	}

	/**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--state
	 */
	public Future<Response> submitBathes(List<Batch> batches) throws InterruptedException, ExecutionException {
		
		return null;
	}

}
