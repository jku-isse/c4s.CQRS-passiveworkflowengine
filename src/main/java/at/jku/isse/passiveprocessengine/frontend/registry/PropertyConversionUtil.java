package at.jku.isse.passiveprocessengine.frontend.registry;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;

public class PropertyConversionUtil {

	public static String valueToString(Object value) {
		if (value == null) return "null";
		if (value instanceof Instance ) {
			return ((Instance) value).name();
		}
		else if (value instanceof InstanceType) {
			return ((InstanceType) value).name();
		} else if (value instanceof Map) {
			return (String) ((Map) value).entrySet().stream()
					.map(entry-> ((Map.Entry<String, Object>)entry).getKey()+valueToString(((Map.Entry<String, Object>)entry).getValue()) )
					.collect(Collectors.joining(", "));
		} else if (value instanceof Collection) {
			return (String) ((Collection) value).stream().map(v->valueToString(v)).collect(Collectors.joining(", "));
		} else if (value instanceof PropertyType) {
			return ((PropertyType) value).name();
		}
		else
			return value.toString();

	}
}
