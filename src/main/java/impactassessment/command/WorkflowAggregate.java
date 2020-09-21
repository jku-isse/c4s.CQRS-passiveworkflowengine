package impactassessment.command;

import impactassessment.passiveprocessengine.instance.*;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.kiesession.KieSessionService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@Slf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    private String id;
    private WorkflowInstanceWrapper model;
    private IJiraArtifact artifact;

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
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(AddMockArtifactCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), kieSessionService);
        IJiraArtifact a = JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary());
        apply(new ImportedOrUpdatedArtifactEvt(cmd.getId(), a))
                .andThen(() -> insertEverything(cmd.getId(), kieSessionService, a));
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ImportOrUpdateArtifactCmd cmd, KieSessionService kieSessionService, IJiraArtifactService artifactService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), kieSessionService);
        if (cmd.getSource().equals(Sources.JIRA)) {
            IJiraArtifact a = artifactService.get(cmd.getId());
            if (a != null) {
                apply(new ImportedOrUpdatedArtifactEvt(cmd.getId(), a))
                        .andThen(() -> insertEverything(cmd.getId(), kieSessionService, a));
            }
        } else {
            log.error("Unsupported Artifact source: "+cmd.getSource());
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ImportOrUpdateArtifactWithWorkflowDefinitionCmd cmd, KieSessionService kieSessionService, IJiraArtifactService artifactService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), kieSessionService);
        if (cmd.getSource().equals(Sources.JIRA)) {
            IJiraArtifact a = artifactService.get(cmd.getId());
            if (a != null) {
                apply(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(id, a, cmd.getWfd()))
                        .andThen(() -> insertEverything(id, kieSessionService, a));
            }
        } else {
            log.error("Unsupported Artifact source: "+cmd.getSource());
        }
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getRes()))
            .andThen(() -> {
                model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                        .forEach(wft -> kieSessionService.insertOrUpdate(cmd.getId(), wft));
                model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                        .forEach(dni -> kieSessionService.insertOrUpdate(cmd.getId(), dni));
                kieSessionService.fire(cmd.getId());
            });
    }

    @CommandHandler
    public void handle(ActivateInBranchCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> kieSessionService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> kieSessionService.insertOrUpdate(cmd.getId(), dni));
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(ActivateOutBranchCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> kieSessionService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> kieSessionService.insertOrUpdate(cmd.getId(), dni));
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(ActivateInOutBranchCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchId()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> kieSessionService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> kieSessionService.insertOrUpdate(cmd.getId(), dni));
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(ActivateInOutBranchesCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInOutBranchesEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchIds()))
                .andThen(() -> {
                    model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                            .forEach(wft -> kieSessionService.insertOrUpdate(cmd.getId(), wft));
                    model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                            .forEach(dni -> kieSessionService.insertOrUpdate(cmd.getId(), dni));
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(DeleteCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()))
            .andThen(() -> {
                kieSessionService.dispose(cmd.getId());
            });
    }

    @CommandHandler
    public void handle(AddConstraintsCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()))
                .andThen(() -> {
                    QACheckDocument doc = model.getQACDocOfWft(cmd.getWftId());
                    doc.getConstraintsReadonly().stream()
                            .filter(q -> q instanceof RuleEngineBasedConstraint)
                            .map(q -> (RuleEngineBasedConstraint) q)
                            .forEach(c -> {
                                kieSessionService.insertOrUpdate(cmd.getId(), c);
                                // insert constraint trigger
                                ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance(), new CorrelationTuple(c.getId(), "AddConstraintCmd"));
                                ct.addConstraint(c.getConstraintType());
                                kieSessionService.insertOrUpdate(cmd.getId(), ct);
                            });
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(AddEvaluationResultToConstraintCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()))
                .andThen(() -> {
                    QACheckDocument.QAConstraint qac = model.getQAC(cmd.getQacId());
                    kieSessionService.insertOrUpdate(cmd.getId(), qac);
                    kieSessionService.fire(cmd.getId());
                });
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), kieSessionService);
        RuleEngineBasedConstraint rebc = model.getQAC(cmd.getCorrId());
        if (rebc != null) {
            ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance(), new CorrelationTuple(cmd.getCorrId(), "CheckConstraintCmd"));
            ct.addConstraint(rebc.getConstraintType());
            kieSessionService.insertOrUpdate(cmd.getId(), ct);
            kieSessionService.fire(cmd.getId());
        } else {
            log.warn("Concerned RuleEngineBasedConstraint wasn't found");
        }
    }

    @CommandHandler
    public void handle(CheckAllConstraintsCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        ensureInitializedKB(cmd.getId(), kieSessionService);
        ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance(), new CorrelationTuple(cmd.getId(), "CheckAllConstraintsCmd"));
        ct.addConstraint("*");
        kieSessionService.insertOrUpdate(cmd.getId(), ct);
        kieSessionService.fire(cmd.getId());
    }

    // no side effects can occur
    @CommandHandler
    public void handle(PrintKBCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        if (kieSessionService.getKieSession(cmd.getId()) != null) {
            StringBuilder s = new StringBuilder();
            s.append("\n############## KB CONTENT ################\n");
            kieSessionService.getKieSession(cmd.getId()).getObjects().stream()
                    .forEach(o -> s.append(o.toString() + "\n"));
            s.append("####### SIZE: " + kieSessionService.getKieSession(cmd.getId()).getObjects().size() +
                    " ######### "+ kieSessionService.getNumKieSessions()+" #######");
            log.info(s.toString());
        }
    }

    @CommandHandler
    public void handle(AddAsInputCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedAsInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()))
            .andThen(() -> {
                WorkflowTask wft = model.getWorkflowInstance().getWorkflowTask(cmd.getWftId());
                kieSessionService.insertOrUpdate(cmd.getId(), wft);
            });
    }

    @CommandHandler
    public void handle(AddAsOutputCmd cmd, KieSessionService kieSessionService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedAsOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()))
                .andThen(() -> {
                    WorkflowTask wft = model.getWorkflowInstance().getWorkflowTask(cmd.getWftId());
                    kieSessionService.insertOrUpdate(cmd.getId(), wft);
                });
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(ImportedOrUpdatedArtifactEvt evt) {
        log.debug("[AGG] applying {}", evt);
        if (model == null || id == null) { // CREATE
            id = evt.getId();
            model = new WorkflowInstanceWrapper();
            model.handle(evt);
        }
        artifact = evt.getArtifact(); // UPDATE
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("[AGG] applying {}", evt);
        markDeleted();
    }

    // this event handler processes all events (if not already treated by above)
    // because every event inherits from IdentifiableEvt
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
     * @param kieSessionService
     */
    private void ensureInitializedKB(String id, KieSessionService kieSessionService) {
        if (!kieSessionService.isInitialized(id) && artifact != null && model != null) {
            log.info(">>INIT KB<<");
            // if kieSession is not initialized, try to add all artifacts
            kieSessionService.insertOrUpdate(id, artifact);
            kieSessionService.insertOrUpdate(id, model.getWorkflowInstance());
            model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> {
                        kieSessionService.insertOrUpdate(id, wft);
                        QACheckDocument doc = model.getQACDocOfWft(wft.getTaskId());
                        if (doc != null) {
                            kieSessionService.insertOrUpdate(id, doc);
                            doc.getConstraintsReadonly().stream()
                                    .filter(q -> q instanceof RuleEngineBasedConstraint)
                                    .map(q -> (RuleEngineBasedConstraint) q)
                                    .forEach(rebc -> kieSessionService.insertOrUpdate(id, rebc));
                        }
                    });
            model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> kieSessionService.insertOrUpdate(id, dni));
            kieSessionService.setInitialized(id);
        }
    }

    private void insertEverything(String id, KieSessionService kieSessionService, IJiraArtifact a) {
        kieSessionService.insertOrUpdate(id, a);
        kieSessionService.insertOrUpdate(id, model.getWorkflowInstance());
        model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                .forEach(wft -> kieSessionService.insertOrUpdate(id, wft));
        model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                .forEach(dni -> kieSessionService.insertOrUpdate(id, dni));
        kieSessionService.setInitialized(id);
        kieSessionService.fire(id);
    }
}
