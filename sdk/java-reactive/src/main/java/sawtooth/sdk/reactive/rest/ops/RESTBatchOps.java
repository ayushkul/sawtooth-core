package sawtooth.sdk.reactive.rest.ops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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

import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.reactive.config.SawtoothConfiguration;
import sawtooth.sdk.reactive.factory.RESTFactory;
import sawtooth.sdk.reactive.rest.model.RESTBatchList;

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
	private Invocation.Builder iBuilder;

	@PostConstruct
	public void setUp() {
		webTarget = globalClient.target(SawtoothConfiguration.getRESTURL());
		webTarget.register(globalClient.getConfiguration().getClasses());
	}

	/**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#post--batches
	 * 
	 */
	public Future<Response> submitBatches(List<Batch> batches) {
		RESTBatchList rbl = new RESTBatchList();
		rbl.batches(batches.stream().map(bl -> {
			try {
				return RESTFactory.fromBatch(bl);
			}
			catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList()));
		webTarget = webTarget.queryParam("batches", rbl);
		iBuilder = webTarget.request();

		return CompletableFuture.supplyAsync(() -> iBuilder.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(String.class, MediaType.APPLICATION_OCTET_STREAM)));
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
