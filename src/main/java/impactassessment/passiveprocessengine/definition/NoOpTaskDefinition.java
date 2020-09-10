package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.WorkflowTask;

public class NoOpTaskDefinition extends TaskDefinition {

    public NoOpTaskDefinition(String definitionId, WorkflowDefinition wfd) {
        super(definitionId, wfd);
    }
    @Deprecated // needed only for neo4j persistence mechanism requires non-arg constructor
    public NoOpTaskDefinition() {
        super();
    }

    @Override
    public TaskLifecycle.InputState calcInputState(WorkflowTask wt) {
        return TaskLifecycle.InputState.INPUT_SUFFICIENT;
    }
    @Override
    public TaskLifecycle.OutputState calcOutputState(WorkflowTask wt) {
        return TaskLifecycle.OutputState.OUTPUT_SUFFICIENT;
    }
}
