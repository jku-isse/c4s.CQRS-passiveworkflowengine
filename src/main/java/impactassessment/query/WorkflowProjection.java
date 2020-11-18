package impactassessment.query;

import impactassessment.api.Events.*;
import impactassessment.api.Queries.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.kiesession.KieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.FrontendPusher;
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
import passiveprocessengine.instance.AbstractWorkflowInstanceObject;
import passiveprocessengine.instance.ConstraintTrigger;
import passiveprocessengine.instance.CorrelationTuple;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static impactassessment.query.WorkflowHelpers.*;

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
    private final FrontendPusher pusher;

    // Event Handlers

    @EventHandler
    public void on(CreatedWorkflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        createKieSession(kieSessions, projection, evt.getId(), kieContainer);
        WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, evt.getArtifacts(), status.isReplay());
    }

    @EventHandler
    public void on(CreatedSubWorkflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        KieContainer kieContainer = registry.get(evt.getDefinitionName()).getKieContainer();
        createKieSession(kieSessions, projection, evt.getId(), kieContainer);
        WorkflowInstanceWrapper wfiWrapper = projection.createAndPutWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, null, status.isReplay());
    }

    @EventHandler
    public void on(CompletedDataflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        Map<IWorkflowTask, ArtifactInput> mappedInputs = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            // TODO remove debug output
            mappedInputs.forEach((key, value) -> log.info("MappedInputs: WFT=" + key.getId() + " AI=" + value.toString()));
            mappedInputs.forEach((wft, ai) -> addToSubWorkflow(commandGateway, wft, ai));
        }
    }

    @EventHandler
    public void on(ActivatedInBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, null, status.isReplay());
    }

    @EventHandler
    public void on(ActivatedOutBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, null, status.isReplay());
        createSubWorkflow(commandGateway, awos, wfiWrapper.getWorkflowInstance().getId(), status.isReplay());
    }

    @EventHandler
    public void on(ActivatedInOutBranchEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, null, status.isReplay());
        createSubWorkflow(commandGateway, awos, wfiWrapper.getWorkflowInstance().getId(), status.isReplay());
    }

    @EventHandler
    public void on(ActivatedInOutBranchesEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        List<AbstractWorkflowInstanceObject> awos = wfiWrapper.handle(evt);
        insertOrUpdateKieSession(kieSessions, evt.getId(), awos, null, status.isReplay());
        createSubWorkflow(commandGateway, awos, wfiWrapper.getWorkflowInstance().getId(), status.isReplay());
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
        pusher.update(new ArrayList<>(projection.getDb().values()));
    }

    @DisallowReplay
    @EventHandler
    public void on(CheckedConstraintEvt evt) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        if (wfiWrapper != null) {
            RuleEngineBasedConstraint rebc = wfiWrapper.getQAC(evt.getCorrId());
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
        log.info("[PRJ] projecting {}", evt);
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
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        IWorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(kieSessions, projection, evt.getId());
            kieSessions.insertOrUpdate(evt.getId(), wft);
            kieSessions.fire(evt.getId());
            addToSubWorkflow(commandGateway, wft, new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType()));
        }
    }

    @EventHandler
    public void on(AddedOutputEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        IWorkflowTask wft = wfiWrapper.handle(evt);
        if (!status.isReplay()) {
            ensureInitializedKB(kieSessions, projection, evt.getId());
            kieSessions.insertOrUpdate(evt.getId(), wft);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(AddedInputToWorkflowEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        // if this input is an jira-artifact, insert it into kieSession
        if (!status.isReplay()) {
            if (evt.getInput().getArtifact() instanceof ArtifactWrapper) {
                ArtifactWrapper artWrapper = (ArtifactWrapper) evt.getInput().getArtifact();
                if (artWrapper.getWrappedArtifact() instanceof IJiraArtifact) {
                    IJiraArtifact iJira = (IJiraArtifact) artWrapper.getWrappedArtifact();
                    kieSessions.insertOrUpdate(evt.getId(), iJira);
                    kieSessions.fire(evt.getId());
                }
            }
        }
    }

    @EventHandler
    public void on(UpdatedArtifactsEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        WorkflowInstanceWrapper wfiWrapper = projection.getWorkflowModel(evt.getId());
        // Is artifact used as Input/Output to workflow? --> update workflow, update in kieSession
        for (IJiraArtifact updatedArtifact : evt.getArtifacts()) {
            for (ArtifactInput input : wfiWrapper.getWorkflowInstance().getInput()) {
                IJiraArtifact presentArtifact = checkIfJiraArtifactInside(input.getArtifact());
                if (presentArtifact != null && presentArtifact.getKey().equals(updatedArtifact.getKey())) {
                    input.setArtifact(new ArtifactWrapper("Wrapping-"+updatedArtifact.getKey(), "IJiraArtifact", wfiWrapper.getWorkflowInstance(), updatedArtifact));
                    if (!status.isReplay()) {
                        kieSessions.insertOrUpdate(evt.getId(), updatedArtifact);
                        kieSessions.fire(evt.getId());
                    }
                }
            }
        }
        // TODO: Is artifact used as Input/Output of a WFT --> update WFT, update WFT in kieSession

        // CheckAllConstraints
        if (!status.isReplay()) {
            ensureInitializedKB(kieSessions, projection, evt.getId());
            ConstraintTrigger ct = new ConstraintTrigger(wfiWrapper.getWorkflowInstance(), new CorrelationTuple(evt.getId(), "CheckAllConstraintsCmd"));
            ct.addConstraint("*");
            kieSessions.insertOrUpdate(evt.getId(), ct);
            kieSessions.fire(evt.getId());
        }
    }

    @EventHandler
    public void on(DeletedEvt evt, ReplayStatus status) {
        log.info("[PRJ] projecting {}", evt);
        projection.handle(evt);
        pusher.update(new ArrayList<>(projection.getDb().values()));
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

}
