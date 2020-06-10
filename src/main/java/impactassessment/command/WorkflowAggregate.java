package impactassessment.command;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.rulebase.RuleBaseService;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@Slf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    private String id;
    private WorkflowInstanceWrapper model;
    private Artifact artifact;

    public WorkflowAggregate() {
        log.debug("[AGG] empty constructor invoked");
    }

    public String getId() {
        return id;
    }
    public WorkflowInstanceWrapper getModel() {
        return model;
    }

    // Command Handlers

    @CommandHandler
    public WorkflowAggregate(AddArtifactCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedArtifactEvt(cmd.getId(), cmd.getArtifact()))
            .andThen(() -> {
                ruleBaseService.insertOrUpdate(cmd.getId(), cmd.getArtifact());
                ruleBaseService.insertOrUpdate(cmd.getId(), model.getWorkflowInstance());
                model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                        .forEach(wft -> ruleBaseService.insertOrUpdate(cmd.getId(), wft));
                model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                        .forEach(dni -> ruleBaseService.insertOrUpdate(cmd.getId(), dni));
                ruleBaseService.setInitialized(cmd.getId());
                ruleBaseService.fire(cmd.getId());
            });
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getArtifact()))
            .andThen(() -> {
                ruleBaseService.insertOrUpdate(cmd.getId(), cmd.getArtifact());
                model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                        .forEach(wft -> ruleBaseService.insertOrUpdate(cmd.getId(), wft));
                model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                        .forEach(dni -> ruleBaseService.insertOrUpdate(cmd.getId(), dni));
                ruleBaseService.fire(cmd.getId());
            });
    }

    @CommandHandler
    public void handle(ActivateInBranchCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> ruleBaseService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> ruleBaseService.insertOrUpdate(cmd.getId(), dni));
                    ruleBaseService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(ActivateOutBranchCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> ruleBaseService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> ruleBaseService.insertOrUpdate(cmd.getId(), dni));
                    ruleBaseService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddQAConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedQAConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getState(), cmd.getConstrPrefix(), cmd.getRuleName(), cmd.getDescription()))
                .andThen(() -> {
                    QACheckDocument doc = model.getQACDocOfWft(cmd.getWftId());
                    ruleBaseService.insertOrUpdate(cmd.getId(), doc);
                    Optional<RuleEngineBasedConstraint> rebc = doc.getConstraintsReadonly().stream()
                            .filter(q -> q instanceof RuleEngineBasedConstraint)
                            .map(q -> (RuleEngineBasedConstraint) q)
                            .filter(r -> r.getConstraintType().equals(cmd.getRuleName()))
                            .findAny();
                    rebc.ifPresent(r -> {
                        ruleBaseService.insertOrUpdate(cmd.getId(), r);
                        // insert constraint trigger
                        ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance(), new CorrelationTuple(r.getId(), "AddQAConstraintCmd"));
                        ct.addConstraint(r.getConstraintType());
                        ruleBaseService.insertOrUpdate(cmd.getId(), ct);
                    });
                    ruleBaseService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(AddResourceToConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedResourceToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getFulfilled(), cmd.getRes(), cmd.getCorr(), cmd.getTime()))
                .andThen(() -> {
                    QACheckDocument.QAConstraint qac = model.getQAC(cmd.getQacId());
                    ruleBaseService.insertOrUpdate(cmd.getId(), qac);
                    ruleBaseService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(AddResourcesToConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedResourcesToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getFulfilled(), cmd.getRes(), cmd.getCorr(), cmd.getTime()))
                .andThen(() -> {
                    QACheckDocument.QAConstraint qac = model.getQAC(cmd.getQacId());
                    ruleBaseService.insertOrUpdate(cmd.getId(), qac);
                    ruleBaseService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), ruleBaseService);
        RuleEngineBasedConstraint rebc = model.getQAC(cmd.getCorrId());
        if (rebc != null) {
            ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance(), new CorrelationTuple(cmd.getCorrId(), "CheckConstraintCmd"));
            ct.addConstraint(rebc.getConstraintType());
            ruleBaseService.insertOrUpdate(cmd.getId(), ct);
            ruleBaseService.fire(cmd.getId());
        } else {
            log.warn("Concerened RuleEngineBasedConstraint wasn't found");
        }
    }

    @CommandHandler
    public void handle(PrintKBCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        if (ruleBaseService.getKieSession(cmd.getId()) != null) {
            StringBuilder s = new StringBuilder();
            s.append("\n############## KB CONTENT ################\n");
            ruleBaseService.getKieSession(cmd.getId()).getObjects().stream()
                    .forEach(o -> s.append(o.toString() + "\n"));
            s.append("############## SIZE: " + ruleBaseService.getKieSession(cmd.getId()).getObjects().size() + " ################");
            log.info(s.toString());
        }
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(AddedArtifactEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getArtifact().getId();
        model = new WorkflowInstanceWrapper();
        artifact = evt.getArtifact();
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("[AGG] applying {}", evt);
        // TODO delete kieSession
        markDeleted();
    }

    @EventSourcingHandler
    public void on(IdentifiableEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.handle(evt);
    }


    /**
     * Needed for user commands, because then the KB must be checked if it is initialized.
     * First call in such command handlers!
     *
     * @param id
     * @param ruleBaseService
     */
    private void ensureInitializedKB(String id, RuleBaseService ruleBaseService) {
        if (!ruleBaseService.isInitialized(id)) {
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            ruleBaseService.insertOrUpdate(id, artifact);
            ruleBaseService.insertOrUpdate(id, model.getWorkflowInstance());
            model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> {
                        ruleBaseService.insertOrUpdate(id, wft);
                        QACheckDocument doc = model.getQACDocOfWft(wft.getTaskId());
                        if (doc != null) {
                            ruleBaseService.insertOrUpdate(id, doc);
                            doc.getConstraintsReadonly().stream()
                                    .filter(q -> q instanceof RuleEngineBasedConstraint)
                                    .map(q -> (RuleEngineBasedConstraint) q)
                                    .forEach(rebc -> ruleBaseService.insertOrUpdate(id, rebc));
                        }
                    });
            model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> ruleBaseService.insertOrUpdate(id, dni));
            ruleBaseService.setInitialized(id);
        }
    }
}
