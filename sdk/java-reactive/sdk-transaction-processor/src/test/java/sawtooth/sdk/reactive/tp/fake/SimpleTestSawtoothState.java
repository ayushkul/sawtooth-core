package sawtooth.sdk.reactive.tp.fake;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import sawtooth.sdk.reactive.common.exceptions.InternalError;
import sawtooth.sdk.reactive.common.exceptions.InvalidTransactionException;
import sawtooth.sdk.reactive.tp.processor.SawtoothState;

/**
 * 
 * @author Leonardo T. de Carvalho
 * 
 *         <a href="https://github.com/CarvalhoLeonardo">GitHub</a>
 *         <a href="https://br.linkedin.com/in/leonardocarvalho">LinkedIn</a>
 * 
 *         Simple in-memory implementation of a sawtooth ledger state.
 *
 */
public class SimpleTestSawtoothState implements SawtoothState {

  private final ConcurrentHashMap<String, ByteString> verySimpleStateHolder =
      new ConcurrentHashMap<>();

  @Override
  public Map<String, ByteString> getState(List<String> addresses) {
    final Map<String, ByteString> result = new HashMap<>();
    if (addresses != null && !addresses.isEmpty()) {
      verySimpleStateHolder.entrySet().stream().filter(ee -> {
        return addresses.contains(ee.getKey());
      }).map(eo -> {
        return result.putIfAbsent(eo.getKey(), eo.getValue());
      });
    }
    return result;
  }

  @Override
  public Collection<String> setState(List<Entry<String, ByteString>> addressValuePairs)
      throws InternalError, InvalidTransactionException {
    if (addressValuePairs != null && !addressValuePairs.isEmpty()) {
      return addressValuePairs.stream().map(eo -> {
        verySimpleStateHolder.putIfAbsent(eo.getKey(), eo.getValue());
        return eo.getKey();
      }).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Override
  public ByteString AddEvent(String eventType, Map<String, String> attributes, ByteString extraData)
      throws InternalError, InvalidTransactionException, InvalidProtocolBufferException {
    throw new InternalError("Not Implemented");
  }

}
