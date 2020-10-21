package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.WorkflowTask;
import lombok.Getter;

public class NoOpTaskDefinition extends TaskDefinition {

    private @Getter boolean isNoOp;

    public NoOpTaskDefinition(String definitionId, WorkflowDefinition wfd) {
        super(definitionId, wfd);
        this.isNoOp = true;
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
