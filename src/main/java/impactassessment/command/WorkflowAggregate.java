package impactassessment.command;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.registry.WorkflowDefinitionContainer;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;
import passiveprocessengine.definition.Artifact;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.ArtifactWrapper;

import java.util.*;
import java.util.stream.Collectors;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@Profile("command")
@Slf4j
public class WorkflowAggregate {

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
    public void handle(CreateMockWorkflowCmd cmd, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        IJiraArtifact a = JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary());
        apply(new CreatedDefaultWorkflowEvt(cmd.getId(), List.of(a)));
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateDefaultWorkflowCmd cmd, IJiraArtifactService artifactService, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        List<IJiraArtifact> artifacts = createWorkflow(cmd.getId(), artifactService, cmd.getInput());
        if (artifacts.size() > 0) {
            apply(new CreatedDefaultWorkflowEvt(cmd.getId(), artifacts));
        } else {
            log.error("Artifacts couldn't get retrieved!");
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateWorkflowCmd cmd, IJiraArtifactService artifactService, WorkflowDefinitionRegistry registry, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        List<IJiraArtifact> artifacts = createWorkflow(cmd.getId(), artifactService, cmd.getInput());
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), artifacts, cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    private List<IJiraArtifact> createWorkflow(String id, IJiraArtifactService artifactService, Map<String, String> inputs) {
        List<IJiraArtifact> artifacts = new ArrayList<>();
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            String key = entry.getKey();
            String source = entry.getValue();
            if (source.equals(Sources.JIRA.toString())) {
                IJiraArtifact a = artifactService.get(key, id);
                if (a != null) {
                    artifacts.add(a);
                }
            } else {
                log.error("Unsupported Artifact source: "+source);
            }
        }
        return artifacts;
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry registry, CommandGateway commandGateway) {
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
    public void handle(DeleteCmd cmd, CommandGateway commandGateway) {
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

    @CommandHandler
    public void handle(UpdateArtifactsCmd cmd) {
        log.info("[AGG] handling {}", cmd);
//        for (IJiraArtifact artifact : cmd.getArtifacts()) {
//            String artifactKey = artifact.getKey();
//            log.debug("Artifact that was updated: {}, Artifacts used by this aggregate: {}", artifactKey, artifactUsages.stream()
//                    .map(ArtifactUsage::getArtifactKey)
//                    .collect(Collectors.joining(",")));
//            for (ArtifactUsage artifactUsage : artifactUsages) {
//                if (artifactUsage.getArtifactKey().equals(artifactKey)) {
//                    ArtifactWrapper wrapper = new ArtifactWrapper(artifact.getKey(), "JiraArtifact", null, artifact);
//                    switch (artifactUsage.getUsage()) {
//                        case OUTPUT:
//                            apply(new AddedOutputEvt(cmd.getId(), artifactUsage.getWftId(), wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType()));
//                            break;
//                        case INPUT:
//                            apply(new AddedInputEvt(cmd.getId(), artifactUsage.getWftId(), wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType()));
//                            break;
//                        case WF_OUTPUT:
//                            ArtifactOutput out = new ArtifactOutput(wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType());
//                            apply(new AddedOutputToWorkflowEvt(cmd.getId(), out));
//                            break;
//                        case WF_INPUT:
//                            ArtifactInput in = new ArtifactInput(wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType());
//                            apply(new AddedInputToWorkflowEvt(cmd.getId(), in));
//                            break;
//                        case RESOURCE:
//                            apply(new CheckedConstraintEvt(cmd.getId(), artifactUsage.getCorrId()));
//                            break;
//                    }
//                }
//            }
//        }
    }

    // -------------------------------- Event Handlers --------------------------------


    @EventSourcingHandler
    public void on(CreatedDefaultWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
    }

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

//    private IJiraArtifact checkIfJiraArtifactInside(Artifact artifact) {
//        if (artifact instanceof ArtifactWrapper) {
//            ArtifactWrapper artifactWrapper = (ArtifactWrapper) artifact;
//            if (artifactWrapper.getWrappedArtifact() instanceof IJiraArtifact) {
//                return (IJiraArtifact) artifactWrapper.getWrappedArtifact();
//            }
//        }
//        return null;
//    }

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
