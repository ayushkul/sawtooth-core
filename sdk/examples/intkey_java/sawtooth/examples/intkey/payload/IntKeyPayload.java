package sawtooth.examples.intkey.payload;

import java.util.HashMap;
import java.util.Map;

public class IntKeyPayload {

	private final Map<String, String> data = new HashMap<>();
	
	public String getVerb() {
		return data.get("verb");
	}

	public String getName() {
		return data.get("name");
		
	}

	public String getValue() {
		return data.get("value");
	}

	public IntKeyPayload(String verb, String name, int value) {
		data.put("verb", verb);
		data.put("name ", name);
		data.put("value", String.valueOf(value));
	}
	
	public Map<String,String> toHash(){
		return data; 
	}

}
