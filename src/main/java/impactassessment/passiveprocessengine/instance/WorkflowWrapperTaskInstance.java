package impactassessment.passiveprocessengine.instance;

import com.github.oxo42.stateless4j.StateMachine;
import impactassessment.passiveprocessengine.definition.AbstractWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.TaskLifecycle;
import impactassessment.passiveprocessengine.definition.TaskStateTransitionEventPublisher;
import lombok.Getter;

public class WorkflowWrapperTaskInstance extends WorkflowTask {

    private @Getter String subWfdId;
    private @Getter String subWfiId;

    @Deprecated
    public WorkflowWrapperTaskInstance() {
        super();
    }

    public WorkflowWrapperTaskInstance(String taskId, WorkflowInstance wfi, StateMachine<TaskLifecycle.State, TaskLifecycle.Events> sm, TaskStateTransitionEventPublisher pub, String subWfdId) {
        super(taskId, wfi, sm, pub);
        subWfiId = "Nested#"+taskId;
        this.subWfdId = subWfdId;
    }

}
