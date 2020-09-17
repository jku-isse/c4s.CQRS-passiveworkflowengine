package impactassessment.passiveprocessengine.definition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import impactassessment.neo4j.ArtifactTypeConverter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.google.inject.Inject;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@NodeEntity
public abstract class AbstractWorkflowDefinition extends AbstractWorkflowDefinitionObject implements WorkflowDefinition {

	@Relationship("TASK_DEFINITION")
	protected List<TaskDefinition> taskDefinitions = new ArrayList<TaskDefinition>();
	@Relationship("DECISIONNODE_DEFINITION")
	protected List<DecisionNodeDefinition> dnds = new ArrayList<DecisionNodeDefinition>();
	
	@Inject
	protected transient TaskStateTransitionEventPublisher pub;

	@Convert(ArtifactTypeConverter.Input.class)
	private Map<String, ArtifactType> expectedInput = new HashMap<>();
	@Convert(ArtifactTypeConverter.Output.class)
	private Map<String,ArtifactType> expectedOutput = new HashMap<>();
	
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

	@Override
	public Map<String,ArtifactType> getExpectedInput() {
		return expectedInput;
	}

	@Override
	public ArtifactType putExpectedInput(String key, ArtifactType value) {
		return expectedInput.put(key, value);
	}

	@Override
	public Map<String,ArtifactType> getExpectedOutput() {
		return expectedOutput;
	}

	@Override
	public ArtifactType putExpectedOutput(String key, ArtifactType value) {
		return expectedOutput.put(key, value);
	}

}
