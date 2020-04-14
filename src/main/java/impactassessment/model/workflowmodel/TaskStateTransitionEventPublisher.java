package impactassessment.model.workflowmodel;

public interface TaskStateTransitionEventPublisher {

	public void publishEvent(TaskStateTransitionEvent event);
	
}
