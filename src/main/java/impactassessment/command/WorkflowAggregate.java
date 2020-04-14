package impactassessment.command;

import impactassessment.api.*;
import impactassessment.model.WorkflowModel;
import impactassessment.rulebase.RuleBaseService;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.QACheckDocument;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@XSlf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    private String id;
    private WorkflowModel model;

    public WorkflowAggregate() {
        log.debug("empty constructor invoked");
    }

    public String getId() {
        return id;
    }
    public WorkflowModel getModel() {
        return model;
    }
    // Command Handlers

    @CommandHandler
    public WorkflowAggregate(CreateWorkflowCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedWorkflowEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(CreateWorkflowInstanceOfCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedWorkflowInstanceOfEvt(cmd.getId(), cmd.getWfd()));
    }

    @CommandHandler
    public void handle(EnableTasksAndDecisionsCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new EnabledTasksAndDecisionsEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(CompleteDataflowOfDecisionNodeInstanceCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CompletedDataflowOfDecisionNodeInstanceEvt(cmd.getId(), cmd.getDniIndex()));
    }

    @CommandHandler
    public void handle(AddQAConstraintsAsArtifactOutputsCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new AddedQAConstraintsAsArtifactOutputsEvt(cmd.getId(), cmd.getQac()));
    }

    @CommandHandler
    public void handle(CreateConstraintTriggerCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedConstraintTriggerEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getId();
        model = new WorkflowModel();
    }

    @EventSourcingHandler
    public void on(CreatedWorkflowInstanceOfEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {
        log.debug("applying {}", evt);
        model.handle(evt);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(model.getWorkflowInstance());
        }
    }

    @EventSourcingHandler
    public void on(EnabledTasksAndDecisionsEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(CompletedDataflowOfDecisionNodeInstanceEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(AddedQAConstraintsAsArtifactOutputsEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {
        log.debug("applying {}", evt);
        QACheckDocument.QAConstraint qac = model.handle(evt);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(qac);
        }
    }

    @EventSourcingHandler
    public void on(CreatedConstraintTriggerEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {
        log.debug("applying {}", evt);
        ConstraintTrigger ct = model.handle(evt);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(ct);
        }
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("applying {}", evt);
        markDeleted();
    }

}
