package c4s.impactassessment.tools;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class URLExtractor {

	// from: https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
	
	public static List<Map.Entry<String, String>> parseQueryPart(String queryPart) {
		if (queryPart==null) return Collections.emptyList();
		List<Map.Entry<String, String>> list = Pattern.compile("&")
				   .splitAsStream(queryPart)
				   .map(s -> Arrays.copyOf(s.split("=", 2), 2))
				   .map(o -> Map.entry(decode(o[0]), decode(o[1])))
				   .collect(Collectors.toList());
		return list;
	}
	
	public static String returnFirstValueFromQueryForKey(String queryPart, String key) {
		List<Map.Entry<String, String>> list = parseQueryPart(queryPart);
		return list.stream().filter(entry -> entry.getKey().equals(key)).findFirst().orElse(new AbstractMap.SimpleEntry<String,String>(key, "")).getValue();
	}
	
	private static String decode(final String encoded) {
	    return Optional.ofNullable(encoded)
	                   .map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
	                   .orElse(null);
	}
}
