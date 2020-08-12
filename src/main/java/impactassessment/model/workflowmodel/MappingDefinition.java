package impactassessment.model.workflowmodel;

import java.util.ArrayList;
import java.util.List;

/**
 * A MappingDefinition is defined by a List of TaskDefinition IDs "from", a List of TaskDefinition IDs "to"
 * and the "mappingType"
 * A DecisionNodeDefinition can have 0 to * MappingDefinitions
 * If a MappingDefinition is defined the DecisionNodeInstance of this will map the ArtifactOutputs from the
 * WorkflowTask corresponding to "from" to the ArtifactInputs from the WorkflowTask corresponding to "to".
 */
class MappingDefinition {
    private List<String> from = new ArrayList<>();
    private List<String> to = new ArrayList<>();
    private MappingType mappingType;
    public MappingDefinition(String from, String to, MappingType mappingType) {
        this.from.add(from);
        this.to.add(to);
        this.mappingType = mappingType;
    }
    public MappingDefinition(String from, String to) {
        this(from, to, MappingType.ANY);
    }
    public MappingDefinition(List<String> from, String to, MappingType mappingType) {
        this.from = from;
        this.to.add(to);
        this.mappingType = mappingType;
    }
    public MappingDefinition(List<String> from, String to) {
        this(from, to, MappingType.ANY);
    }
    public MappingDefinition(String from, List<String> to, MappingType mappingType) {
        this.from.add(from);
        this.to = to;
        this.mappingType = mappingType;
    }
    public MappingDefinition(String from, List<String> to) {
        this(from, to, MappingType.ANY);
    }
    public MappingDefinition(List<String> from, List<String> to, MappingType mappingType) {
        this.from = from;
        this.to = to;
        this.mappingType = mappingType;
    }
    public MappingDefinition(List<String> from, List<String> to) {
        this(from, to, MappingType.ANY);
    }

    public List<String> getFrom() {
        return from;
    }

    public List<String> getTo() {
        return to;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public enum MappingType {ALL, ANY}
}
