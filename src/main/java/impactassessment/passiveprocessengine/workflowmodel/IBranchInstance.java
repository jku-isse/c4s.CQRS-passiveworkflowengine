package impactassessment.passiveprocessengine.workflowmodel;

public interface IBranchInstance {

	public static enum BranchState {
		Waiting, // Waiting for preconditions to be fulfilled
		TransitionEnabled, // Preconditions fullfilled, transition not yet used: i.e., for out branches task at end not activated, for inbranches: DN has not propagated controlflow
		TransitionPassed, // transition now used,
		Disabled // alternative branch used that disallowed this one, e.g., part of XOR
	}

	WorkflowTask getTask();

	void setTask(WorkflowTask task);

	IBranchDefinition getBranchDefinition();

	BranchState getState();

	void setConditionsFulfilled();

	void setBranchUsedForProgress();

	void setConditionsNoLongerHold();

	void setBranchNotAllowed();

	boolean hasTask();

	void setDecisionNodeInstance(DecisionNodeInstance dni);

}