package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.TaskStateTransitionEvent;

public interface TaskStateTransitionEventPublisher {

	public void publishEvent(TaskStateTransitionEvent event);
	
}
