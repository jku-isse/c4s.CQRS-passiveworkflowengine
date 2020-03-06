package counter.workflowmodel;

public interface TaskStateTransitionEventPublisher {

	public void publishEvent(TaskStateTransitionEvent event);
	
}
