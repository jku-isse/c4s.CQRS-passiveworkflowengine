package impactassessment.workflowmodel;

import org.kie.api.definition.type.Modifies;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public abstract class AbstractArtifact extends AbstractWorkflowInstanceObject implements Artifact  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Property
	public boolean isRemovedAtOrigin = false;
	//@Relationship(type="CONTAINED_IN")
//	@Property
//	private String workflowId = null;	
	@Relationship(type="ARTIFACT_SPECIFIED_BY")
	protected ArtifactType type = null;
	
	
	@Override
	@Modifies( { "isRemovedAtOrigin" } )
	public void setRemovedAtOriginFlag() {
		this.isRemovedAtOrigin = true; 
	}

	@Override
	public boolean isRemovedAtOrigin() {
		return isRemovedAtOrigin;
	}		
	
	public String getWorkflowId() {
		if (this.workflow != null)
			return workflow.getId();
		else if (this.getParentArtifact() != null)
			return this.getParentArtifact().getWorkflowId();
		else return null;
	}

	
	
//	public void setWorkflowId(String workflowId) {		
//		this.workflowId = workflowId;
//	}

	@Override
	public WorkflowInstance getWorkflow() {
		if (this.workflow != null)
			return workflow;
		else if (this.getParentArtifact() != null && this.getParentArtifact() instanceof AbstractWorkflowInstanceObject)
			return ((AbstractWorkflowInstanceObject)this.getParentArtifact()).getWorkflow();
		else return null;
	}

	@Override
	public ArtifactType getType() {
		return this.type;
	}
	
	@Deprecated
	public AbstractArtifact() {}
	
	public AbstractArtifact(String id, ArtifactType at, WorkflowInstance wfi) {
		super(id, wfi);
		this.type = at;
	}
}
