package impactassessment.command;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.registry.WorkflowDefinitionContainer;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@Slf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    String id;
    WorkflowInstanceWrapper model;
    private String parentWfiId; // also parent aggregate id
    private String parentWftId;

    public WorkflowAggregate() {
        log.debug("[AGG] empty constructor WorkflowAggregate invoked");
    }

    public String getId() {
        return id;
    }
    public WorkflowInstanceWrapper getModel() {
        return model;
    }


    // -------------------------------- Command Handlers --------------------------------


    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateMockWorkflowCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        IJiraArtifact a = JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary());
        apply(new CreatedDefaultWorkflowEvt(cmd.getId(), List.of(a)));
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateDefaultWorkflowCmd cmd, IJiraArtifactService artifactService) {
        log.info("[AGG] handling {}", cmd);
        List<IJiraArtifact> artifacts = new ArrayList<>();
        for (Map.Entry<String, String> entry : cmd.getInput().entrySet()) {
            String id = entry.getKey();
            String source = entry.getValue();
            if (source.equals(Sources.JIRA.toString())) {
                IJiraArtifact a = artifactService.get(id);
                if (a != null) {
                    artifacts.add(a);
                }
            } else {
                log.error("Unsupported Artifact source: "+source);
            }
        }

        if (artifacts.size() > 0) {
            apply(new CreatedDefaultWorkflowEvt(cmd.getId(), artifacts));
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateWorkflowCmd cmd, IJiraArtifactService artifactService, WorkflowDefinitionRegistry registry) {
        log.info("[AGG] handling {}", cmd);
        List<IJiraArtifact> artifacts = new ArrayList<>();
        for (Map.Entry<String, String> entry : cmd.getInput().entrySet()) {
            String id = entry.getKey();
            String source = entry.getValue();
            if (source.equals(Sources.JIRA.toString())) {
                IJiraArtifact a = artifactService.get(id);
                if (a != null) {
                    artifacts.add(a);
                }
            } else {
                log.error("Unsupported Artifact source: "+source);
            }
        }

        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), artifacts, cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry registry) {
        log.info("[AGG] handling {}", cmd);
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedSubWorkflowEvt(cmd.getId(), cmd.getParentWfiId(), cmd.getParentWftId(), cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    @CommandHandler
    public void handle(CompleteDataflowCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getRes()));
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
    public void handle(ActivateInOutBranchCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchId()));
    }

    @CommandHandler
    public void handle(ActivateInOutBranchesCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new ActivatedInOutBranchesEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchIds()));
    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddConstraintsCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()));
    }

    @CommandHandler
    public void handle(AddEvaluationResultToConstraintCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()));
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new CheckedConstraintEvt(cmd.getId(), cmd.getCorrId()));
    }

    @CommandHandler
    public void handle(CheckAllConstraintsCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new CheckedAllConstraintsEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddInputCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()));
    }

    @CommandHandler
    public void handle(AddOutputCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()));
        if (parentWfiId != null && parentWftId != null) {
            apply(new AddedOutputEvt(parentWfiId, parentWftId, cmd.getArtifact(), cmd.getRole(), cmd.getType()));
        }
    }

    @CommandHandler
    public void handle(AddInputToWorkflowCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedInputToWorkflowEvt(cmd.getId(), cmd.getInput()));
    }

    @CommandHandler
    public void handle(AddOutputToWorkflowCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedOutputToWorkflowEvt(cmd.getId(), cmd.getOutput()));
    }


    // -------------------------------- Event Handlers --------------------------------


    @EventSourcingHandler
    public void on(CreatedDefaultWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        if (model == null || id == null) { // CREATE
            id = evt.getId();
            model = new WorkflowInstanceWrapper();
            model.handle(evt);
        }
        model.setArtifact(evt.getArtifacts()); // UPDATE
    }

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        if (model == null || id == null) { // CREATE
            id = evt.getId();
            model = new WorkflowInstanceWrapper();
            model.handle(evt);
        }
        model.setArtifact(evt.getArtifacts()); // UPDATE
    }

    @EventSourcingHandler
    public void on(CreatedSubWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        parentWfiId = evt.getParentWfiId();
        parentWftId = evt.getParentWftId();
        model = new WorkflowInstanceWrapper();
        model.handle(evt);
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

}
