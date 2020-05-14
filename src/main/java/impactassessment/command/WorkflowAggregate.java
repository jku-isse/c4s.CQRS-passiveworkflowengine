package impactassessment.command;

import impactassessment.api.*;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.rulebase.RuleBaseService;
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
@XSlf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    private String id;
    private WorkflowInstanceWrapper model;

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
                ruleBaseService.insertOrUpdate(cmd.getArtifact());
                ruleBaseService.insertOrUpdate(model.getWorkflowInstance());
                model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                        .forEach(wft -> ruleBaseService.insertOrUpdate(wft));
                model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                        .forEach(dni -> ruleBaseService.insertOrUpdate(dni));
                ruleBaseService.fire();
            });
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getArtifact()))
            .andThen(() -> {
                ruleBaseService.insertOrUpdate(cmd.getArtifact());
                model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                        .forEach(wft -> ruleBaseService.insertOrUpdate(wft));
                model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                        .forEach(dni -> ruleBaseService.insertOrUpdate(dni));
                ruleBaseService.fire();
            });
    }

    @CommandHandler
    public void handle(ActivateInBranchCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()));
    }

    @CommandHandler
    public void handle(ActivateOutBranchCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()));
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AppendQACheckDocumentCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AppendedQACheckDocumentEvt(cmd.getId(), cmd.getWftId(), cmd.getState()))
                .andThen(() -> {
                    QACheckDocument doc = model.getQACDocOfWft(cmd.getWftId());
                    ruleBaseService.insertOrUpdate(doc);
                    ruleBaseService.fire();
                });
    }

    @CommandHandler
    public void handle(AddQAConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedQAConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getConstrPrefix(), cmd.getRuleName(), cmd.getDescription()))
                .andThen(() -> {
                    QACheckDocument doc = model.getQACDocOfWft(cmd.getWftId());
                    Optional<RuleEngineBasedConstraint> rebc = doc.getConstraintsReadonly().stream()
                            .filter(q -> q instanceof RuleEngineBasedConstraint)
                            .map(q -> (RuleEngineBasedConstraint) q)
                            .filter(r -> r.getConstraintType().equals(cmd.getRuleName()))
                            .findAny();
                    rebc.ifPresent(r -> {
                        ruleBaseService.insertOrUpdate(r);
                        // insert constraint trigger
                        ConstraintTrigger ct = new ConstraintTrigger(model.getWorkflowInstance());
                        ct.addConstraint(r.getConstraintType());
                        ruleBaseService.insertOrUpdate(ct);

                        ruleBaseService.fire();
                    });

                });
    }

    @CommandHandler
    public void handle(AddResourceToConstraintCmd cmd, RuleBaseService ruleBaseService) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedResourceToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getFulfilled(), cmd.getRes()))
                .andThen(() -> {
                    QACheckDocument.QAConstraint qac = model.getQAC(cmd.getQacId());
                    ruleBaseService.insertOrUpdate(qac);
                    ruleBaseService.fire();
                });
    }
    // Event Handlers

    @EventSourcingHandler
    public void on(AddedArtifactEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getArtifact().getId();
        model = new WorkflowInstanceWrapper();
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("[AGG] applying {}", evt);
        markDeleted();
    }

    @EventSourcingHandler
    public void on(IdentifiableEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.handle(evt);
    }

}
