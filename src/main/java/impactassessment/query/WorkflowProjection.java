package impactassessment.query;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands;
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
import passiveprocessengine.instance.WorkflowChangeEvent.ChangeType;

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
			initUpdateFireAndUITrigger(evt, wfiWrapper, awos);
		}

	}

	@EventHandler
	public void on(CreatedSubWorkflowEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
		WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		wfiWrapper.setParentWfiId(evt.getParentWfiId());
		wfiWrapper.setParentWftId(evt.getParentWftId());
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
			initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, wios, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedInputEvt"));
		}
	}

	@EventHandler
	public void on(AddedOutputEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, wios, createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "AddedOutputEvt"));
		}

	}
	
	@EventHandler
	public void on(SetPostConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
			initUpdateFireAndUITrigger(evt, wfiWrapper, wios);
		}

	}
	
	@EventHandler
	public void on(SetPreConditionsFulfillmentEvt evt, ReplayStatus status) {
		log.debug("[PRJ] projecting {}", evt);
		WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
		List<WorkflowChangeEvent> wios = wfiWrapper.handle(evt);
		if (!status.isReplay()) {
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
		List<WorkflowChangeEvent> awos = wfiWrapper.handle(evt);
		// we need to add the update artifact as well to have it replaced in the rule bases
		if (awos.size() > 0) { // there was some change, thus artifact is relevant

		}
		initUpdateFireConstraintInsertAndUITrigger(
				evt,
				wfiWrapper,
				awos,
				createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "UpdatedArtifactsEvt")
		);
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
		ConstraintTrigger ct = null;
		if (evt.getOptionalOutputs().size() > 0) 
			ct = createConstraintTrigger(evt.getId(), wfiWrapper.getWorkflowInstance(), "*", "InstantiatedTaskEvt");
		if (!status.isReplay() && awos.size() > 0) {
			initUpdateFireConstraintInsertAndUITrigger(evt, wfiWrapper, awos, ct);
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
			.filter(cevt -> cevt.getChangeType().equals(CREATED))
			.map(WorkflowChangeEvent::getChangedObject)
			.distinct()
			.filter(WorkflowWrapperTaskInstance.class::isInstance)
			.map(WorkflowWrapperTaskInstance.class::cast)
			.forEach(awo -> createSubWorkflow(commandGateway, awo, wfiWrapper.getWorkflowInstance().getId()));

		// adding inputs to subworkflow
		events.stream()
				.filter(cevt -> cevt.getChangeType().equals(NEW_INPUT))
				.map(WorkflowChangeEvent::getChangedObject)
				.distinct()
				.filter(WorkflowWrapperTaskInstance.class::isInstance)
				.map(WorkflowWrapperTaskInstance.class::cast)
				// all inputs are sent to the subworkflow (addInput of AbstractWorkflowTask adds only those that are not yet listed)
				.forEach(wwti -> wwti.getInput().forEach(ai -> ai.getArtifacts().forEach(art -> commandGateway.send(
						new Commands.AddInputToWorkflowCmd(
							wwti.getSubWfiId(),
							art.getArtifactIdentifier().getId(),
							ai.getRole(),
							art.getArtifactIdentifier().getType())))));

		// addoutputs to parentworkflow
		events.stream()
				.filter(cevt -> cevt.getChangeType().equals(NEW_OUTPUT))
				.map(WorkflowChangeEvent::getChangedObject)
				.distinct()
				.filter(WorkflowInstance.class::isInstance)
				.map(WorkflowInstance.class::cast)
				.forEach(wfi -> wfi.getOutput().forEach(ao -> ao.getArtifacts().forEach(art -> {
					if (projection.getWorkflowModel(evt.getId()).getParentWfiId() != null && projection.getWorkflowModel(evt.getId()).getParentWftId() != null) {
						commandGateway.send(
								new Commands.AddOutputCmd(
										projection.getWorkflowModel(evt.getId()).getParentWfiId(),
										projection.getWorkflowModel(evt.getId()).getParentWftId(),
										art.getArtifactIdentifier().getId(),
										ao.getRole(),
										art.getArtifactIdentifier().getType()));
					}
				})));

		//if (events.size() > 0) // even without changes in the process, the conditions might now fire
		if  (ct != null) {
			kieSessions.insertOrUpdate(evt.getId(), ct);
		}
		if (ct != null || events.size() > 0) {
			kieSessions.insertOrUpdate(evt.getId(), evt);
			kieSessions.fire(evt.getId());
		}
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
