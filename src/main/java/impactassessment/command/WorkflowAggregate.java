package impactassessment.command;

import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.model.WorkflowModel;
import impactassessment.model.workflowmodel.AbstractWorkflowInstanceObject;
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

import java.util.List;

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
    public WorkflowAggregate(AddArtifactCmd cmd, RuleBaseService ruleBaseService) {
    log.debug("handling {}", cmd);
        apply(new AddedArtifactEvt(cmd.getId(), cmd.getArtifact())).andThen(() -> {
            log.debug("insert workflow artifacts into knowledge base");
            ruleBaseService.insert(cmd.getArtifact());
            ruleBaseService.insert(model.getWorkflowInstance());
            model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> ruleBaseService.insert(wft));
            model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> ruleBaseService.insert(dni));
            ruleBaseService.fire();
        });
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd, RuleBaseService ruleBaseService) {
        log.debug("handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getArtifact())).andThen(() -> {
            log.debug("insert workflow artifacts into knowledge base");
            model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> ruleBaseService.insert(wft));
            model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> ruleBaseService.insert(dni));
            ruleBaseService.fire();
        });
    }

    @CommandHandler
    public void handle(ActivateInBranchCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()));
    }

    @CommandHandler
    public void handle(ActivateOutBranchCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()));
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(AddedArtifactEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getArtifact().getId();
        model = new WorkflowModel();
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(CompletedDataflowEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(ActivatedInBranchEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(ActivatedOutBranchEvt evt) {
        log.debug("applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("applying {}", evt);
        markDeleted();
    }
}
