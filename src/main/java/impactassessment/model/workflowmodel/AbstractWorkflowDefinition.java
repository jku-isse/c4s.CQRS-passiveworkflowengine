package impactassessment.model.workflowmodel;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.google.inject.Inject;

@NodeEntity
public abstract class AbstractWorkflowDefinition extends AbstractWorkflowDefinitionObject implements WorkflowDefinition {

	@Relationship("TASK_DEFINITION")
	protected List<TaskDefinition> taskDefinitions = new ArrayList<TaskDefinition>();
	@Relationship("DECISIONNODE_DEFINITION")
	protected List<DecisionNodeDefinition> dnds = new ArrayList<DecisionNodeDefinition>();
	
	@Inject
	protected transient TaskStateTransitionEventPublisher pub;
	
	public void setTaskStateTransitionEventPublisher(TaskStateTransitionEventPublisher pub) {
		this.pub = pub;
	}
	
	@Deprecated
	public AbstractWorkflowDefinition(){}
	
	public AbstractWorkflowDefinition(String id) {
		super(id, null);
	}

	@Override
	public DecisionNodeDefinition getDNDbyID(String dndID) {
		return this.dnds.stream()
			.filter(dnd -> dnd.getId().equals(dndID))
			.findAny()
			.orElse(null);		
	}
	
	@Override
	public TaskDefinition getTDbyID(String tdID) {
		return this.taskDefinitions.stream()
				.filter(td -> td.getId().equals(tdID))
				.findAny()
				.orElse(null);
	}

	@Override
	public List<TaskDefinition> getWorkflowTaskDefinitions() {		
		return taskDefinitions;
	}

	@Override
	public List<DecisionNodeDefinition> getDecisionNodeDefinitions() {
		return dnds;
	}

}
