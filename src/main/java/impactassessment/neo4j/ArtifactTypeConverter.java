package impactassessment.neo4j;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

import impactassessment.passiveprocessengine.definition.ArtifactType;

public abstract class ArtifactTypeConverter implements CompositeAttributeConverter<Map<String,ArtifactType>>{

	protected String prefix;
	
	@Override
	public Map<String, ArtifactType> toEntityAttribute(Map<String, ?> arg0) {
		Map<String, ArtifactType> artTypes = new HashMap<String, ArtifactType>();
		arg0.entrySet().stream()
			.filter(entry -> entry.getKey().startsWith(prefix))
			.filter(entry -> entry.getValue() instanceof String)
			.map(entry -> new AbstractMap.SimpleEntry<String, String>(entry.getKey().substring(prefix.length()), (String)entry.getValue()))
			.forEach(entry -> artTypes.put(entry.getKey(), new ArtifactType(entry.getValue())));
		return artTypes;
	}

	@Override
	public Map<String, ?> toGraphProperties(Map<String, ArtifactType> arg0) {
		Map<String, String> properties = new HashMap<String, String>();
		arg0.entrySet().stream().forEach(entry -> properties.put(prefix+entry.getKey(), entry.getValue().getArtifactType()));
	return properties;
	}

	
	public static class Input extends ArtifactTypeConverter {
		public Input() {
			super.prefix = "input.";
		}
	}
	
	public static class Output extends ArtifactTypeConverter {
		public Output() {
			super.prefix = "output.";
		}
	}
}
