package impactassessment.passiveprocessengine.instance;

import impactassessment.passiveprocessengine.definition.IdentifiableObject;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import impactassessment.neo4j.WorkflowInstanceConverter;

@NodeEntity
public abstract class AbstractWorkflowInstanceObject extends IdentifiableObject {
	
	@Convert(WorkflowInstanceConverter.class)
	protected WorkflowInstance workflow;
	
	public WorkflowInstance getWorkflow() {
		return workflow;
	}

//	@Modifies( { "workflow" } )
	public void setWorkflow(WorkflowInstance wfi) {
		this.workflow = wfi;
	}

	public AbstractWorkflowInstanceObject(String id, WorkflowInstance wfi) {
		super(id);
		this.workflow = wfi;
	}
	
	
	@Deprecated  
	public AbstractWorkflowInstanceObject() {
		super();
	}
	
}
