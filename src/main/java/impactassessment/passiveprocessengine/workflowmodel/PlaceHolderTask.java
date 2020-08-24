package impactassessment.passiveprocessengine.workflowmodel;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import impactassessment.passiveprocessengine.workflowmodel.TaskLifecycle.Events;
import impactassessment.passiveprocessengine.workflowmodel.TaskLifecycle.InputState;
import impactassessment.passiveprocessengine.workflowmodel.TaskLifecycle.OutputState;
import impactassessment.passiveprocessengine.workflowmodel.TaskLifecycle.State;
import impactassessment.passiveprocessengine.actor.Participant;

public class PlaceHolderTask extends WorkflowTask{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Deprecated
	public PlaceHolderTask() {
		super();
	}
	
	public PlaceHolderTask(WorkflowInstance wfi, TaskDefinition td, String placeHolderId) {
		super();
		super.id = placeHolderId;
		super.setTaskType(td);
		super.setWorkflow(wfi);
	}
	
	@Override
	public State getLifecycleState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public boolean isInSuperEndState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public void setTaskType(TaskDefinition taskType) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Participant getResponsibleEngineer() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public void setResponsibleEngineer(Participant responsibleEngineer) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public List<ArtifactOutput> getOutput() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public void addOutput(ArtifactOutput ao) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public List<ArtifactInput> getInput() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public void addInput(ArtifactInput ai) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Artifact removeInputArtifactById(String artifactId) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public boolean hasInputArtifactWithId(String artifactId) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public boolean hasOutputArtifactOfRole(String outputRole) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public boolean hasOutputArtifactWithId(String artifactId) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public int countOutputOfType(String artifactType) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public int countInputOfType(String artifactType) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public boolean hasInputArtifactOfRole(String inputRole) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Artifact getAnyOneInputByType(String artifactType) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Artifact getAnyOneInputByRole(String inputRole) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Set<Entry<String, ArtifactType>> getMissingInput() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");	
		}

	@Override
	public Artifact getAnyOneOutputByType(String artifactType) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Artifact getAnyOneOutputByRole(String outputRole) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public Set<Entry<String, ArtifactType>> getMissingOutput() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public OutputState getOutputState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public InputState getInputState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public InputState recalcInputState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public OutputState recalcOutputState() {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public void signalEvent(Events event) {
		throw new RuntimeException("PlaceHolderTask invocation disallowed");
	}

	@Override
	public String toString() {
		return "PlaceholderTask";
	}
	
	
}
