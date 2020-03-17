package impactassessment.workflowmodel;

public interface TaskStateTransitionEventPublisher {

	public void publishEvent(TaskStateTransitionEvent event);
	
}
