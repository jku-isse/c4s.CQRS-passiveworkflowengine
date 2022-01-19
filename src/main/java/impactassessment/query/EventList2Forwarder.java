package impactassessment.query;

import static passiveprocessengine.definition.TaskLifecycle.State.ACTIVE;
import static passiveprocessengine.definition.TaskLifecycle.State.AVAILABLE;
import static passiveprocessengine.definition.TaskLifecycle.State.CANCELED;
import static passiveprocessengine.definition.TaskLifecycle.State.ENABLED;
import static passiveprocessengine.definition.TaskLifecycle.State.NO_WORK_EXPECTED;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import impactassessment.api.Events.IdentifiableEvt;
import impactassessment.api.Events.TimedEvt;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.DecisionNodeDefinition;
import passiveprocessengine.instance.WorkflowChangeEvent;
import passiveprocessengine.instance.WorkflowInstance;
import passiveprocessengine.instance.events.StateTransitionEvent;
import passiveprocessengine.instance.events.TaskTransitionEventAugmentation;
import passiveprocessengine.instance.events.TaskTransitionEventAugmentation.AugmentedTaskTransitionEventType;

@Slf4j
@Component
public class EventList2Forwarder {

	LinkedHashSet<ChangeEventProcessor> processors = new LinkedHashSet<>();
	
	public EventList2Forwarder() {
	}
	
	public boolean registerProcessor(ChangeEventProcessor cep) {
		log.info("Registering "+cep.getClass().getSimpleName());
		return processors.add(cep);
	}
	
	public boolean unregisterProcessor(ChangeEventProcessor cep) {
		log.info("Unregistering "+cep.getClass().getSimpleName());
		return processors.remove(cep);
	}
	
	public void transformAndLogEventImpact(TimedEvt evt, List<WorkflowChangeEvent> events, OffsetDateTime occurredOn) {
		List<WorkflowChangeEvent> augmentedList = events.stream()
				.flatMap(event -> event.getFlatCascade())
				.map(event -> augmentEvent(event))
				.collect(Collectors.toList());
		processors.stream().forEach(p -> p.processChangeImpact(evt, augmentedList, occurredOn));
	}
	
	HashMap<String, Boolean> step2prematureFlag = new HashMap<>();
	
	protected WorkflowChangeEvent augmentEvent(WorkflowChangeEvent wce) {
		if (wce.getChangeType().equals(WorkflowChangeEvent.ChangeType.ACTUAL_STATE_CHANGED)) {
			StateTransitionEvent event = (StateTransitionEvent)wce;
			if (event.getFromState().equals(AVAILABLE) && !event.getToState().equals(ENABLED) && !event.getToState().equals(CANCELED) && !event.getToState().equals(NO_WORK_EXPECTED) ) { // a premature step
				// we should also check if this was not due to a race condition
				if (!(event.getTask() instanceof WorkflowInstance)) {
					event.getTask().getWorkflow().getDecisionNodeInstancesReadonly().stream() 
					.filter(dni -> dni.getDefinition().equals(event.getTask().getType().getInDND()) )							
					.findAny().ifPresent(dni -> { // there must be exactly one
						if (!dni.getState().equals(DecisionNodeDefinition.States.PASSED_OUTBRANCH_CONDITIONS)) {
							step2prematureFlag.put(event.getTask().getId(), true);
							wce.getAugmentations().put(TaskTransitionEventAugmentation.class.getSimpleName(), new TaskTransitionEventAugmentation(AugmentedTaskTransitionEventType.PREMATURE_START));
							//return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.PREMATURE_START);
						} 
					});	
				}
			}
			if (event.getToState().equals(ACTIVE) && 
					!(event.getFromState().equals(AVAILABLE) || event.getFromState().equals(ENABLED))) { // a reactivated/repeated step
				wce.getAugmentations().put(TaskTransitionEventAugmentation.class.getSimpleName(), new TaskTransitionEventAugmentation(AugmentedTaskTransitionEventType.REACTIVATED));
				//return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.REACTIVATED);
				
			}
			if (event.getToState().equals(AVAILABLE)) { // a revoked step as any new step automatically goes into AVAILABLE without an event, thus this event here means the task was already progressed
				wce.getAugmentations().put(TaskTransitionEventAugmentation.class.getSimpleName(), new TaskTransitionEventAugmentation(AugmentedTaskTransitionEventType.REVOKED));
				//return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.REVOKED);
			}
		} else if (wce.getChangeType().equals(WorkflowChangeEvent.ChangeType.EXPECTED_STATE_CHANGED)) {
			StateTransitionEvent event = (StateTransitionEvent)wce;
			if (event.getFromState().equals(AVAILABLE) && event.getToState().equals(ENABLED) && step2prematureFlag.getOrDefault(event.getTask().getId(), false) == true) {
				// AND WE NEED TO CHECK if preceeding tasks are fulfilled, as the precondition of this step is ignorant of the other QA conditions and just might trigger this transition anyway
				if (!(event.getTask() instanceof WorkflowInstance)) {
					event.getTask().getWorkflow().getDecisionNodeInstancesReadonly().stream() 
					.filter(dni -> dni.getDefinition().equals(event.getTask().getType().getInDND()) )							
					.findAny().ifPresent(dni -> { // there must be exactly one
						if (dni.getState().equals(DecisionNodeDefinition.States.PASSED_OUTBRANCH_CONDITIONS)) {
							step2prematureFlag.remove(event.getTask().getId());
							wce.getAugmentations().put(TaskTransitionEventAugmentation.class.getSimpleName(), new TaskTransitionEventAugmentation(AugmentedTaskTransitionEventType.FIXED_PREMATURE_START));
							//return AugmentedTaskEvent.fromStateTransitionEvent(event, AugmentedTaskEventType.FIXED_PREMATURE_START); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
						} 
					});
				}
			}
		} 
		return wce;
	}
	
	
	
}
