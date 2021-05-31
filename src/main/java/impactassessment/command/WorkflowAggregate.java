package impactassessment.command;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands.*;
import impactassessment.api.Events.*;
import impactassessment.command.model.CmdConstraint;
import impactassessment.command.model.CmdTask;
import impactassessment.command.model.CmdWorkflow;
import impactassessment.passiveprocessengine.LazyLoadingArtifactInput;
import impactassessment.passiveprocessengine.LazyLoadingArtifactOutput;
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
import java.util.stream.Collectors;

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

    private CmdWorkflow model;

    public WorkflowAggregate() {
        log.debug("[AGG] empty constructor WorkflowAggregate invoked");
    }

    public String getId() {
        return id;
    }

    // -------------------------------- Command Handlers --------------------------------

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateWorkflowCmd cmd, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
        log.info("[AGG] handling {}", cmd);
        WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), cmd.getInput(), cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry registry) {
        log.debug("[AGG] handling {}", cmd);
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedSubWorkflowEvt(cmd.getId(), cmd.getParentWfiId(), cmd.getParentWftId(), cmd.getDefinitionName(), wfdContainer.getWfd(), cmd.getInput()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

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
        if (model.getConstraint(cmd.getWftId(), cmd.getQacId()).isEmpty() || model.getConstraint(cmd.getWftId(), cmd.getQacId()).get().hasChanged(cmd.getRes())) {
            log.debug("AddEvaluationResultToConstraintCmd caused --> AddedEvaluationResultToConstraintEvt");
            apply(new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime()));
        } else {
            log.debug("AddEvaluationResultToConstraintCmd caused --> UpdatedEvaluationTimeEvt");
            apply(new UpdatedEvaluationTimeEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getCorr(), cmd.getTime()));
        }
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new CheckedConstraintEvt(cmd.getId(), cmd.getConstrId()));
    }

    @CommandHandler
    public void handle(CheckAllConstraintsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new CheckedAllConstraintsEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddInputCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        if (model.getTask(cmd.getWftId()).isPresent()) {
            ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
            Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
            if (opt.isPresent()) {
                apply(new AddedInputEvt(cmd.getId(), cmd.getWftId(), opt.get().getArtifactIdentifier(), cmd.getRole()));
            } else {
                log.warn("Artifact {} was not found.", cmd.getArtifactId());
            }
        } else {
            log.warn("Process step {} is not existing.", cmd.getWftId());
        }
    }

    @CommandHandler
    public void handle(AddOutputCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        if (model.getTask(cmd.getWftId()).isPresent()) {
            ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
            Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
            if (opt.isPresent()) {
                apply(new AddedOutputEvt(cmd.getId(), cmd.getWftId(), ai, cmd.getRole()));
                if (parentWfiId != null && parentWftId != null) {
                    apply(new AddedOutputEvt(parentWfiId, parentWftId, ai, cmd.getRole()));
                }
            } else {
                log.warn("Artifact {} was not found.", cmd.getArtifactId());
            }
        } else {
            log.warn("Process step {} is not existing.", cmd.getWftId());
        }
    }

    @CommandHandler
    public void handle(AddInputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        opt.ifPresent(artifact -> apply(new AddedInputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole())));
    }

    @CommandHandler
    public void handle(AddOutputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        opt.ifPresent(artifact -> apply(new AddedOutputToWorkflowEvt(cmd.getId(), artifact.getArtifactIdentifier(), cmd.getRole())));
    }

    @CommandHandler
    public void handle(UpdateArtifactsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        apply(new UpdatedArtifactsEvt(cmd.getId(), cmd.getArtifacts().stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toList())));
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
    public void handle(InstantiateTaskCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        apply(new InstantiatedTaskEvt(cmd.getId(), cmd.getTaskDefinitionId(), 
        										cmd.getOptionalInputs().stream().map(in -> LazyLoadingArtifactInput.generateFrom(in, artifactRegistry, cmd.getId())).collect(Collectors.toList())  , 
        										cmd.getOptionalOutputs().stream().map(out -> LazyLoadingArtifactOutput.generateFrom(out, artifactRegistry, cmd.getId())).collect(Collectors.toList())   ));
    }

    @CommandHandler
    public void handle(RemoveInputCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        if (model.getId().equals(cmd.getWftId()) || model.getTask(cmd.getWftId()).isPresent())
            apply(new RemovedInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole()));
        else
            log.warn("Target: '{}' is not existing.", cmd.getWftId());
    }

    @CommandHandler
    public void handle(RemoveOutputCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        if (model.getId().equals(cmd.getWftId()) || model.getTask(cmd.getWftId()).isPresent())
            apply(new RemovedOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole()));
        else
            log.warn("Target: '{}' is not existing.", cmd.getWftId());
    }

    // -------------------------------- Event Handlers --------------------------------

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        model = new CmdWorkflow(id);
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

    @EventSourcingHandler
    public void on(AddedConstraintsEvt evt) {
        log.debug("[AGG] applying {}", evt);
        CmdTask task = new CmdTask(evt.getWftId());
        model.add(task);
    }

    @EventSourcingHandler
    public void on(AddedEvaluationResultToConstraintEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.getConstraint(evt.getWftId(), evt.getQacId())
                .ifPresentOrElse(
                        constr -> constr.update(evt.getRes()),
                        () -> model.getTask(evt.getWftId())
                                .ifPresent(task -> {
                                    CmdConstraint constr = new CmdConstraint(evt.getQacId());
                                    constr.update(evt.getRes());
                                    task.add(constr);
                                })
                );
    }

    // this event handler processes all events (if not already treated by above)
    // because every event inherits from IdentifiableEvt
    @EventSourcingHandler
    public void on(IdentifiableEvt evt) {
        log.debug("[AGG] applying {}", evt);
    }

}
