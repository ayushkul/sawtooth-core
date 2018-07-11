package sawtooth.sdk.reactive.rest.ops;

import java.util.LinkedHashMap;
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
	public Future<Response> submitBatches(List<Batch> batches) {
		BatchList.Builder rbl = BatchList.newBuilder();
		rbl.addAllBatches(batches);
		WebTarget thisTarget = webTarget.path(REQPATH);
		Invocation.Builder thisBuilder = thisTarget.request();

		return CompletableFuture.supplyAsync(() -> thisBuilder.accept(MediaType.APPLICATION_OCTET_STREAM)
				.post(Entity.entity(rbl.build(), MediaType.APPLICATION_OCTET_STREAM)));
	}

	/**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--batches
	 */

	public Future<LinkedHashMap<String, Object>> listBatches(String head, String start, Integer limit, String reverse) {
		return CompletableFuture.supplyAsync(() -> {
			LinkedHashMap<String, Object> result = null;
			result = null;// (LinkedHashMap<String, Object>) globalClient.batchesGet(head,
							// start, limit, reverse);
			return result;
		});

	}

}
