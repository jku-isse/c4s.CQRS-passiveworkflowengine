package impactassessment.passiveprocessengine.instance;

import com.github.oxo42.stateless4j.StateMachine;
import impactassessment.passiveprocessengine.definition.AbstractWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.TaskLifecycle;
import impactassessment.passiveprocessengine.definition.TaskStateTransitionEventPublisher;

public class WorkflowWrapperTaskInstance extends WorkflowTask {

    private String subWorkflowId;
    private AbstractWorkflowDefinition subWfd;

    @Deprecated
    public WorkflowWrapperTaskInstance() {
        super();
    }

    public WorkflowWrapperTaskInstance(String taskId, WorkflowInstance wfi, StateMachine<TaskLifecycle.State, TaskLifecycle.Events> sm, TaskStateTransitionEventPublisher pub, AbstractWorkflowDefinition subWfd) {
        super(taskId, wfi, sm, pub);
        subWorkflowId = "Nested#"+taskId;
        this.subWfd = subWfd;
    }

    public String getSubWorkflowId() {
        return subWorkflowId;
    }

    public AbstractWorkflowDefinition getSubWfd() {
        return subWfd;
    }

}
