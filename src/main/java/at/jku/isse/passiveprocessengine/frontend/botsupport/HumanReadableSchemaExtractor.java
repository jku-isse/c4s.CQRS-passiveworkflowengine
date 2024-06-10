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

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.NonNull;

public class HumanReadableSchemaExtractor {

	private Map<PPEInstanceType, Set<PPEInstance>> samples;
	final SchemaRegistry schemaReg;
	final PPEInstanceType stepType;
	final PPEInstanceType procType;
	final PPEInstanceType artType;
	
	public HumanReadableSchemaExtractor(@NonNull SchemaRegistry schemaReg) {
		samples = Collections.emptyMap();
		this.schemaReg = schemaReg;
		this.stepType = schemaReg.getType(ProcessStep.class);
		this.procType = schemaReg.getType(ProcessInstance.class);
		this.artType = schemaReg.getTypeByName(CoreTypeFactory.BASE_TYPE_NAME);
	}
	
	public Map<PPEInstanceType, String> getSchemaForInstanceTypeAndOneHop(PPEInstanceType type, boolean includeSubTypes) {
		Map<PPEInstanceType, String> schemas = new HashMap<>();
		Set<PPEInstanceType> foundComplexTypes = new HashSet<>();
		schemas.put(type, getSchema(type, foundComplexTypes));
		foundComplexTypes.stream()
		.filter(cType -> !schemas.containsKey(cType))
		.forEach(cType -> schemas.put(cType,(getSchema(cType, null))));
		// subtypes of type:
		if (includeSubTypes) {
			List<AbstractMap.SimpleEntry<PPEInstanceType, String>> childTypes = schemas.keySet().stream()
				.flatMap(ctype -> ctype.getAllSubtypesRecursively().stream())
				.distinct()
				.filter(cType -> !schemas.containsKey(cType))
				.map(cType -> new AbstractMap.SimpleEntry<PPEInstanceType, String>(cType,(getSchema(cType, null))))
				.collect(Collectors.toList());
			childTypes.stream().forEach(tuple -> schemas.put(tuple.getKey(), tuple.getValue()));
		}		
		return schemas;
	}
	
	public String getSchemaForInstanceType(PPEInstanceType type) {
		return getSchema(type, null);
	}
	
	private String getSchema(PPEInstanceType type, Set<PPEInstanceType> foundComplexTypes) {
		// check if a process or step:
		
		if (type.isOfTypeOrAnySubtype(stepType)) {
			return getSchemaForStep(type, foundComplexTypes);
		}
		
		StringBuffer sb = new StringBuffer();
		if (type.getParentType()==null) {
			sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.getName()));
		} else {
			String parent = type.getParentType().getName();
			if ( type.getParentType().isOfTypeOrAnySubtype(artType)) {
				sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.getName()));
			} else {
				sb.append(String.format("\r\nThe %s object is a subtype of %s and consists of the following properties: ", type.getName(), parent));
			}
		}
		type.getPropertyNamesIncludingSuperClasses().stream()
		.map(propName -> type.getPropertyType(propName))
		.forEach(prop ->  { 
			if (foundComplexTypes != null && isAComplexType(prop)) {
				foundComplexTypes.add(prop.getInstanceType());
			}
			sb.append(getPropertyDescription(type, prop)); } );
		return sb.toString();
	}
	
	private String getSchemaForStep(PPEInstanceType type, Set<PPEInstanceType> foundComplexTypes) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("\r\nThe process step %s consists of the following input and output properties: ", type.getName()));
		type.getPropertyNamesIncludingSuperClasses().stream()
			.map(propName -> type.getPropertyType(propName))
			.filter(prop -> prop.getName().startsWith(SpecificProcessStepType.PREFIX_IN) || prop.getName().startsWith(SpecificProcessStepType.PREFIX_OUT))
			.forEach(prop ->  { 
				if (foundComplexTypes != null && isAComplexType(prop)) {
					foundComplexTypes.add(prop.getInstanceType());
				}
				sb.append(getPropertyDescription(type, prop)); } );
		return sb.toString();
	}
	
	private String getPropertyDescription(PPEInstanceType type, PPEPropertyType prop) {
		if (samples.containsKey(type)) { // we have some sample values to add
			String sampleValues = samples.get(type).stream()
					.filter(sType -> sType.getInstanceType().isOfTypeOrAnySubtype(type)) // ensure no typing problem
					.map(sType -> sType.getTypedProperty(prop.getName(), Object.class) ) // now we have the values
					.filter(Objects::nonNull) // ignore null objects
					.map(propValue -> PropertyConversionUtil.valueToString(propValue).trim())
					.distinct()
					.filter(strValue -> strValue.length() > 0)
					.collect(Collectors.joining(", "));
			if (sampleValues.length() > 0) {
				return String.format("\r\n %s %s %s e.g., %s", prop.getName(), getCardinality(prop), getType(prop), condenseValue(sampleValues));
			} else 
				return String.format("\r\n %s %s %s", prop.getName(), getCardinality(prop), getType(prop));
		} else 
			return String.format("\r\n %s %s %s", prop.getName(), getCardinality(prop), getType(prop));
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
	
	private boolean isAComplexType(PPEPropertyType prop) {
		if (BuildInType.isAtomicType(prop.getInstanceType().getInstanceType()))
			return false;
		else
			return true;
	}
	
	private String getType(PPEPropertyType prop) {
		return prop.getInstanceType().getName();
	}

	private static String getCardinality(PPEPropertyType prop) {
		if (prop.getCardinality().equals(CARDINALITIES.SINGLE)) {
			return "of type ";
		} else {
			return "of multiple ";
		}
	}
}
 