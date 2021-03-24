package impactassessment.command;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands.*;
import impactassessment.api.Events.*;
import impactassessment.artifactconnector.jira.mock.JiraMockService;
import impactassessment.registry.WorkflowDefinitionContainer;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate(snapshotTriggerDefinition="workflowSnapshotTrigger")
@Profile("command")
@Slf4j
public class WorkflowAggregate implements Serializable {

    @AggregateIdentifier
    String id;
    private String parentWfiId; // also parent aggregate id
    private String parentWftId;

    public WorkflowAggregate() {
        log.debug("[AGG] empty constructor WorkflowAggregate invoked");
    }

    public String getId() {
        return id;
    }


    // -------------------------------- Command Handlers --------------------------------


    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateMockWorkflowCmd cmd, WorkflowDefinitionRegistry registry) {
        log.debug("[AGG] handling {}", cmd);
        String workflowName = "DRONOLOGY_WORKFLOW_FIXED"; // always used for mock-artifacts
        Entry<String,IArtifact> a  = new AbstractMap.SimpleEntry<>("ROLE_WPTICKET", JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary()));
        WorkflowDefinitionContainer wfdContainer = registry.get(workflowName);
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), List.of(a), workflowName, wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", workflowName);
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateWorkflowCmd cmd, IArtifactRegistry artifactRegistry, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
        log.info("[AGG] handling {}", cmd);
        Collection<Entry<String,IArtifact>> artifacts = createWorkflow(cmd.getId(), artifactRegistry, cmd.getInput());
        WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), artifacts, cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    private Collection<Entry<String,IArtifact>> createWorkflow(String id, IArtifactRegistry artifactRegistry, Map<String, String> inputs) {
        List<Entry<String,IArtifact>> artifacts = new ArrayList<>();
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            String key = entry.getKey();
            String source = entry.getValue();
            // taking"::" as separator from MainView.java
            int sepPos = source.lastIndexOf("::");
            String role =  source.substring(0, sepPos);
            String type = source.substring(sepPos+2);
            ArtifactIdentifier ai = new ArtifactIdentifier(key, type);
            try {
			    artifactRegistry.get(ai, id).ifPresent(art -> artifacts.add(new AbstractMap.SimpleEntry<String, IArtifact>(role,art)));
            } catch (Exception e) {
                log.warn("Artifact {} couldn't be fetched due to {}", key, e.getClass().getSimpleName());
            }
        }
        if (inputs.size() != artifacts.size())
            log.warn("One or more required artifacts couldn't be fetched on creation of {}", id);
        return artifacts;
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry registry) {
        log.debug("[AGG] handling {}", cmd);
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedSubWorkflowEvt(cmd.getId(), cmd.getParentWfiId(), cmd.getParentWftId(), cmd.getDefinitionName(), wfdContainer.getWfd(), cmd.getArtifacts()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

//    @CommandHandler
//    public void handle(CompleteDataflowCmd cmd) {
//        log.debug("[AGG] handling {}", cmd);
//        apply(new CompletedDataflowEvt(cmd.getId(), cmd.getDniId(), cmd.getRes()));
//    }
//
//    @CommandHandler
//    public void handle(ActivateInBranchCmd cmd) {
//        log.debug("[AGG] handling {}", cmd);
//        apply(new ActivatedInBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId()));
//    }
//
//    @CommandHandler
//    public void handle(ActivateOutBranchCmd cmd) {
//        log.debug("[AGG] handling {}", cmd);
//        apply(new ActivatedOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getBranchId()));
//    }
//
//    @CommandHandler
//    public void handle(ActivateInOutBranchCmd cmd) {
//        log.debug("[AGG] handling {}", cmd);
//        apply(new ActivatedInOutBranchEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchId()));
//    }
//
//    @CommandHandler
//    public void handle(ActivateInOutBranchesCmd cmd) {
//        log.debug("[AGG] handling {}", cmd);
//        apply(new ActivatedInOutBranchesEvt(cmd.getId(), cmd.getDniId(), cmd.getWftId(), cmd.getBranchIds()));
//    }

    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new DeletedEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddConstraintsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()));
    }

    @CommandHandler
    public void handle(AddEvaluationResultToConstraintCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()));
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new CheckedConstraintEvt(cmd.getId(), cmd.getCorrId()));
    }

    @CommandHandler
    public void handle(CheckAllConstraintsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new CheckedAllConstraintsEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddInputCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        if (opt.isPresent()) {
            apply(new AddedInputEvt(cmd.getId(), cmd.getWftId(), opt.get().getArtifactIdentifier(), cmd.getRole(), cmd.getType()));
        } else {
            log.warn("Artifact {} was not found.", cmd.getArtifactId());
        }
    }

    @CommandHandler
    public void handle(AddOutputCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        if (opt.isPresent()) {
            apply(new AddedOutputEvt(cmd.getId(), cmd.getWftId(), opt.get(), cmd.getRole(), cmd.getType()));
            if (parentWfiId != null && parentWftId != null) {
                apply(new AddedOutputEvt(parentWfiId, parentWftId, opt.get(), cmd.getRole(), cmd.getType()));
            }
        } else {
            log.warn("Artifact {} was not found.", cmd.getArtifactId());
        }
    }

    @CommandHandler
    public void handle(AddInputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        opt.ifPresent(artifact -> apply(new AddedInputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole(), cmd.getType())));
    }

    @CommandHandler
    public void handle(AddOutputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        opt.ifPresent(artifact -> apply(new AddedOutputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole(), cmd.getType())));
    }

    @CommandHandler
    public void handle(UpdateArtifactsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new UpdatedArtifactsEvt(cmd.getId(), cmd.getArtifacts()));
    }

    @CommandHandler
    public void handle(SetPreConditionsFulfillmentCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new SetPreConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled()));
    }

    @CommandHandler
    public void handle(SetPostConditionsFulfillmentCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new SetPostConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled()));
    }

    @CommandHandler
    public void handle(ActivateTaskCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new ActivatedTaskEvt(cmd.getId(), cmd.getWftId()));
    }

    @CommandHandler
    public void handle(SetPropertiesCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new SetPropertiesEvt(cmd.getId(), cmd.getIwftId(), cmd.getProperties()));
    }

    @CommandHandler
    public void handle(InstantiateTaskCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new InstantiatedTaskEvt(cmd.getId(), cmd.getTaskDefinitionId(), cmd.getOptionalInputs(), cmd.getOptionalOutputs()));
    }

    // -------------------------------- Event Handlers --------------------------------

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
    }

    @EventSourcingHandler
    public void on(CreatedSubWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        parentWfiId = evt.getParentWfiId();
        parentWftId = evt.getParentWftId();
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
    }

}
