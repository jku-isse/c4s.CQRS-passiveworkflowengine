package impactassessment.passiveprocessengine.definition;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * A MappingDefinition is defined by a List of TaskDefinition IDs "from", a List of TaskDefinition IDs "to"
 * and the "mappingType"
 * A DecisionNodeDefinition can have 0 to * MappingDefinitions
 * If a MappingDefinition is defined the DecisionNodeInstance of this will map the ArtifactOutputs from the
 * WorkflowTask corresponding to "from" to the ArtifactInputs from the WorkflowTask corresponding to "to".
 */
public class MappingDefinition {
    private @Getter List<Pair<String, String>> from = new ArrayList<>();
    private @Getter List<Pair<String, String>> to = new ArrayList<>();
    private @Getter MappingType mappingType;
    public MappingDefinition(String fromId, String fromRole, String toId, String toRole, MappingType mappingType) {
        this.from.add(Pair.of(fromId, fromRole));
        this.to.add(Pair.of(toId, toRole));
        this.mappingType = mappingType;
    }
    public MappingDefinition(String fromId, String fromRole, String toId, String toRole) {
        this(fromId, fromRole, toId, toRole, MappingType.ANY);
    }
    public MappingDefinition(List<Pair<String, String>> from, String toId, String toRole, MappingType mappingType) {
        this.from = from;
        this.to.add(Pair.of(toId, toRole));
        this.mappingType = mappingType;
    }
    public MappingDefinition(List<Pair<String, String>> from, String toId, String toRole) {
        this(from, toId, toRole, MappingType.ANY);
    }
    public MappingDefinition(String fromId, String fromRole, List<Pair<String, String>> to, MappingType mappingType) {
        this.from.add(Pair.of(fromId, fromRole));
        this.to = to;
        this.mappingType = mappingType;
    }
    public MappingDefinition(String fromId, String fromRole, List<Pair<String, String>> to) {
        this(fromId, fromRole, to, MappingType.ANY);
    }
    public MappingDefinition(List<Pair<String, String>> from, List<Pair<String, String>> to, MappingType mappingType) {
        this.from = from;
        this.to = to;
        this.mappingType = mappingType;
    }
    public MappingDefinition(List<Pair<String, String>> from, List<Pair<String, String>> to) {
        this(from, to, MappingType.ANY);
    }

    public enum MappingType {ALL, ANY}

    @Data(staticConstructor = "of")
    public static class Pair<A, B> {
        private final A first;
        private final B second;
    }
}
