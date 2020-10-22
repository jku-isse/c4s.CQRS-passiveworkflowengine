package impactassessment.passiveprocessengine.definition;

public class WorkflowWrapperTaskDefinition extends TaskDefinition {

    private String subWfdId;

    public WorkflowWrapperTaskDefinition(String definitionId, WorkflowDefinition wfd, String subWfdId) {
        super(definitionId, wfd);
        this.subWfdId = subWfdId;
    }
    @Deprecated // needed only for neo4j persistence mechanism requires non-arg constructor
    public WorkflowWrapperTaskDefinition() {
        super();
    }

    public String getSubWfdId() {
        return subWfdId;
    }

    public void setSubWfdId(String subWfdId) {
        this.subWfdId = subWfdId;
    }

    // TODO

}
