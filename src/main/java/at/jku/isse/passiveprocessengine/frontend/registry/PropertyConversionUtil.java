package at.jku.isse.passiveprocessengine.frontend.registry;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;

public class PropertyConversionUtil {

	public static String valueToString(Object value) {
		if (value == null) return "null";
		if (value instanceof PPEInstance ) {
			return ((PPEInstance) value).getName();
		}
		else if (value instanceof PPEInstanceType) {
			return ((PPEInstanceType) value).getName();
		} else if (value instanceof Map) {
			return (String) ((Map) value).entrySet().stream()
					.map(entry-> ((Map.Entry<String, Object>)entry).getKey()+valueToString(((Map.Entry<String, Object>)entry).getValue()) )
					.collect(Collectors.joining(", "));
		} else if (value instanceof Collection) {
			return (String) ((Collection) value).stream().map(v->valueToString(v)).collect(Collectors.joining(", "));
		} else if (value instanceof PPEPropertyType) {
			return ((PPEPropertyType) value).getName();
		}
		else
			return value.toString();

	}
}
