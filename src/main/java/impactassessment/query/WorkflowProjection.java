package impactassessment.query;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.kiesession.KieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.passiveprocessengine.instance.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("query")
@ProcessingGroup("projection")
public class WorkflowProjection {

    private final ProjectionModel projection;

    // Event Handlers

    @EventHandler
    public void on(ImportedOrUpdatedArtifactEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        ensureInitializedKB(evt.getId(), kieSession);
        WorkflowInstanceWrapper wfiWrapper = projection.getOrCreateWorkflowModel(evt.getId());
        wfiWrapper.setArtifact(evt.getArtifact());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            kieSession.insertOrUpdate(evt.getId(), evt.getArtifact());
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        ensureInitializedKB(evt.getId(), kieSession);
        WorkflowInstanceWrapper wfiWrapper = projection.getOrCreateWorkflowModel(evt.getId());
        wfiWrapper.setArtifact(evt.getArtifact());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            kieSession.insertOrUpdate(evt.getId(), evt.getArtifact());
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(CreatedChildWorkflowEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        ensureInitializedKB(evt.getId(), kieSession);
        WorkflowInstanceWrapper wfiWrapper = projection.getOrCreateWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(CompletedDataflowEvt evt, ReplayStatus status, KieSessionService kieSession, CommandGateway commandGateway) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        Map<WorkflowTask, ArtifactInput> mappedInputs = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            System.out.println("#################### size: "+mappedInputs.size());
            mappedInputs.entrySet().stream().forEach(e -> System.out.println("#################### WFT: "+e.getKey().getId()+" AI: "+e.getValue().toString()));
            mappedInputs.entrySet().stream().forEach(entry -> addToSubWorkflow(entry.getKey(), entry.getValue(), commandGateway));
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(ActivatedInBranchEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(ActivatedOutBranchEvt evt, ReplayStatus status, KieSessionService kieSession, CommandGateway commandGateway) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
            // TODO here
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance(), commandGateway);
        }
    }

    @EventHandler
    public void on(ActivatedInOutBranchEvt evt, ReplayStatus status, KieSessionService kieSession, CommandGateway commandGateway) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
            // TODO here
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance(), commandGateway);
        }
    }

    @EventHandler
    public void on(ActivatedInOutBranchesEvt evt, ReplayStatus status, KieSessionService kieSession, CommandGateway commandGateway) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSession.insertOrUpdate(evt.getId(), awo));
            kieSession.fire(evt.getId());
            // TODO here
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance(), commandGateway);
        }
    }

    @EventHandler
    public void on(AddedConstraintsEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<RuleEngineBasedConstraint> rebcs = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            rebcs.forEach(rebc -> {
                kieSession.insertOrUpdate(evt.getId(), rebc);
                ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(rebc.getId(), "AddConstraintCmd"));
                ct.addConstraint(rebc.getConstraintType());
                kieSession.insertOrUpdate(evt.getId(), ct);
            });
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedEvaluationResultToConstraintEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        RuleEngineBasedConstraint updatedRebc = wfiWrapper.handle(evt);
        if (!status.isReplay() && updatedRebc != null) {
            kieSession.insertOrUpdate(evt.getId(), updatedRebc);
            kieSession.fire(evt.getId());
        }
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedConstraintEvt evt, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            RuleEngineBasedConstraint rebc = wfiWrapper.getQAC(evt.getCorrId());
            if (rebc != null) {
                ensureInitializedKB(evt.getId(), kieSession);
                ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getCorrId(), "CheckConstraintCmd"));
                ct.addConstraint(rebc.getConstraintType());
                kieSession.insertOrUpdate(evt.getId(), ct);
                kieSession.fire(evt.getId());
            } else {
                log.warn("Concerned RuleEngineBasedConstraint wasn't found");
            }
        }
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedAllConstraintsEvt evt, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            ensureInitializedKB(evt.getId(), kieSession);
            ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getId(), "CheckAllConstraintsCmd"));
            ct.addConstraint("*");
            kieSession.insertOrUpdate(evt.getId(), ct);
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedAsInputEvt evt, ReplayStatus status, KieSessionService kieSession, CommandGateway commandGateway) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        WorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(evt.getId(), kieSession);
            kieSession.insertOrUpdate(evt.getId(), wft);
            kieSession.fire(evt.getId());
            // TODO
            addToSubWorkflow(wft, new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType()), commandGateway);
        }
    }

    @EventHandler
    public void on(AddedAsOutputEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        WorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(evt.getId(), kieSession);
            kieSession.insertOrUpdate(evt.getId(), wft);
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedAsInputToWfiEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        if (!status.isReplay()) {
            kieSession.insertOrUpdate(evt.getId(), evt.getInput().getArtifact()); // TODO IJiraArtifact is not supported!
            kieSession.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(DeletedEvt evt, ReplayStatus status, KieSessionService kieSession) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        if (!status.isReplay()) {
            kieSession.dispose(evt.getId());
        }
    }

    @EventHandler
    public void on(IdentifiableEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
    }

    // Query Handlers

    @QueryHandler
    public GetStateResponse handle(GetStateQuery query) {
        log.debug("[PRJ] handle {}", query);
        return new GetStateResponse(new ArrayList<>(projection.getDb().values()));
    }

    @QueryHandler
    public PrintKBResponse handle(PrintKBQuery query, KieSessionService kieSession) {
        log.debug("[PRJ] handle {}", query);
        StringBuilder s = new StringBuilder();
        if (kieSession.getKieSession(query.getId()) != null) {
            s.append("\n############## KB CONTENT ################\n");
            kieSession.getKieSession(query.getId()).getObjects().stream()
                    .forEach(o -> s.append(o.toString() + "\n"));
            s.append("####### SIZE: " + kieSession.getKieSession(query.getId()).getObjects().size() +
                    " ######### "+ kieSession.getNumKieSessions()+" #######");
            log.info(s.toString());
        }
        return new PrintKBResponse(s.toString());
    }

    // Reset Handler

    @ResetHandler
    public void reset() {
        log.debug("[PRJ] reset view db");
        projection.reset();
    }

    // Helper Methods

    /**
     * Needed for user commands, because then the KB must be checked if it is initialized.
     * First call in such command handlers!
     *
     * @param id
     * @param kieSessionService
     */
    private void ensureInitializedKB(String id, KieSessionService kieSessionService) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessionService.isInitialized(id) && wfiWrapper != null && wfiWrapper.getArtifact() != null) {
            IJiraArtifact artifact = wfiWrapper.getArtifact();
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            kieSessionService.insertOrUpdate(id, artifact);
            wfiWrapper.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> {
                        kieSessionService.insertOrUpdate(id, wft);
                        QACheckDocument doc = wfiWrapper.getQACDocOfWft(wft.getTaskId());
                        if (doc != null) {
                            kieSessionService.insertOrUpdate(id, doc);
                            doc.getConstraintsReadonly().stream()
                                    .filter(q -> q instanceof RuleEngineBasedConstraint)
                                    .map(q -> (RuleEngineBasedConstraint) q)
                                    .forEach(rebc -> kieSessionService.insertOrUpdate(id, rebc));
                        }
                    });
            wfiWrapper.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> kieSessionService.insertOrUpdate(id, dni));
            kieSessionService.setInitialized(id);
        }
    }

    private void createSubWorkflow(List<AbstractWorkflowInstanceObject> awos, WorkflowInstance wfi,  CommandGateway commandGateway) {
        awos.stream()
                .filter(awo -> awo instanceof WorkflowWrapperTaskInstance)
                .map(awo -> (WorkflowWrapperTaskInstance) awo)
                .forEach(wwti -> commandGateway.send(new CreateChildWorkflowCmd(wwti.getSubWorkflowId(), wfi.getId(), wwti.getId(), wwti.getSubWfd())));
    }

    private void addToSubWorkflow(WorkflowTask wft, ArtifactInput ai, CommandGateway commandGateway) {
        if (wft instanceof WorkflowWrapperTaskInstance) {
            WorkflowWrapperTaskInstance wwti = (WorkflowWrapperTaskInstance) wft;
            commandGateway.send(new AddAsInputToWfiCmd(wwti.getSubWorkflowId(), ai));
        }
    }
}
