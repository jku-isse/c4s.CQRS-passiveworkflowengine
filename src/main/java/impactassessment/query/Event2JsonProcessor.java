package impactassessment.query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import impactassessment.api.Events.IdentifiableEvt;
import impactassessment.api.Events.TimedEvt;
import passiveprocessengine.definition.AbstractArtifact;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.IWorkflowInstanceObject;
import passiveprocessengine.instance.WorkflowChangeEvent;
import passiveprocessengine.persistance.json.WorkflowObjectSerializer;

public class Event2JsonProcessor implements ChangeEventProcessor{

	protected Gson gson;
	protected IHistoryLogEventLogger logger;
	
	public Event2JsonProcessor(IHistoryLogEventLogger logger) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(IWorkflowInstanceObject.class, WorkflowObjectSerializer.getWFIOSerializer());
		gsonBuilder.registerTypeAdapter(AbstractArtifact.class, WorkflowObjectSerializer.getAASerializer());
		gsonBuilder.registerTypeAdapter(IWorkflowTask.class, WorkflowObjectSerializer.getWFTSerializer());
		gson = gsonBuilder.create();
		this.logger = logger;
	}
	
	
	@Override
	public void processChangeImpact(TimedEvt evt, List<WorkflowChangeEvent> flatListOfEffects, OffsetDateTime occurredOn) {
		// serialize each event to json structure and then log
		AtomicInteger order = new AtomicInteger(0);	
		logger.log(flatListOfEffects.stream()
		.map(event -> new HistoryLogEntry(evt.getId(), occurredOn.toString() , evt.getClass().getSimpleName(), event.getParentCause(), event, order.getAndAdd(1)))
		.map(entry -> gson.toJson(entry))
		.collect(Collectors.toList()));
	}

	public static class HistoryLogEntry {
		public String processId;
		public String timestampOfRootCauseEvent;
		public String rootCauseEventType;
		public String parentCauseRef;
		public WorkflowChangeEvent effect;
		public int order;
		
		public HistoryLogEntry(String processId, String timestampOfRootCauseEvent, String rootCauseEventType, WorkflowChangeEvent parentCause,
				WorkflowChangeEvent effect, int order) {
			super();
			this.processId = processId;
			this.timestampOfRootCauseEvent = timestampOfRootCauseEvent;
			this.rootCauseEventType = rootCauseEventType;
			this.parentCauseRef = parentCause != null ? parentCause.getId() : null;
			this.effect = effect;
			this.order = order;
		}
		
		
	}
}
