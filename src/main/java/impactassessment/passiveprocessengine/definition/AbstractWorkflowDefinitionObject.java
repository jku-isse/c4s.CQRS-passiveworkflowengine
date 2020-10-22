package impactassessment.passiveprocessengine.definition;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public abstract class AbstractWorkflowDefinitionObject extends AbstractIdentifiableObject {
	
	
	@Relationship(type="WorkflowDefinition")
	transient protected WorkflowDefinition wfd;
	
	public WorkflowDefinition getWorkflowDefinition() {
		return wfd;
	}



	public void setWorkflowDefinition(WorkflowDefinition wfd) {
		this.wfd = wfd;
	}



	public AbstractWorkflowDefinitionObject(String id, WorkflowDefinition wfd) {
		super(id);
		this.wfd = wfd;
	}
	
	@Deprecated  
	public AbstractWorkflowDefinitionObject() {
		super();
	}
	
}
