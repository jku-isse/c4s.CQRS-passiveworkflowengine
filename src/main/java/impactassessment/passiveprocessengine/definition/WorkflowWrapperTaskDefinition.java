package impactassessment.passiveprocessengine.definition;

public class WorkflowWrapperTaskDefinition extends TaskDefinition {

    public WorkflowWrapperTaskDefinition(String definitionId, WorkflowDefinition wfd) {
        super(definitionId, wfd);
    }
    @Deprecated // needed only for neo4j persistence mechanism requires non-arg constructor
    public WorkflowWrapperTaskDefinition() {
        super();
    }

    // TODO
}
