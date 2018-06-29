package sawtooth.sdk.reactive.rest.ops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.reactive.factory.RESTFactory;
import sawtooth.sdk.reactive.rest.api.DefaultApi;
import sawtooth.sdk.reactive.rest.invoker.ApiException;
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
	DefaultApi globalClient;

	/**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#post--batches
	 * 
	 */
	public Future<Object> submitBatches(List<Batch> batches) {
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

		return CompletableFuture.supplyAsync(() -> {
			Object result = null;
			try {
				result = globalClient.batchesPost(rbl);
			}
			catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
		});
	}
	
	/**
	 * https://sawtooth.hyperledger.org/docs/core/releases/latest/rest_api/endpoint_specs.html#get--batches
	 */
	
	public Future<LinkedHashMap<String, Object>> listBatches(String head, String start, Integer limit, String reverse) {
		return CompletableFuture.supplyAsync(() -> {
			LinkedHashMap<String, Object> result = null;
			try {
				result = (LinkedHashMap<String, Object>) globalClient.batchesGet(head, start, limit, reverse);
			}
			catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
		});
		
	}

}
