package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.WorkflowInstance;

import java.util.List;

public interface WorkflowDefinition extends ITaskDefinition {

    public List<TaskDefinition> getWorkflowTaskDefinitions();
    public List<DecisionNodeDefinition> getDecisionNodeDefinitions();
    public DecisionNodeDefinition getDNDbyID(String dndID);
    public TaskDefinition getTDbyID(String tdID);
    public String getId();
    public WorkflowInstance createInstance(String withOptionalId);//, KieSession intoOptionalKSession);

    void setTaskStateTransitionEventPublisher(TaskStateTransitionEventPublisher pub);

}
