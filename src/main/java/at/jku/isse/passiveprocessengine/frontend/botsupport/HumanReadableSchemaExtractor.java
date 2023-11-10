package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.artifactconnector.core.model.BaseElementType;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.NonNull;

public class HumanReadableSchemaExtractor {

	private Map<InstanceType, Set<Instance>> samples;
	
	public HumanReadableSchemaExtractor() {
		samples = Collections.emptyMap();
	}
	
	public HumanReadableSchemaExtractor(@NonNull Map<InstanceType, Set<Instance>> samples) {
		this.samples = samples;
	}
	
	public Map<InstanceType, String> getSchemaForInstanceTypeAndOneHop(InstanceType type, boolean includeSubTypes) {
		Map<InstanceType, String> schemas = new HashMap<>();
		Set<InstanceType> foundComplexTypes = new HashSet<>();
		schemas.put(type, getSchema(type, foundComplexTypes));
		foundComplexTypes.stream()
		.filter(cType -> !schemas.containsKey(cType))
		.forEach(cType -> schemas.put(cType,(getSchema(cType, null))));
		// subtypes of type:
		if (includeSubTypes) {
			List<AbstractMap.SimpleEntry<InstanceType, String>> childTypes = schemas.keySet().stream()
				.flatMap(ctype -> ctype.getAllSubTypes().stream())
				.distinct()
				.filter(cType -> !schemas.containsKey(cType))
				.map(cType -> new AbstractMap.SimpleEntry<InstanceType, String>(cType,(getSchema(cType, null))))
				.collect(Collectors.toList());
			childTypes.stream().forEach(tuple -> schemas.put(tuple.getKey(), tuple.getValue()));
		}
		
		return schemas;
	}
	
	public String getSchemaForInstanceType(InstanceType type) {
		return getSchema(type, null);
	}
	
	private String getSchema(InstanceType type, Set<InstanceType> foundComplexTypes) {
		// check if a process or step:
		if (type.name().startsWith(ProcessStep.designspaceTypeId) || type.name().startsWith(ProcessInstance.designspaceTypeId)) {
			return getSchemaForStep(type, foundComplexTypes);
		}
		
		StringBuffer sb = new StringBuffer();
		if (type.superTypes().isEmpty()) {
			sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.name()));
		} else {
			String parent = type.superTypes().iterator().next().name();
			if (BaseElementType.ARTIFACT.name().toLowerCase().equalsIgnoreCase(parent) || "Instance".equalsIgnoreCase(parent)) {
				sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.name()));
			} else {
				sb.append(String.format("\r\nThe %s object is a subtype of %s and consists of the following properties: ", type.name(), parent));
			}
		}
		type.getPropertyTypes(false,  true).stream()
		.forEach(prop ->  { 
			if (foundComplexTypes != null && isAComplexType(prop)) {
				foundComplexTypes.add(prop.referencedInstanceType());
			}
			sb.append(getPropertyDescription(type, prop)); } );
		return sb.toString();
	}
	
	private String getSchemaForStep(InstanceType type, Set<InstanceType> foundComplexTypes) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("\r\nThe process step %s consists of the following input and output properties: ", type.name()));
		type.getPropertyTypes(false,  true).stream()
			.filter(prop -> prop.name().startsWith(ProcessStep.PREFIX_IN) || prop.name().startsWith(ProcessStep.PREFIX_OUT))
			.forEach(prop ->  { 
				if (foundComplexTypes != null && isAComplexType(prop)) {
					foundComplexTypes.add(prop.referencedInstanceType());
				}
				sb.append(getPropertyDescription(type, prop)); } );
		return sb.toString();
	}
	
	private String getPropertyDescription(InstanceType type, PropertyType prop) {
		if (samples.containsKey(type)) { // we have some sample values to add
			String sampleValues = samples.get(type).stream()
					.filter(sType -> sType.getInstanceType().isKindOf(type)) // ensure no typing problem
					.map(sType -> sType.getPropertyAsValue(prop.name()) ) // now we have the values
					.filter(Objects::nonNull) // ignore null objects
					.map(propValue -> PropertyConversionUtil.valueToString(propValue).trim())
					.distinct()
					.filter(strValue -> strValue.length() > 0)
					.collect(Collectors.joining(", "));
			if (sampleValues.length() > 0) {
				return String.format("\r\n %s %s %s e.g., %s", prop.name(), getCardinality(prop), getType(prop), condenseValue(sampleValues));
			} else 
				return String.format("\r\n %s %s %s", prop.name(), getCardinality(prop), getType(prop));
		} else 
			return String.format("\r\n %s %s %s", prop.name(), getCardinality(prop), getType(prop));
	}

	private String condenseValue(String valueString) {
		if (valueString.length() > 100) {
			int posLastWhiteSpace = valueString.lastIndexOf(" ");
			if (posLastWhiteSpace > 80) 
				return valueString.substring(0, posLastWhiteSpace);
			else 
				return valueString.substring(0, 100)+" ...";
		} else
			return valueString;
	}
	
	private boolean isAComplexType(PropertyType prop) {
		if (prop.workspace.META_INSTANCE_TYPE.equals(prop.referencedInstanceType().getInstanceType()))
			return true;
		else
			return false;
	}
	
	private String getType(PropertyType prop) {
		return prop.referencedInstanceType().name();
	}

	private static String getCardinality(PropertyType prop) {
		if (prop.cardinality().equals(Cardinality.SINGLE)) {
			return "of type ";
		} else {
			return "of multiple ";
		}
	}
}
 