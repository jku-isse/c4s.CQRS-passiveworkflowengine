package impactassessment.passiveprocessengine.instance;

import com.github.oxo42.stateless4j.StateMachine;
import impactassessment.passiveprocessengine.definition.AbstractWorkflowDefinition;
import impactassessment.passiveprocessengine.definition.TaskLifecycle;
import impactassessment.passiveprocessengine.definition.TaskStateTransitionEventPublisher;

import java.util.List;

public class WorkflowWrapperTaskInstance extends WorkflowTask {

    WorkflowInstance internalWfi;

    @Deprecated
    public WorkflowWrapperTaskInstance() {
        super();
    }

    public WorkflowWrapperTaskInstance(String taskId, WorkflowInstance wfi, StateMachine<TaskLifecycle.State, TaskLifecycle.Events> sm, TaskStateTransitionEventPublisher pub, AbstractWorkflowDefinition internalWfd) {
        super(taskId, wfi, sm, pub);
        internalWfd.setTaskStateTransitionEventPublisher(pub);
        this.internalWfi = internalWfd.createInstance(getId()+"#SubWorkflow");
        this.internalWfi.enableWorkflowTasksAndDecisionNodes(); // TODO add these awos into kieSession
    }

    @Override
    public List<ArtifactOutput> getOutput() {
        // outputs from the wrapper task itself (not sure if there will be any)
        List<ArtifactOutput> outputs = super.getOutput();
        // outputs from every task in the sub workflow
        for (WorkflowTask wft : internalWfi.getWorkflowTasksReadonly()) {
            outputs.addAll(wft.getOutput());
        }
        return super.getOutput();
    }

    @Override
    public void addInput(ArtifactInput ai) {
        // add input to every WFT (only those directly after kickoff are available when this is called)
        for (WorkflowTask wft : internalWfi.getWorkflowTasksReadonly()) {
            // only if ArtifactType matches
            if (wft.getType().getExpectedInput().values().stream().anyMatch(artType -> artType.equals(ai.getArtifactType()))) {
                wft.addInput(ai);
            }
        }
    }
}
