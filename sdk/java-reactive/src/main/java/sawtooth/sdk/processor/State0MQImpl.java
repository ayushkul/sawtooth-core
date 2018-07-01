/* Copyright 2016, 2017 Intel Corporation
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

package sawtooth.sdk.processor;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sawtooth.sdk.messaging.MessagesStream;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.processor.exceptions.ValidatorConnectionError;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.TpStateEntry;
import sawtooth.sdk.protobuf.TpStateGetRequest;
import sawtooth.sdk.protobuf.TpStateGetResponse;
import sawtooth.sdk.protobuf.TpStateSetRequest;
import sawtooth.sdk.protobuf.TpStateSetResponse;
import sawtooth.sdk.protobuf.Message.MessageType;
import sawtooth.sdk.reactive.factory.MessageFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client state that interacts with the context manager through Stream networking.
 */
public class State0MQImpl implements SawtoothState{

	private MessagesStream stream;
	private String contextId;
	private static final int TIME_OUT = 2;
	MessageFactory mesgFact;
	private final static Logger LOGGER = LoggerFactory.getLogger(State0MQImpl.class);

	public State0MQImpl(MessagesStream stream, String contextId) {
		this.stream = stream;
		this.contextId = contextId;
	}

	/**
	 * Make a Get request on a specific context specified by contextId.
	 *
	 * @param addresses a collection of address Strings
	 * @return Map where the keys are addresses, values Bytestring
	 * @throws InternalError something went wrong processing transaction
	 * @throws InvalidProtocolBufferException 
	 */
	public Map<String, ByteString> getState(List<String> addresses) throws InternalError, InvalidTransactionException, InvalidProtocolBufferException {
		
		Future<Message> future = stream.send(mesgFact.getStateRequest(addresses));
		Message getResponse = null;
		try {
			getResponse = future.get(TIME_OUT, TimeUnit.SECONDS);
		}
		catch (InterruptedException iee) {
			throw new InternalError(iee.toString());
		}
		catch (Exception e) {
			throw new InternalError(e.toString());
		}
		Map<String, ByteString> results = new HashMap<String, ByteString>();
		if (getResponse != null) {
			if (! getResponse.getMessageType().equals(MessageType.TP_STATE_GET_RESPONSE)) {
				LOGGER.info("Not a response , got "+getResponse.getMessageType().name()+" instead.");
			}
			TpStateGetResponse responsePayload = TpStateGetResponse.parseFrom(getResponse.getContent());
			if (responsePayload.getStatus() == TpStateGetResponse.Status.AUTHORIZATION_ERROR) {
				throw new InvalidTransactionException("Tried to get unauthorized address " + addresses.toString());
			}
			for (TpStateEntry entry : responsePayload.getEntriesList()) {
				results.put(entry.getAddress(), entry.getData());
			}
		}
		if (results.isEmpty()) {
			throw new InternalError("State Error, no result found for get request:" + addresses.toString());
		}

		return results;
	}

	/**
	 * Make a Set request on a specific context specified by contextId.
	 *
	 * @param addressValuePairs A collection of Map.Entry's
	 * @return addressesThatWereSet, A collection of address Strings that were set
	 * @throws InternalError something went wrong processing transaction
	 */
	public Collection<String> setState(List<java.util.Map.Entry<String, ByteString>> addressValuePairs)
			throws InternalError, InvalidTransactionException {
		
		Future<Message> future = stream.send(mesgFact.getSetStateRequest(this.contextId,addressValuePairs));
		Message setResponse = null;
		try {
			setResponse = future.get(TIME_OUT,TimeUnit.SECONDS);
		}
		catch (InterruptedException iee) {
			throw new InternalError(iee.toString());

		}
		catch (Exception e) {
			throw new InternalError(e.toString());
		}
		ArrayList<String> addressesThatWereSet = new ArrayList<String>();
		if (setResponse != null) {
			TpStateSetResponse realResponse;
			try {
				realResponse = mesgFact.createTpStateSetResponse(setResponse);
			}
			catch (InvalidProtocolBufferException ipbe) {
				// server didn't respond with a SetResponse
				throw new InternalError(ipbe.toString());
			}
			if (realResponse.getStatus() == TpStateSetResponse.Status.AUTHORIZATION_ERROR) {
				throw new InvalidTransactionException(
						"Tried to set unauthorized address " + addressValuePairs.toString());
			}
			for (String address : realResponse.getAddressesList()) {
				addressesThatWereSet.add(address);
			}
		}

		return addressesThatWereSet;
	}

}
