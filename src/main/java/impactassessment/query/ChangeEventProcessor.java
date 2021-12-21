package impactassessment.query;

import java.time.OffsetDateTime;
import java.util.List;

import impactassessment.api.Events.IdentifiableEvt;
import impactassessment.api.Events.TimedEvt;
import passiveprocessengine.instance.WorkflowChangeEvent;

public interface ChangeEventProcessor {

	
	void processChangeImpact(TimedEvt evt, List<WorkflowChangeEvent> flatListOfEffects, OffsetDateTime occurredOn);
}
