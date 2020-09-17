package impactassessment.passiveprocessengine.definition;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import impactassessment.passiveprocessengine.instance.WorkflowTask;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import impactassessment.neo4j.ArtifactTypeConverter;
import impactassessment.passiveprocessengine.definition.TaskLifecycle.InputState;
import impactassessment.passiveprocessengine.definition.TaskLifecycle.OutputState;


public class TaskDefinition extends AbstractWorkflowDefinitionObject implements java.io.Serializable, ITaskDefinition {
	
	private static final long serialVersionUID = 2899700748101656836L;
	@Convert(ArtifactTypeConverter.Input.class)
	private Map<String, ArtifactType> expectedInput = new HashMap<String,ArtifactType>(); // later we can enhance on complexity by wrapping artifact type with conditions etc
	@Convert(ArtifactTypeConverter.Output.class)
	private Map<String,ArtifactType> expectedOutput = new HashMap<String,ArtifactType>();
	@Transient // for now
	private Role responsibleRole;
	
	private transient ICustomRoleSelector roleSelector;
	
	public TaskDefinition(String definitionId, WorkflowDefinition wfd, ICustomRoleSelector roleSelector) {
		super(definitionId, wfd);
		this.roleSelector = roleSelector;		
	}
	
	public void setRoleSelector(ICustomRoleSelector roleSelector) {
		this.roleSelector = roleSelector;
	}

	@Deprecated // needed only for neo4j persistence mechanism requires non-arg constructor 
	public TaskDefinition() {
		super();
	}
	
	public TaskDefinition(String definitionId, WorkflowDefinition wfd) {		
		super(definitionId, wfd);
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

	public void setResponsibleRole(Role responsibleRole) {
		this.responsibleRole = responsibleRole;
	}

	public Role getResponsibleRole(WorkflowTask wt) {
		if (responsibleRole != null)
			return responsibleRole;
		if (roleSelector != null) 
			return roleSelector.getRoleForTaskState(wt, this);			
		else 
			return null;
	}
	
	public InputState calcInputState(WorkflowTask wt) {
		// default implementation: for every expected input, is there such an artifact
		if (getExpectedInput().isEmpty())
			return InputState.INPUT_SUFFICIENT;
		int count = getExpectedInput().values().stream()			
			.map(it -> { return (wt.getAnyOneInputByType(it.getArtifactType()) != null) ? 1 : 0; })
			.filter( c -> c > 0)
			.mapToInt(c -> c)
			.sum();
		if (count == 0)
			return InputState.INPUT_MISSING;
		if (count == getExpectedInput().size())
			return InputState.INPUT_SUFFICIENT;
		else
			return InputState.INPUT_PARTIAL;
	}

	public OutputState calcOutputState(WorkflowTask wt) {
		// default implementation: for every expected output, is there such an artifact
		if (getExpectedOutput().isEmpty())
			return OutputState.OUTPUT_SUFFICIENT;
		int count = getExpectedOutput().values().stream()			
			.map(it -> { return (wt.getAnyOneOutputByType(it.getArtifactType()) != null) ?  1 : 0; })
			.filter( c -> c > 0)
			.mapToInt(c -> (int)c)
			.sum();
		if (count == 0)
			return OutputState.OUTPUT_MISSING;
		if (count == getExpectedOutput().size())
			return OutputState.OUTPUT_SUFFICIENT;
		else
			return OutputState.OUTPUT_PARTIAL;
	}
	
	public Set<Map.Entry<String, ArtifactType>> getMissingOutput(WorkflowTask wt) {
		return getExpectedOutput().entrySet().stream()
			.filter(tuple -> !wt.hasOutputArtifactOfRole(tuple.getKey()))
			.collect(Collectors.toSet());
	}
	
	public Set<Map.Entry<String, ArtifactType>> getMissingInput(WorkflowTask wt) {
		return getExpectedInput().entrySet().stream()
			.filter(tuple -> !wt.hasInputArtifactOfRole(tuple.getKey()))
			.collect(Collectors.toSet());
	}

	@Override
	public int hashCode() {
		return Objects.hash(expectedInput, expectedOutput, responsibleRole);
	}
}
