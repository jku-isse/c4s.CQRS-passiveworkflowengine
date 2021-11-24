package impactassessment.query;

import static passiveprocessengine.definition.TaskLifecycle.State.ACTIVE;
import static passiveprocessengine.definition.TaskLifecycle.State.AVAILABLE;
import static passiveprocessengine.definition.TaskLifecycle.State.CANCELED;
import static passiveprocessengine.definition.TaskLifecycle.State.ENABLED;
import static passiveprocessengine.definition.TaskLifecycle.State.NO_WORK_EXPECTED;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import artifactapi.IArtifactRegistry;
import impactassessment.api.Events.IdentifiableEvt;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import lombok.RequiredArgsConstructor;
import passiveprocessengine.definition.AbstractArtifact;
import passiveprocessengine.definition.DecisionNodeDefinition;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.definition.TaskLifecycle.State;
import passiveprocessengine.instance.IWorkflowInstanceObject;
import passiveprocessengine.instance.WorkflowChangeEvent;
import passiveprocessengine.instance.events.AugmentedTaskEvent;
import passiveprocessengine.instance.events.StateTransitionEvent;
import passiveprocessengine.instance.events.AugmentedTaskEvent.AugmentedTaskEventType;
import passiveprocessengine.persistance.json.WorkflowObjectSerializer;

@Component
public class EventList2Logger {

	protected Gson gson;
	protected IHistoryLogEventLogger logger;
	
	public EventList2Logger(IHistoryLogEventLogger logger) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(IWorkflowInstanceObject.class, WorkflowObjectSerializer.getWFIOSerializer());
		gsonBuilder.registerTypeAdapter(AbstractArtifact.class, WorkflowObjectSerializer.getAASerializer());
		gsonBuilder.registerTypeAdapter(IWorkflowTask.class, WorkflowObjectSerializer.getWFTSerializer());
		gson = gsonBuilder.create();
		this.logger = logger;
	}
	
	public void transformAndLogEventImpact(IdentifiableEvt evt, List<WorkflowChangeEvent> events, OffsetDateTime zdt) {
		// serialize each event to json structure and then log
		AtomicInteger order = new AtomicInteger(0);
		events.stream()
		.map(event -> augmentEvent(event))
		.map(event -> new HistoryLogEntry(evt.getId(), zdt.toString() , evt.getClass().getSimpleName(), event, order.getAndAdd(1)))
		.map(entry -> gson.toJson(entry))
		.forEach(str -> logger.log(str));
		// TODO later add more root cause event details
	}
	
	HashMap<String, Boolean> step2prematureFlag = new HashMap<>();
	
	protected WorkflowChangeEvent augmentEvent(WorkflowChangeEvent wce) {
		if (wce.getChangeType().equals(WorkflowChangeEvent.ChangeType.ACTUAL_STATE_CHANGED)) {
			StateTransitionEvent event = (StateTransitionEvent)wce;
			if (event.getFromState().equals(AVAILABLE) && !event.getToState().equals(ENABLED) && !event.getToState().equals(CANCELED) && !event.getToState().equals(NO_WORK_EXPECTED) ) { // a premature step
				// we should also check if this was not due to a race condition
				return event.getTask().getWorkflow().getDecisionNodeInstancesReadonly().stream() 
				.filter(dni -> dni.getDefinition().equals(event.getTask().getType().getInDND()) )							
				.findAny().map(dni -> { // there must be exactly one
					if (!dni.getState().equals(DecisionNodeDefinition.States.PASSED_OUTBRANCH_CONDITIONS)) {
						step2prematureFlag.put(event.getTask().getId(), true);
						return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.PREMATURE_START);
					} else
						return wce;
				}).get();						
			}
			if (event.getToState().equals(ACTIVE) && 
					!(event.getFromState().equals(AVAILABLE) || event.getFromState().equals(ENABLED))) { // a reactivated/repeated step
				return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.REACTIVATED);
				
			}
			if (event.getToState().equals(AVAILABLE)) { // a revoked step as any new step automatically goes into AVAILABLE without an event, thus this event here means the task was already progressed
				return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.REVOKED);
			}
		} else if (wce.getChangeType().equals(WorkflowChangeEvent.ChangeType.EXPECTED_STATE_CHANGED)) {
			StateTransitionEvent event = (StateTransitionEvent)wce;
			if (event.getFromState().equals(AVAILABLE) && event.getToState().equals(ENABLED) && step2prematureFlag.getOrDefault(event.getTask().getId(), false) == true) {
				// AND WE NEED TO CHECK if preceeding tasks are fulfilled, as the precondition of this step is ignorant of the other QA conditions and just might trigger this transition anyway
				return event.getTask().getWorkflow().getDecisionNodeInstancesReadonly().stream() 
					.filter(dni -> dni.getDefinition().equals(event.getTask().getType().getInDND()) )							
					.findAny().map(dni -> { // there must be exactly one
						if (dni.getState().equals(DecisionNodeDefinition.States.PASSED_OUTBRANCH_CONDITIONS)) {
							step2prematureFlag.remove(event.getTask().getId());
							return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.FIXED_PREMATURE_START); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
						} else
							return wce;
					}).get();
			}
		} 
		return wce;
	}
	
	
	public static class HistoryLogEntry {
		public String processId;
		public String timestampOfRootCauseEvent;
		public String rootCauseEventType;
		public WorkflowChangeEvent effect;
		public int order;
		
		public HistoryLogEntry(String processId, String timestampOfRootCauseEvent, String rootCauseEventType,
				WorkflowChangeEvent effect, int order) {
			super();
			this.processId = processId;
			this.timestampOfRootCauseEvent = timestampOfRootCauseEvent;
			this.rootCauseEventType = rootCauseEventType;
			this.effect = effect;
			this.order = order;
		}
		
		
	}
}
