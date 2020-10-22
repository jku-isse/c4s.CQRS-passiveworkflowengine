package impactassessment.query;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.kiesession.KieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.passiveprocessengine.definition.IWorkflowTask;
import impactassessment.passiveprocessengine.instance.*;
import impactassessment.registry.WorkflowDefinitionRegistry;
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
    private final KieSessionService kieSessions;
    private final CommandGateway commandGateway;
    private final WorkflowDefinitionRegistry registry;

    // Event Handlers

    @EventHandler
    public void on(ImportedOrUpdatedArtifactEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        createKieSession(evt.getId(), null);
        ensureInitializedKB(evt.getId());
        if (!projection.contains(evt.getId())) {
            WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
            List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
            wfiWrapper.setArtifact(evt.getArtifacts());
            if (!status.isReplay()) {
                for (IJiraArtifact a : evt.getArtifacts()) {
                    kieSessions.insertOrUpdate(evt.getId(), a);
                }
                awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
                kieSessions.fire(evt.getId());
            }
        } else {
            projection.getWorkflowModel(evt.getId()).setArtifact(evt.getArtifacts());
            if (!status.isReplay()) {
                for (IJiraArtifact a : evt.getArtifacts()) {
                    kieSessions.insertOrUpdate(evt.getId(), a);
                }
                kieSessions.fire(evt.getId());
            }
        }
    }

    @EventHandler
    public void on(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        createKieSession(evt.getId(), kieContainer);
        ensureInitializedKB(evt.getId());
        if (!projection.contains(evt.getId())) {
            WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
            List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
            wfiWrapper.setArtifact(evt.getArtifacts());
            if (!status.isReplay()) {
                for (IJiraArtifact a : evt.getArtifacts()) {
                    kieSessions.insertOrUpdate(evt.getId(), a);
                }
                awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
                kieSessions.fire(evt.getId());
            }
        } else {
            projection.getWorkflowModel(evt.getId()).setArtifact(evt.getArtifacts());
            if (!status.isReplay()) {
                for (IJiraArtifact a : evt.getArtifacts()) {
                    kieSessions.insertOrUpdate(evt.getId(), a);
                }
                kieSessions.fire(evt.getId());
            }
        }
    }

    @EventHandler
    public void on(CreatedChildWorkflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        createKieSession(evt.getId(), kieContainer);
        WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(CompletedDataflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        Map<IWorkflowTask, ArtifactInput> mappedInputs = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            System.out.println("#################### size: "+mappedInputs.size());
            mappedInputs.entrySet().stream().forEach(e -> System.out.println("#################### WFT: "+e.getKey().getId()+" AI: "+e.getValue().toString()));
            mappedInputs.entrySet().stream().forEach(entry -> addToSubWorkflow(entry.getKey(), entry.getValue()));
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(ActivatedInBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(ActivatedOutBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            kieSessions.fire(evt.getId());
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance().getId());
        }
    }

    @EventHandler
    public void on(ActivatedInOutBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            kieSessions.fire(evt.getId());
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance().getId());
        }
    }

    @EventHandler
    public void on(ActivatedInOutBranchesEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            awos.forEach(awo -> kieSessions.insertOrUpdate(evt.getId(), awo));
            kieSessions.fire(evt.getId());
            createSubWorkflow(awos, wfiWrapper.getWorkflowInstance().getId());
        }
    }

    @EventHandler
    public void on(AddedConstraintsEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<RuleEngineBasedConstraint> rebcs = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            rebcs.forEach(rebc -> {
                kieSessions.insertOrUpdate(evt.getId(), rebc);
                ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(rebc.getId(), "AddConstraintCmd"));
                ct.addConstraint(rebc.getConstraintType());
                kieSessions.insertOrUpdate(evt.getId(), ct);
            });
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedEvaluationResultToConstraintEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        RuleEngineBasedConstraint updatedRebc = wfiWrapper.handle(evt);
        if (!status.isReplay() && updatedRebc != null) {
            kieSessions.insertOrUpdate(evt.getId(), updatedRebc);
            kieSessions.fire(evt.getId());
        }
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedConstraintEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            RuleEngineBasedConstraint rebc = wfiWrapper.getQAC(evt.getCorrId());
            if (rebc != null) {
                ensureInitializedKB(evt.getId());
                ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getCorrId(), "CheckConstraintCmd"));
                ct.addConstraint(rebc.getConstraintType());
                kieSessions.insertOrUpdate(evt.getId(), ct);
                kieSessions.fire(evt.getId());
            } else {
                log.warn("Concerned RuleEngineBasedConstraint wasn't found");
            }
        }
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedAllConstraintsEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            ensureInitializedKB(evt.getId());
            ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getId(), "CheckAllConstraintsCmd"));
            ct.addConstraint("*");
            kieSessions.insertOrUpdate(evt.getId(), ct);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedAsInputEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        IWorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(evt.getId());
            kieSessions.insertOrUpdate(evt.getId(), wft);
            kieSessions.fire(evt.getId());
            addToSubWorkflow(wft, new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType()));
        }
    }

    @EventHandler
    public void on(AddedAsOutputEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        WorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(evt.getId());
            kieSessions.insertOrUpdate(evt.getId(), wft);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedAsInputToWfiEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        if (!status.isReplay()) {
            kieSessions.insertOrUpdate(evt.getId(), evt.getInput().getArtifact());
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(DeletedEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        if (!status.isReplay()) {
            kieSessions.dispose(evt.getId());
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
    public PrintKBResponse handle(PrintKBQuery query) {
        log.debug("[PRJ] handle {}", query);
        StringBuilder s = new StringBuilder();
        if (kieSessions.getKieSession(query.getId()) != null) {
            s.append("\n############## KB CONTENT ################\n");
            kieSessions.getKieSession(query.getId()).getObjects().stream()
                    .forEach(o -> s.append(o.toString() + "\n"));
            s.append("####### SIZE: " + kieSessions.getKieSession(query.getId()).getObjects().size() +
                    " ######### "+ kieSessions.getNumKieSessions()+" #######");
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
    private void createKieSession(String id, KieContainer kieContainer) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessions.isInitialized(id) && wfiWrapper == null) { // if artifact is only updated wfiWrapper won't be null anymore
            kieSessions.create(id, kieContainer);
        }
    }

    private void ensureInitializedKB(String id) {
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(id);
        if (!kieSessions.isInitialized(id) && wfiWrapper != null) {
            List<IJiraArtifact> artifacts = wfiWrapper.getArtifacts();
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            for (IJiraArtifact artifact : artifacts) {
                kieSessions.insertOrUpdate(id, artifact);
            }
            wfiWrapper.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> {
                        kieSessions.insertOrUpdate(id, wft);
                        QACheckDocument doc = wfiWrapper.getQACDocOfWft(wft.getTaskId());
                        if (doc != null) {
                            kieSessions.insertOrUpdate(id, doc);
                            doc.getConstraintsReadonly().stream()
                                    .filter(q -> q instanceof RuleEngineBasedConstraint)
                                    .map(q -> (RuleEngineBasedConstraint) q)
                                    .forEach(rebc -> kieSessions.insertOrUpdate(id, rebc));
                        }
                    });
            wfiWrapper.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> kieSessions.insertOrUpdate(id, dni));
            kieSessions.setInitialized(id);
        }
    }

    private void createSubWorkflow(List<AbstractWorkflowInstanceObject> awos, String wfiId) {
        awos.stream()
                .filter(awo -> awo instanceof WorkflowWrapperTaskInstance)
                .map(awo -> (WorkflowWrapperTaskInstance) awo)
                .forEach(wwti -> {
//                    Optional<IJiraArtifact> optIJira = wwti.getInput().stream()
//                            .filter(ai -> ai.getArtifact() instanceof ArtifactWrapper)
//                            .map(ai -> ((ArtifactWrapper)ai.getArtifact()).getWrappedArtifact())
//                            .filter(o -> o instanceof IJiraArtifact)
//                            .map(o -> (IJiraArtifact)o)
//                            .findAny();
//                    if (optIJira.isPresent()) {
//
//                    } else {
                        commandGateway.send(new CreateChildWorkflowCmd(wwti.getSubWfiId(), wfiId, wwti.getId(), wwti.getSubWfdId()));
//                    }
                });
    }

    private void addToSubWorkflow(IWorkflowTask wft, ArtifactInput ai) {
        if (wft instanceof WorkflowWrapperTaskInstance) {
            WorkflowWrapperTaskInstance wwti = (WorkflowWrapperTaskInstance) wft;
            commandGateway.send(new AddAsInputToWfiCmd(wwti.getSubWfiId(), ai));
        }
    }
}
