package impactassessment.model.workflowmodel;

import java.util.List;

public interface WorkflowDefinition {

    public List<TaskDefinition> getWorkflowTaskDefinitions();
    public List<DecisionNodeDefinition> getDecisionNodeDefinitions();
    public DecisionNodeDefinition getDNIbyID(String dndID);
    public TaskDefinition getTDbyID(String tdID);
    public String getId();
    public WorkflowInstance createInstance(String withOptionalId);//, KieSession intoOptionalKSession);

}
