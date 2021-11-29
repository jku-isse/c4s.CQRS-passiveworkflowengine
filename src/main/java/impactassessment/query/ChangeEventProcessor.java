package impactassessment.query;

import java.time.OffsetDateTime;
import java.util.List;

import impactassessment.api.Events.IdentifiableEvt;
import passiveprocessengine.instance.WorkflowChangeEvent;

public interface ChangeEventProcessor {

	
	void processChangeImpact(IdentifiableEvt evt, List<WorkflowChangeEvent> effect, OffsetDateTime occurredOn);
}
