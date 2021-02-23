package impactassessment.query;

import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
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
import passiveprocessengine.definition.ArtifactType;
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

    // Event Handlers

    @EventHandler
    public void on(CreatedWorkflowEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        evt.getArtifacts().forEach(e -> artifactRegistry.injectArtifactService(e.getValue(), evt.getId()));
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        kieSessions.create(evt.getId(), kieContainer);
        if (!status.isReplay()) {
            kieSessions.setInitialized(evt.getId());
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            evt.getArtifacts().forEach(art -> kieSessions.insertOrUpdate(evt.getId(), art.getValue()));
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(CreatedSubWorkflowEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        evt.getArtifacts().forEach(e -> artifactRegistry.injectArtifactService(e.getValue(), evt.getId()));
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        kieSessions.create(evt.getId(), kieContainer);
        if (!status.isReplay()) {
            kieSessions.setInitialized(evt.getId());
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            evt.getArtifacts().forEach(art -> kieSessions.insertOrUpdate(evt.getId(), art.getValue()));
            kieSessions.fire(evt.getId());
        }
    }

//    @EventHandler
//    public void on(CompletedDataflowEvt evt, ReplayStatus status) {
//        log.debug("[PRJ] projecting {}", evt);
//        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
//        Map<IWorkflowTask, ArtifactInput> mappedInputs = wfiWrapper.handle(evt);
//        if (!status.isReplay()) {
//            mappedInputs.forEach((key, value) -> log.debug("MappedInputs: WFT=" + key.getId() + " AI=" + value.toString()));
//            mappedInputs.forEach((wft, ai) -> addToSubWorkflow(commandGateway, wft, ai));
//        }
//    }

//    @EventHandler
//    public void on(ActivatedInBranchEvt evt, ReplayStatus status) {
//        log.debug("[PRJ] projecting {}", evt);
//        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
//        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
//        if (!status.isReplay()) {
//            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
//            kieSessions.fire(evt.getId());
//        }
//    }

//    @EventHandler
//    public void on(ActivatedOutBranchEvt evt, ReplayStatus status) {
//        log.debug("[PRJ] projecting {}", evt);
//        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
//        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
//        if (!status.isReplay()) {
//            awos.forEach(awo -> {
//                kieSessions.insertOrUpdate(evt.getId(), awo);
//                createSubWorkflow(commandGateway, (WorkflowWrapperTaskInstance)awo, wfiWrapper.getWorkflowInstance().getId());
//            });
//            kieSessions.fire(evt.getId());
//        }
//    }

//    @EventHandler
//    public void on(ActivatedInOutBranchEvt evt, ReplayStatus status) {
//        log.debug("[PRJ] projecting {}", evt);
//        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
//        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
//        if (!status.isReplay()) {
//            awos.forEach(awo -> {
//                kieSessions.insertOrUpdate(evt.getId(), awo);
//                createSubWorkflow(commandGateway, (WorkflowWrapperTaskInstance)awo, wfiWrapper.getWorkflowInstance().getId());
//            });
//            kieSessions.fire(evt.getId());
//        }
//    }

//    @EventHandler
//    public void on(ActivatedInOutBranchesEvt evt, ReplayStatus status) {
//        log.debug("[PRJ] projecting {}", evt);
//        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
//        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
//        if (!status.isReplay()) {
//            awos.forEach(awo -> {
//                kieSessions.insertOrUpdate(evt.getId(), awo);
//                createSubWorkflow(commandGateway, (WorkflowWrapperTaskInstance)awo, wfiWrapper.getWorkflowInstance().getId());
//            });
//            kieSessions.fire(evt.getId());
//        }
//    }

    @EventHandler
    public void on(AddedConstraintsEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> {
                kieSessions.insertOrUpdate(evt.getId(), awo);
                if (awo instanceof RuleEngineBasedConstraint) {
                    ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(awo.getId(), "AddConstraintCmd"));
                    ct.addConstraint(((RuleEngineBasedConstraint)awo).getConstraintType());
                    kieSessions.insertOrUpdate(evt.getId(), ct);
                }
            });
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedEvaluationResultToConstraintEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        Set<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> {
                kieSessions.insertOrUpdate(evt.getId(), awo);
                if (awo instanceof WorkflowWrapperTaskInstance) {
                    createSubWorkflow(commandGateway, (WorkflowWrapperTaskInstance)awo, wfiWrapper.getWorkflowInstance().getId());
                }
            });
            kieSessions.fire(evt.getId());
        }
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedConstraintEvt evt) {
        log.debug("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            RuleEngineBasedConstraint rebc = wfiWrapper.getRebc(evt.getCorrId());
            if (rebc != null) {
                ensureInitializedKB(kieSessions, projection, evt.getId());
                ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getCorrId(), "CheckConstraintCmd"));
                ct.addConstraint(rebc.getConstraintType());
                kieSessions.insertOrUpdate(evt.getId(), ct);
                kieSessions.fire(evt.getId());
            } else {
                log.warn("Concerned RuleEngineBasedConstraint wasn't found");
            }
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
            ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getId(), "CheckAllConstraintsCmd"));
            ct.addConstraint("*");
            kieSessions.insertOrUpdate(evt.getId(), ct);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedInputEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        IWorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(kieSessions, projection, evt.getId());
            kieSessions.insertOrUpdate(evt.getId(), wft);
            kieSessions.fire(evt.getId());
            addToSubWorkflow(commandGateway, wft, evt.getRole(), evt.getType());
        }
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @EventHandler
    public void on(AddedOutputEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<IWorkflowInstanceObject> wios = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(kieSessions, projection, evt.getId());
            wios.forEach(wio -> kieSessions.insertOrUpdate(evt.getId(), wio));
            kieSessions.fire(evt.getId());
        }
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @EventHandler
    public void on(AddedInputToWorkflowEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        wfiWrapper.handle(evt);
        // if this input is an IArtifact, insert it into kieSession
        if (!status.isReplay()) {
            wfiWrapper.getWorkflowInstance().getInput().stream()
                    .filter(o -> o.getRole().equals(evt.getRole()))
                    .filter(o -> o.getArtifactType().getArtifactType().equals(evt.getType()))
                    .findAny()
                    .ifPresent(o -> {
                        kieSessions.remove(evt.getId(), evt.getArtifact()); // TODO what if the artifact is used twice in the workflow?
                    });
            kieSessions.insertOrUpdate(evt.getId(), evt.getArtifact());
            kieSessions.fire(evt.getId());
        }
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @EventHandler
    public void on(AddedOutputToWorkflowEvt evt) {
        log.debug("[PRJ] projecting {}", evt);
        artifactRegistry.injectArtifactService(evt.getArtifact(), evt.getId());
        projection.handle(evt);
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @EventHandler
    public void on(UpdatedArtifactsEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (!status.isReplay()) {
            evt.getArtifacts().forEach(e -> artifactRegistry.injectArtifactService(e, evt.getId()));
            ensureInitializedKB(kieSessions, projection, evt.getId());
        }
        // Is artifact used as Input/Output to workflow? --> update workflow, update in kieSession
        for (IArtifact updatedArtifact : evt.getArtifacts()) {
         //FIXME  xxx: //ONly input, why not output????
        	for (ArtifactInput input : wfiWrapper.getWorkflowInstance().getInput()) {
                IArtifact presentArtifact = checkIfIArtifactInside(input.getArtifact());
                if (presentArtifact != null && presentArtifact.getArtifactIdentifier().getId().equals(updatedArtifact.getArtifactIdentifier().getId())) {	
                	((ArtifactWrapper) input.getArtifact()).updateWrappedArtifact(updatedArtifact);
                	if (!status.isReplay()) {
                    	kieSessions.insertOrUpdate(evt.getId(), updatedArtifact);
                    }
                }
            }
        }
        // TODO: Is artifact used as Input/Output of a WFT --> update WFT, update WFT in kieSession

        // CheckAllConstraints
        if (!status.isReplay()) {
           // ensureInitializedKB(kieSessions, projection, evt.getId());
            ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getId(), "CheckAllConstraintsCmd"));
            ct.addConstraint("*");
            kieSessions.insertOrUpdate(evt.getId(), ct);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(DeletedEvt evt, ReplayStatus status) {
        log.debug("[PRJ] projecting {}", evt);
        projection.handle(evt);
        pusher.update(new ArrayList<>(projection.getDb().values()));
        if (!status.isReplay()) {
            kieSessions.dispose(evt.getId());
        }
    }

    @EventHandler
    public void on(IdentifiableEvt evt) {
        log.debug("[PRJ] projecting {}", evt);
        projection.handle(evt);
    }

    // Query Handlers

    @QueryHandler
    public GetStateResponse handle(GetStateQuery query) {
        log.debug("[PRJ] handle {}", query);
        if (projection.getDb().size() == 0) return new GetStateResponse(Collections.emptyList());
        return new GetStateResponse(new ArrayList<>(projection.getDb().values()));
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

}
