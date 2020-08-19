package impactassessment.passiveprocessengine.workflowmodel;

public interface TaskStateTransitionEventPublisher {

	public void publishEvent(TaskStateTransitionEvent event);
	
}
