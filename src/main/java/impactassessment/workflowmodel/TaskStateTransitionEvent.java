package impactassessment.workflowmodel;

import impactassessment.workflowmodel.TaskLifecycle.State;

public class TaskStateTransitionEvent {

	public TaskLifecycle.State fromState;
	public TaskLifecycle.State toState;
	public transient WorkflowTask task;
	
	public TaskStateTransitionEvent(State fromState, State toState, WorkflowTask task) {
		super();
		this.fromState = fromState;
		this.toState = toState;
		this.task = task;
	}	
		
	public void setFromState(TaskLifecycle.State fromState) {
		this.fromState = fromState;
	}

	public void setToState(TaskLifecycle.State toState) {
		this.toState = toState;
	}

	public TaskLifecycle.State getFromState() {
		return fromState;
	}
	public TaskLifecycle.State getToState() {
		return toState;
	}
	public WorkflowTask getTask() {
		return task;
	}
	
	
}
