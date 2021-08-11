package impactassessment.query;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Events.*;
import impactassessment.api.Queries.*;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
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
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.*;

import java.util.*;

import static impactassessment.query.WorkflowHelpers.*;

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

	private boolean updateFrontend = true;

	// Event Handlers

	@EventHandler
	public void on(CreatedWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {} in {}", evt, status.toString());
		KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
		WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		kieSessions.create(evt.getId(), kieContainer);
		if (!status.isReplay()) {
//			kieSessions.setInitialized(evt.getId());
//			awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
//			kieSessions.insertOrUpdate(evt.getId(), wfiWrapper.getWorkflowInstance());
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}

	}

	@EventHandler
	public void on(CreatedSubWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
		WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		kieSessions.create(evt.getId(), kieContainer);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}



	@EventHandler
	public void on(AddedConstraintsEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
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

	@EventHandler
	public void on(AddedEvaluationResultToConstraintEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
//			awos.forEach(awo -> {
//				kieSessions.insertOrUpdate(evt.getId(), awo);
//				
//			});
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);			
		}
	}

	@DisallowReplay
	@EventHandler
	public void on(UpdatedEvaluationTimeEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		projection.handle(evt);
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}

	@DisallowReplay
	@EventHandler
	public void on(CheckedConstraintEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		if (wfiWrapper != null) {
			wfiWrapper.getRebc(evt.getConstrId()).ifPresentOrElse(rebc -> {
				ensureInitializedKB(kieSessions, projection, evt.getId());
				insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), rebc.getConstraintType(), "CheckedConstraintEvt");
				kieSessions.fire(evt.getId());
			}, () -> log.warn("Concerned RuleEngineBasedConstraint wasn't found"));
		} else {
			log.warn("WFI not initialized");
		}
	}

	@DisallowReplay
	@EventHandler
	public void on(CheckedAllConstraintsEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		if (wfiWrapper != null) {
			ensureInitializedKB(kieSessions, projection, evt.getId());
			insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "CheckedAllConstraintsEvt");
			kieSessions.fire(evt.getId());
		}
	}

	@EventHandler
	public void on(AddedInputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {} in {}", evt, status.toString());
		//artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			addToSubWorkflow(commandGateway, wft, evt.getRole(), evt.getArtifact().getType()); //TODO: move this into initUpdate... method that checks for every change if there was output removed or added to a task or the process itself
//			ensureInitializedKB(kieSessions, projection, evt.getId());
//			kieSessions.insertOrUpdate(evt.getId(), wft);			
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, wios, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedInputEvt"));
		}
	}

	@EventHandler
	public void on(AddedOutputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
//			ensureInitializedKB(kieSessions, projection, evt.getId());
//			wios.forEach(wio -> kieSessions.insertOrUpdate(evt.getId(), wio));
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, wios, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedOutputEvt"));
		}

	}
	
	@EventHandler
	public void on(SetPostConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
//			ensureInitializedKB(kieSessions, projection, evt.getId());
//			wios.forEach(wio -> kieSessions.insertOrUpdate(evt.getId(), wio));
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireAndUITrigger(evt, wfiWrapper, wios);
		}

	}
	
	@EventHandler
	public void on(SetPreConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
//			ensureInitializedKB(kieSessions, projection, evt.getId());
//			wios.forEach(wio -> kieSessions.insertOrUpdate(evt.getId(), wio));
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireAndUITrigger(evt, wfiWrapper, wios);
		}

	}

	@EventHandler
	public void on(AddedInputToWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		// artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		// if this input is an IArtifact, insert it into kieSession
		if (!status.isReplay()) {
			insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedInputToWorkflowEvt");
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}

	@EventHandler
	public void on(AddedOutputToWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}

	@DisallowReplay
	@EventHandler
	public void on(UpdatedArtifactsEvt evt) {
		log.debug("[PRJ] projecting {}", evt); 
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		ensureInitializedKB(kieSessions, projection, evt.getId());
		// Is artifact used as Input/Output to workflow? --> update workflow, update in kieSession
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		awos.stream().map(WorkflowChangeEvent::getChangedObject).distinct().forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
		// insert Event as trigger for rule to react upon
		kieSessions.insertOrUpdate(evt.getId(), evt);
		insertConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "UpdatedArtifactsEvt");
		kieSessions.fire(evt.getId());
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

	@EventHandler
	public void on(InstantiatedTaskEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos= wfiWrapper.handle(evt);
		if (!status.isReplay() && awos.size() > 0) {
//			ensureInitializedKB(kieSessions, projection, evt.getId());
//			awos.forEach(x -> kieSessions.insertOrUpdate(evt.getId(), x));
//			kieSessions.fire(evt.getId());
//			if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}

	@EventHandler
	public void on(RemovedInputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}

	@EventHandler
	public void on(RemovedOutputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}
	}

	@EventHandler
	public void on(IdentifiableEvt evt) {
		log.debug("[PRJ] projecting {}", evt);
		projection.handle(evt);
	}

	private void initUpdateFireConstraintInsertAndUITrigger(IdentifiableEvt evt, WorkflowInstanceWrapper wfiWrapper, List<WorkflowChangeEvent> events, ConstraintTrigger ct) {
		ensureInitializedKB(kieSessions, projection, evt.getId());
		events.stream().map(WorkflowChangeEvent::getChangedObject).distinct().forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));		
		
		// now handling subworkflow aspects here:
		// creation of new subworkflows
		events.stream()
		.filter(cevt -> cevt.getChangeType().equals(WorkflowChangeEvent.ChangeType.CREATED))
		.map(WorkflowChangeEvent::getChangedObject)
		.distinct()
		.filter(WorkflowWrapperTaskInstance.class::isInstance)
		.map(WorkflowWrapperTaskInstance.class::cast)
		.forEach(awo -> createSubWorkflow(commandGateway, awo, wfiWrapper.getWorkflowInstance().getId()));	
		
		if (events.size() > 0)
			if (ct != null)
				kieSessions.insertOrUpdate(evt.getId(), ct);
			kieSessions.fire(evt.getId());
		if (updateFrontend) projection.getWfi(evt.getId()).ifPresent(pusher::update);
	}
	
	private void initUpdateFireAndUITrigger(IdentifiableEvt evt, WorkflowInstanceWrapper wfiWrapper, List<WorkflowChangeEvent> events) {		
		initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, events, null);
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
