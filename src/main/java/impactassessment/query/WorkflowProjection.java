package impactassessment.query;

import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands;
import impactassessment.api.Events.*;
import impactassessment.api.Queries.*;
import impactassessment.command.CollectingGatewayProxy;
import impactassessment.command.IGatewayProxy;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.passiveprocessengine.RootWorkflowChangeEvent;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import io.atlassian.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import passiveprocessengine.instance.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static impactassessment.query.WorkflowHelpers.*;
import static passiveprocessengine.instance.WorkflowChangeEvent.ChangeType.*;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class WorkflowProjection {

	private final ProjectionModel projection;
	private final IKieSessionService kieSessions;
	private final CommandGateway commandGateway;
	private final WorkflowDefinitionRegistry registry;
	private final IFrontendPusher pusher;
	private final IArtifactRegistry artifactRegistry;
	private final EventList2Forwarder el2l;
	private transient IGatewayProxy gw = null;
	
	private boolean updateFrontend = true;

	// Event Handlers

	
	@EventHandler 
	public void on(CompositeEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {} in {}", evt, status.toString());
		
		List<List<WorkflowChangeEvent>> listOfListEvents = evt.getEventList()
				.stream()
				.peek(subevt -> log.debug("[PRJ] projecting subevent {}", subevt))
				.map(subevt -> dispatch(subevt)).collect(Collectors.toList());
		initUpdateFireConstraintsAndUITriggerForMultiEvents(evt, listOfListEvents);
	}
	
	protected List<WorkflowChangeEvent> dispatch(TimedEvt evt) {
		if (evt instanceof CreatedWorkflowEvt)
			return executeEvent((CreatedWorkflowEvt) evt);
		if (evt instanceof CreatedSubWorkflowEvt)
			return executeEvent((CreatedSubWorkflowEvt) evt);
		if (evt instanceof AddedConstraintsEvt)
			return executeEvent((AddedConstraintsEvt)evt);
		if (evt instanceof AddedEvaluationResultToConstraintEvt)
			return executeEvent((AddedEvaluationResultToConstraintEvt)evt);
		if (evt instanceof UpdatedEvaluationTimeEvt)
			return executeEvent((UpdatedEvaluationTimeEvt)evt);
		if (evt instanceof AddedInputEvt)
			return executeEvent((AddedInputEvt)evt);
		if (evt instanceof AddedOutputEvt)
			return executeEvent((AddedOutputEvt)evt);
		if (evt instanceof SetPostConditionsFulfillmentEvt)
			return executeEvent((SetPostConditionsFulfillmentEvt)evt);
		if (evt instanceof SetPreConditionsFulfillmentEvt)
			return executeEvent((SetPreConditionsFulfillmentEvt)evt);
		if (evt instanceof AddedInputToWorkflowEvt)
			return executeEvent((AddedInputToWorkflowEvt)evt);
		if (evt instanceof AddedOutputToWorkflowEvt)
			return executeEvent((AddedOutputToWorkflowEvt)evt);
		if (evt instanceof UpdatedArtifactsEvt)
			return executeEvent((UpdatedArtifactsEvt)evt);
		if (evt instanceof DeletedEvt)
			return executeEvent((DeletedEvt)evt);
		if (evt instanceof InstantiatedTaskEvt)
			return executeEvent((InstantiatedTaskEvt)evt);
		if (evt instanceof RemovedInputEvt)
			return executeEvent((RemovedInputEvt)evt);
		if (evt instanceof RemovedOutputEvt)
			return executeEvent((RemovedOutputEvt)evt);
		if (evt instanceof ActivatedTaskEvt)
			return executeEvent((ActivatedTaskEvt)evt);
		else {
			log.warn("Unsupported Event in composite event found: "+evt.getClass().getSimpleName());
			return Collections.emptyList();
		}
	}
	
	@EventHandler
	public void on(CreatedWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {} in {}", evt, status.toString());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, awos);
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(CreatedWorkflowEvt evt) {
		KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
		WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		gw = kieSessions.create(evt.getId(), kieContainer);
		return awos;
	}

	@EventHandler
	public void on(CreatedSubWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, awos);
		}
	}

	protected List<WorkflowChangeEvent> executeEvent(CreatedSubWorkflowEvt evt) {
		KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
		WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		wfiWrapper.setParentWfiId(evt.getParentWfiId());
		wfiWrapper.setParentWftId(evt.getParentWftId());
		gw = kieSessions.create(evt.getId(), kieContainer);
		return awos;
	}

	@EventHandler
	public void on(AddedConstraintsEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		
		if (!status.isReplay()) {
			awos.stream().map(WorkflowChangeEvent::getChangedObject).distinct().forEach(awo -> {
				kieSessions.insertOrUpdate(evt.getId(), awo);
				if (awo instanceof RuleEngineBasedConstraint) {
					ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(awo.getId(), "AddConstraintCmd"));
					ct.addConstraint(((RuleEngineBasedConstraint)awo).getConstraintType());
					kieSessions.insertOrUpdate(evt.getId(), ct);
				}
			});
			if (awos.size() > 0) { //there was some impact, thus eval constraints
				kieSessions.fire(evt.getId());
			}
		}
	}

	protected List<WorkflowChangeEvent> executeEvent(AddedConstraintsEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
		//TODO: we somehow need to ensure these added constraints are also triggered later when rules fire
	}
	
	@EventHandler
	public void on(AddedEvaluationResultToConstraintEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, awos);			
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(AddedEvaluationResultToConstraintEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(ActivatedTaskEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}
	
	protected List<WorkflowChangeEvent> executeEvent(ActivatedTaskEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		wfiWrapper.handle(evt);
		return Collections.emptyList();
	}
	
	@DisallowReplay
	@EventHandler
	public void on(UpdatedEvaluationTimeEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}
	
	protected List<WorkflowChangeEvent> executeEvent(UpdatedEvaluationTimeEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		wfiWrapper.handle(evt);
		return Collections.emptyList();
	}

	@DisallowReplay
	@EventHandler
	public void on(CheckedConstraintEvt evt) { // comes from Web Frontend only, never from RuleEngine (should not ;-)
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		if (wfiWrapper != null) {
			wfiWrapper.getRebc(evt.getConstrId()).ifPresentOrElse(rebc -> {
				ensureInitializedKB(kieSessions, projection, evt.getId());
				insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), rebc.getConstraintType(), "CheckedConstraintEvt");
				kieSessions.fire(evt.getId());
				if (gw instanceof CollectingGatewayProxy) { //FIXME soooo ugly
					((CollectingGatewayProxy) gw).sendAllAsCompositeCommand(); //otherwise those commands wont be dispatched
				}
			}, () -> log.warn("Concerned RuleEngineBasedConstraint wasn't found"));
		} else {
			log.warn("WFI not initialized");
		}
	}

	@DisallowReplay
	@EventHandler
	public void on(CheckedAllConstraintsEvt evt) { // comes from Web Frontend only, never from RuleEngine (should not ;-)
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		if (wfiWrapper != null) {
			ensureInitializedKB(kieSessions, projection, evt.getId());
			insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "CheckedAllConstraintsEvt");
			kieSessions.fire(evt.getId());
			if (gw instanceof CollectingGatewayProxy) { //FIXME soooo ugly
				((CollectingGatewayProxy) gw).sendAllAsCompositeCommand(); //otherwise those commands wont be dispatched
			}
		}
	}

	@EventHandler
	public void on(AddedInputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {} in {}", evt, status.toString());
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) { 
			initUpdateFireConstraintInsertAndUITrigger(evt,  awos, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedInputEvt"));
		}
	}

	protected List<WorkflowChangeEvent> executeEvent(AddedInputEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}
	
	@EventHandler
	public void on(AddedOutputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireConstraintInsertAndUITrigger(evt, awos, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedOutputEvt"));
		}

	}
	
	protected List<WorkflowChangeEvent> executeEvent(AddedOutputEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}
	
	@EventHandler
	public void on(SetPostConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> wios =  executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt,  wios);
		}

	}
	
	protected List<WorkflowChangeEvent> executeEvent(SetPostConditionsFulfillmentEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}
	
	@EventHandler
	public void on(SetPreConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> wios = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt,  wios);
		}

	}
	
	protected List<WorkflowChangeEvent> executeEvent(SetPreConditionsFulfillmentEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(AddedInputToWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		// artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		// if this input is an IArtifact, insert it into kieSession
		if (!status.isReplay()) {
//			insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedInputToWorkflowEvt");//should not be necessary as input if needed is mapped to steps and these then cause triggering
			initUpdateFireAndUITrigger(evt,  awos);
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(AddedInputToWorkflowEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(AddedOutputToWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, awos);
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(AddedOutputToWorkflowEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@DisallowReplay
	@EventHandler
	public void on(UpdatedArtifactsEvt evt) {
		log.debug("[PRJ] projecting {}", evt); 
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		initUpdateFireConstraintInsertAndUITrigger(
				evt,
				awos,
				createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "UpdatedArtifactsEvt")
		);
	}
	
	protected List<WorkflowChangeEvent> executeEvent(UpdatedArtifactsEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(DeletedEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		projection.handle(evt);
		if (!status.isReplay()) {
			kieSessions.dispose(evt.getId());
			artifactRegistry.deleteAllDataScopes(evt.getId());
			if (updateFrontend) pusher.remove(evt.getId());
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(DeletedEvt evt) {
		projection.handle(evt);
		return Collections.emptyList();
	}

	@EventHandler
	public void on(InstantiatedTaskEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		
		if (!status.isReplay() && awos.size() > 0) {
			ConstraintTrigger ct = null;
			if (evt.getOptionalOutputs().size() > 0) 
				ct = createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "InstantiatedTaskEvt");
			initUpdateFireConstraintInsertAndUITrigger(evt, awos, ct);
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(InstantiatedTaskEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(RemovedInputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			// shouldnt this also trigger eval of constraints?! as adding input does
			initUpdateFireAndUITrigger(evt, awos);
		}
	}
	
	protected List<WorkflowChangeEvent> executeEvent(RemovedInputEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}

	@EventHandler
	public void on(RemovedOutputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		
		List<WorkflowChangeEvent> awos = executeEvent(evt);
		if (!status.isReplay()) {
			// shouldnt this also trigger eval of constraints?! as adding input does
			initUpdateFireAndUITrigger(evt, awos);
		}
	}

	protected List<WorkflowChangeEvent> executeEvent(RemovedOutputEvt evt) {
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		return wfiWrapper.handle(evt);
	}
	
	@EventHandler
	public void on(IdentifiableEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		projection.handle(evt);
	}

	private void initUpdateFireConstraintsAndUITriggerForMultiEvents(CompositeEvt evt, List<List<WorkflowChangeEvent>> listofEventLists) {
		// we have a lot of changes from multiple commands, now we update kieSession, and then call fire once.
		
		ensureInitializedKB(kieSessions, projection, evt.getId());
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		
		listofEventLists.stream()
		.flatMap(list -> list.stream())
		.flatMap(event -> event.getFlatCascade())
		.map(WorkflowChangeEvent::getChangedObject).distinct().forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));		
	
		
		listofEventLists.stream().forEach(eventList -> 
			dispatchToChildAndParentWorkflows(eventList, wfiWrapper));
		
		listofEventLists
			.stream()
			.filter(list -> list.size() > 0)
			.forEach(list -> {
				OffsetDateTime ts = evt.getTimestamp() != null ? evt.getTimestamp() : OffsetDateTime.now();
				el2l.transformAndLogEventImpact(evt, list, ts);
			});
		
		// when do we want to insert a ConstraintCheckTrigger: 
		// --> whenever there was an artifact changed, added, or removed as part of the commands/events
		AtomicInteger countArtEvents = new AtomicInteger(0);
		evt.getEventList().forEach(event -> {
			if (event instanceof AddedOutputEvt || event instanceof AddedInputEvt 
					|| event instanceof RemovedInputEvt || event instanceof RemovedOutputEvt
					|| event instanceof UpdatedArtifactsEvt || event instanceof AddedOutputToWorkflowEvt 
					|| event instanceof AddedInputToWorkflowEvt || event instanceof InstantiatedTaskEvt) {
				countArtEvents.addAndGet(1);
			}
		});
		if (countArtEvents.get() > 0) {
			insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "CompositeEvt");
		}
		// if there was a constraint trigger inserted, or if there are some changes in the processinstance:
		if (countArtEvents.get() > 0 || listofEventLists.stream().flatMap(list -> list.stream()).count() > 0) {
			kieSessions.insertOrUpdate(evt.getId(), evt);
			WorkflowChangeEvent root = new RootWorkflowChangeEvent(evt.getParentCauseRef() != null ? evt.getParentCauseRef() : "", wfiWrapper.getWorkflowInstance());
			kieSessions.insertOrUpdate(evt.getId(), root);
			gw.setRootCause(root);
			kieSessions.fire(evt.getId());
			// now get the collected commands and dispatch them
			if (gw instanceof CollectingGatewayProxy) { //FIXME soooo ugly
				((CollectingGatewayProxy) gw).sendAllAsCompositeCommand(); //otherwise those commands wont be dispatched
			}
		}
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}
	
	
	private void dispatchToChildAndParentWorkflows(List<WorkflowChangeEvent> events, WorkflowInstanceWrapper wfiWrapper) {
		// now handling subworkflow aspects here:
				// creation of new subworkflows
				events.stream()
					.flatMap(event -> event.getFlatCascade())
					.filter(cevt -> cevt.getChangeType().equals(CREATED))
					.filter(cevt -> cevt.getChangedObject() instanceof WorkflowWrapperTaskInstance)
					.distinct()
					.forEach(cevt -> createSubWorkflow(commandGateway, (WorkflowWrapperTaskInstance)cevt.getChangedObject(), wfiWrapper.getWorkflowInstance().getId(), cevt.getId()));

				// adding inputs to subworkflow
				events.stream()
						.flatMap(event -> event.getFlatCascade())
						.filter(cevt -> cevt.getChangeType().equals(NEW_INPUT))
						.filter(cevt -> cevt.getChangedObject() instanceof WorkflowWrapperTaskInstance)
						.distinct()
						// all inputs are sent to the subworkflow (addInput of AbstractWorkflowTask adds only those that are not yet listed)
						.forEach(cevt -> cevt.getAffectedTask().getInput().forEach(ai -> ai.getArtifacts().forEach(art -> commandGateway.send(
								new Commands.AddInputToWorkflowCmd(
									((WorkflowWrapperTaskInstance) cevt.getAffectedTask()).getSubWfiId(),
									art.getArtifactIdentifier().getId(),
									ai.getRole(),
									art.getArtifactIdentifier().getType()).setParentCauseRef(cevt.getParentCause().getId())))));

				// addoutputs to parentworkflow
				events.stream()
						.flatMap(event -> event.getFlatCascade())
						.filter(cevt -> cevt.getChangeType().equals(NEW_OUTPUT))
						.filter(cevt -> cevt.getChangedObject() instanceof WorkflowWrapperTaskInstance)
						.distinct()
						// all inputs are sent to the subworkflow (addInput of AbstractWorkflowTask adds only those that are not yet listed)
						.forEach(cevt -> cevt.getAffectedTask().getOutput().forEach(ao -> ao.getArtifacts().forEach(art -> {
							if (wfiWrapper.getParentWfiId() != null && wfiWrapper.getParentWftId() != null) {
								commandGateway.send(
										new Commands.AddOutputCmd(
												wfiWrapper.getParentWfiId(),
												wfiWrapper.getParentWftId(),
												art.getArtifactIdentifier().getId(),
												ao.getRole(),
												art.getArtifactIdentifier().getType()).setParentCauseRef(cevt.getParentCause().getId()));
							}
						})));
	}
	
	
	private void initUpdateFireConstraintInsertAndUITrigger(TimedEvt evt, List<WorkflowChangeEvent> events, ConstraintTrigger ct) {
		ensureInitializedKB(kieSessions, projection, evt.getId());
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		events.stream()
			.flatMap(event -> event.getFlatCascade())
			.map(WorkflowChangeEvent::getChangedObject).distinct().forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));		
		
		dispatchToChildAndParentWorkflows(events, wfiWrapper);

		if (events.size() > 0) { // we need to log first, to maintain correct order in logs
			// log events
			// log process id, event type, and consequence:
			OffsetDateTime ts = evt.getTimestamp() != null ? evt.getTimestamp() : OffsetDateTime.now();
			el2l.transformAndLogEventImpact(evt, events, ts);
		}
		
		//if (events.size() > 0) // even without changes in the process, the conditions might now fire
		if  (ct != null) {
			kieSessions.insertOrUpdate(evt.getId(), ct);
		}
		if (ct != null || events.size() > 0) {
			kieSessions.insertOrUpdate(evt.getId(), evt);
			WorkflowChangeEvent root = new RootWorkflowChangeEvent(evt.getParentCauseRef() != null ? evt.getParentCauseRef() : "", wfiWrapper.getWorkflowInstance());
			kieSessions.insertOrUpdate(evt.getId(), root);
			gw.setRootCause(root);
			kieSessions.fire(evt.getId());
			// now get the collected commands and dispatch them
			if (gw instanceof CollectingGatewayProxy) { //FIXME soooo ugly
				((CollectingGatewayProxy) gw).sendAllAsCompositeCommand(); //otherwise those commands wont be dispatched
			}
		}
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}
	
	
	
	private void initUpdateFireAndUITrigger(TimedEvt evt, List<WorkflowChangeEvent> events) {		
		initUpdateFireConstraintInsertAndUITrigger(evt, events, null);
	}
	
	
	// Query Handlers

	@QueryHandler
	public GetStateResponse handle(GetStateQuery query) {
		log.debug("[PRJ] handle {}", query);
		if (projection.getDb().size() == 0) return new GetStateResponse(Collections.emptyList());
		if (query.getId().equals("*")) {
			return new GetStateResponse(projection.getWfis());
		} else {
			log.warn("GetStateQuery with id-parameter other that '*' is not supported! (id was: {})", query.getId());
			return new GetStateResponse(Collections.emptyList());
		}

	}

	@QueryHandler
	public PrintKBResponse handle(PrintKBQuery query) {
		log.debug("[PRJ] handle {}", query);
		StringBuilder s = new StringBuilder();
		if (kieSessions.getKieSession(query.getId()) != null) {
			s.append("\n############## KB CONTENT ################\n");
			kieSessions.getKieSession(query.getId()).getObjects()
			.forEach(o -> s.append(o.toString()).append("\n"));
			s.append("####### SIZE: ")
			.append(kieSessions.getKieSession(query.getId()).getObjects().size())
			.append(" ######### ")
			.append(kieSessions.getNumKieSessions())
			.append(" #######");
			log.debug(s.toString());
		}
		return new PrintKBResponse(s.toString());
	}

	// Reset Handler

	@ResetHandler
	public void reset() {
		log.debug("[PRJ] reset view db");
		projection.reset();
	}
	
	private void insertConstraintTrigger(String id, WorkflowInstance wfi, String constraintType, String correlationObjectType) {
		ConstraintTrigger ct = new ConstraintTrigger(wfi, new CorrelationTuple(id, correlationObjectType));
		ct.addConstraint(constraintType);
		kieSessions.insertOrUpdate(id, ct);
	}
	
	private ConstraintTrigger createConstraintTrigger(String id, WorkflowInstance wfi, String constraintType, String correlationObjectType) {
		ConstraintTrigger ct = new ConstraintTrigger(wfi, new CorrelationTuple(id, correlationObjectType));
		ct.addConstraint(constraintType);
		return ct;
	}
}
