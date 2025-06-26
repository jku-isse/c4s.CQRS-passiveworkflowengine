package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypeSelectionResponse {

	static ObjectMapper objectMapper =  JsonMapper.builder().build();
	@Getter final List<String> types;
	
	public static TypeSelectionResponse buildFromString(String response) throws Exception {
		var json = extractJson(response);
		if (json instanceof ArrayNode arrayNode) {
			return new TypeSelectionResponse(parseArray(arrayNode));
		} else
			throw new Exception("Json is not an array");
		
	}

	private static JsonNode extractJson(String response) throws Exception {
		var inner = getStrWithinTrippleComma(response);
		var start = findJsonStartBracket(inner);
		var end = findJsonEndBracket(inner, start);
		var strJson = inner.substring(start, end+1);
		var json = objectMapper.readTree(strJson);
		return json;
	}

	private static String getStrWithinTrippleComma(String response) throws Exception {
		var start = response.indexOf("```", 0);
		var end = response.indexOf("```", start+1);
		if (start == end)
			throw new Exception("Cannot find tripple back ticks surrounding json");
		else {
			return response.substring(start+3, end);
		}
	}
	
	private static int findJsonStartBracket(String mixed) throws Exception{
		var startCurly = mixed.indexOf("{");
		var startSquare = mixed.indexOf("[");
		if (startCurly == -1 && startSquare == -1) 
			throw new Exception("Cannot find starting '{' or '[' bracket");
		if (startCurly == -1) return startSquare;
		if (startSquare == -1) return startCurly;
		else return Math.min(startCurly, startSquare);
	}
	
	private static int findJsonEndBracket(String mixed, int start) throws Exception{
		var startBracket = mixed.charAt(start);
		if (startBracket == '[') {
			var end = mixed.lastIndexOf("]");
			if (end == -1)
				throw new Exception("Cannot find ending '[' bracket");
			else 
				return end;
		} else {
			var end = mixed.lastIndexOf("}");
			if (end == -1)
				throw new Exception("Cannot find ending '}' bracket");
			else 
				return end;
		}		
	}
		
	private static List<String> parseArray(ArrayNode jsonNode) throws Exception {
		List<String> values = new ArrayList<>();
		var iter = jsonNode.elements();
		while (iter.hasNext()) {
			var node = iter.next();
			values.add(node.asText());
		}
		return values;
	}
	
}
