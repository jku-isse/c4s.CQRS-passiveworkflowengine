package impactassessment.command;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.query.WorkflowModel;
import impactassessment.rulebase.RuleBaseService;
import impactassessment.workflowmodel.*;
import impactassessment.workflowmodel.definition.ConstraintTrigger;
import impactassessment.workflowmodel.definition.QACheckDocument;
import impactassessment.workflowmodel.definition.RuleEngineBasedConstraint;
import impactassessment.workflowmodel.definition.WPManagementWorkflow;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Set;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

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

    // Command Handlers

    @CommandHandler
    public WorkflowAggregate(CreateWorkflowCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedWorkflowEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(EnableCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new EnabledEvt(cmd.getId(), cmd.getDniNumber()));
    }

    @CommandHandler
    public void handle(CompleteCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CompletedEvt(cmd.getId()));
    }


    // Event Handlers

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {
        log.debug("applying {}", evt);
        id = evt.getId();
        model = new WorkflowModel();
        model.handle(evt);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(model.getWorkflowInstance());
        }
    }

    @EventSourcingHandler
    public void on(EnabledEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(CompletedEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {
        log.debug("applying {}", evt);
        model.handle(evt);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(model.getWorkflowInstance());
        }
    }

}
