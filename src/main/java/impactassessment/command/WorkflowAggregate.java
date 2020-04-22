package impactassessment.command;

import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.model.WorkflowModel;
import impactassessment.model.workflowmodel.IdentifiableObject;
import impactassessment.rulebase.RuleBaseService;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@XSlf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    private String id;
    private WorkflowModel model;

    @Transient
    private Map<String, FactHandle> kbContent;

    public WorkflowAggregate() {
        log.debug("[AGG] empty constructor invoked");
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
    log.debug("[AGG] handling {}", cmd);
    initKbContentIfNull();
    apply(new AddedArtifactEvt(cmd.getId(), cmd.getArtifact())).andThen(() -> {
        log.debug("[AGG] insert workflow artifacts into knowledge base");
        updateOrInsert(cmd.getArtifact(), ruleBaseService);
        updateOrInsert(model.getWorkflowInstance(), ruleBaseService);
        model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                .forEach(wft -> updateOrInsert(wft, ruleBaseService));
        model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                .forEach(dni -> updateOrInsert(dni, ruleBaseService));
        ruleBaseService.fire();
    });
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd, RuleBaseService ruleBaseService) {
        log.debug("[AGG] handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getArtifact())).andThen(() -> {
            log.debug("[AGG] insert workflow artifacts into knowledge base");
            updateOrInsert(cmd.getArtifact(), ruleBaseService);
            model.getWorkflowInstance().getWorkflowTasksReadonly().stream()
                    .forEach(wft -> updateOrInsert(wft, ruleBaseService));
            model.getWorkflowInstance().getDecisionNodeInstancesReadonly().stream()
                    .forEach(dni -> updateOrInsert(dni, ruleBaseService));
            ruleBaseService.fire();
        });
    }

    @CommandHandler
    public void handle(ActivateInBranchCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()));
    }

    @CommandHandler
    public void handle(ActivateOutBranchCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()));
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    // Event Handlers

    @EventSourcingHandler
    public void on(AddedArtifactEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getArtifact().getId();
        model = new WorkflowModel();
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(CompletedDataflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(ActivatedInBranchEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(ActivatedOutBranchEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.handle(evt);
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("[AGG] applying {}", evt);
        markDeleted();
    }


    private void initKbContentIfNull() {
        // TODO put this into cmd/evt handler later..not sure at the moment
        if (kbContent == null) {
            kbContent = new HashMap<>();
        }
    }
    private void updateOrInsert(IdentifiableObject o, RuleBaseService ruleBaseService) {
        if (kbContent.containsKey(o.getId())) {
            FactHandle handle = kbContent.get(o.getId());
            ruleBaseService.update(handle, o);
        } else {
            FactHandle handle = ruleBaseService.insert(o);
            kbContent.put(o.getId(), handle);
        }
    }

    private void updateOrInsert(Artifact a, RuleBaseService ruleBaseService) {
        if (kbContent.containsKey(a.getId())) {
            FactHandle handle = kbContent.get(a.getId());
            ruleBaseService.update(handle, a);
        } else {
            FactHandle handle = ruleBaseService.insert(a);
            kbContent.put(a.getId(), handle);
        }
    }
}
