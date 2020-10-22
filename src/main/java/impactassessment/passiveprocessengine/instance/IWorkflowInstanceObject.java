package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.IdentifiableObject;

public interface IWorkflowInstanceObject extends IdentifiableObject {
    WorkflowInstance getWorkflow();

    //	@Modifies( { "workflow" } )
    void setWorkflow(WorkflowInstance wfi);
}
