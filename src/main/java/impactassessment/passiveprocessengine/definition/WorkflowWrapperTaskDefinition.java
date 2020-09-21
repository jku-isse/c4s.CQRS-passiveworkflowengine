package impactassessment.passiveprocessengine.definition;

public class WorkflowWrapperTaskDefinition extends TaskDefinition {

    private AbstractWorkflowDefinition internalWfd;

    public WorkflowWrapperTaskDefinition(String definitionId, WorkflowDefinition wfd, AbstractWorkflowDefinition internalWfd) {
        super(definitionId, wfd);
        this.internalWfd = internalWfd;
    }
    @Deprecated // needed only for neo4j persistence mechanism requires non-arg constructor
    public WorkflowWrapperTaskDefinition() {
        super();
    }

    public AbstractWorkflowDefinition getInternalWfd() {
        return internalWfd;
    }

    public void setInternalWfd(AbstractWorkflowDefinition internalWfd) {
        this.internalWfd = internalWfd;
    }

    // TODO

}
