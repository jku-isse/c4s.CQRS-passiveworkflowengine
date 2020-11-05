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
import static impactassessment.command.ArtifactUsage.Usage;

@Aggregate
@Profile("command")
@Slf4j
public class WorkflowAggregate {

    @AggregateIdentifier
    String id;
    private String parentWfiId; // also parent aggregate id
    private String parentWftId;
    private Set<ArtifactUsage> artifactUsages = new HashSet<>();

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
        commandGateway.send(new AddIdCmd("update", cmd.getId()));
        IJiraArtifact a = JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary());
        apply(new CreatedDefaultWorkflowEvt(cmd.getId(), List.of(a)));
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateDefaultWorkflowCmd cmd, IJiraArtifactService artifactService, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        commandGateway.send(new AddIdCmd("update", cmd.getId()));
        List<IJiraArtifact> artifacts = createWorkflow(artifactService, cmd.getInput());
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
        commandGateway.send(new AddIdCmd("update", cmd.getId()));
        List<IJiraArtifact> artifacts = createWorkflow(artifactService, cmd.getInput());
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            apply(new CreatedWorkflowEvt(cmd.getId(), artifacts, cmd.getDefinitionName(), wfdContainer.getWfd()));
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
        }
    }

    private List<IJiraArtifact> createWorkflow(IJiraArtifactService artifactService, Map<String, String> inputs) {
        List<IJiraArtifact> artifacts = new ArrayList<>();
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
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
        return artifacts;
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry registry, CommandGateway commandGateway) {
        log.info("[AGG] handling {}", cmd);
        commandGateway.send(new AddIdCmd("update", cmd.getId()));
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
        commandGateway.send(new RemovedIdEvt("update", cmd.getId()));
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
    public void handle(ArtifactUpdateCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        String artifactKey = cmd.getArtifact().getKey();
        log.debug("Artifact that was updated: {}, Artifacts used by this aggregate: {}", artifactKey, artifactUsages.stream()
                .map(ArtifactUsage::getArtifactKey)
                .collect( Collectors.joining( "," ) ));
        for (ArtifactUsage artifactUsage : artifactUsages) {
            if (artifactUsage.getArtifactKey().equals(artifactKey)) {
                ArtifactWrapper wrapper = new ArtifactWrapper(cmd.getArtifact().getKey(), "JiraArtifact", null, cmd.getArtifact());
                switch (artifactUsage.getUsage()) {
                    case OUTPUT:
                        apply(new AddedOutputEvt(cmd.getId(), artifactUsage.getWftId(), wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType()));
                        break;
                    case INPUT:
                        apply(new AddedInputEvt(cmd.getId(), artifactUsage.getWftId(), wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType()));
                        break;
                    case WF_OUTPUT:
                        ArtifactOutput out = new ArtifactOutput(wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType());
                        apply(new AddedOutputToWorkflowEvt(cmd.getId(), out));
                        break;
                    case WF_INPUT:
                        ArtifactInput in = new ArtifactInput(wrapper, artifactUsage.getRole(), artifactUsage.getArtifactType());
                        apply(new AddedInputToWorkflowEvt(cmd.getId(), in));
                        break;
                    case RESOURCE:
                        apply(new CheckedConstraintEvt(cmd.getId(), artifactUsage.getCorrId()));
                        break;
                }
            }
        }
    }

    @CommandHandler
    public void handle(AddArtifactUsageCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedArtifactUsageEvt(cmd.getId(), cmd.getKey(), cmd.getCorrId()));
    }


    // -------------------------------- Event Handlers --------------------------------


    @EventSourcingHandler
    public void on(CreatedDefaultWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        evt.getArtifacts().stream()
                .map(a -> new ArtifactUsage(a.getKey(), Usage.WF_OUTPUT, "INPUT", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET))) // FIXME role and artifacttype are hardcoded in WFIWrapper setArtifact
                .forEach(u -> artifactUsages.add(u));
    }

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        evt.getArtifacts().stream()
                .map(a -> new ArtifactUsage(a.getKey(), Usage.WF_OUTPUT, "INPUT", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET))) // FIXME role and artifacttype are hardcoded in WFIWrapper setArtifact
                .forEach(u -> artifactUsages.add(u));
    }

    @EventSourcingHandler
    public void on(CreatedSubWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        parentWfiId = evt.getParentWfiId();
        parentWftId = evt.getParentWftId();
    }

    @EventSourcingHandler
    public void on(AddedInputEvt evt) {
        log.debug("[AGG] applying {}", evt);
        IJiraArtifact jiraArtifact = checkIfJiraArtifactInside(evt.getArtifact());
        if (jiraArtifact != null) {
            artifactUsages.add(new ArtifactUsage(jiraArtifact.getKey(), Usage.INPUT, evt.getWftId(), evt.getRole(), evt.getType()));
        }
    }

    @EventSourcingHandler
    public void on(AddedOutputEvt evt) {
        log.debug("[AGG] applying {}", evt);
        IJiraArtifact jiraArtifact = checkIfJiraArtifactInside(evt.getArtifact());
        if (jiraArtifact != null) {
            artifactUsages.add(new ArtifactUsage(jiraArtifact.getKey(), Usage.OUTPUT, evt.getWftId(), evt.getRole(), evt.getType()));
        }
    }

    @EventSourcingHandler
    public void on(AddedInputToWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        IJiraArtifact jiraArtifact = checkIfJiraArtifactInside(evt.getInput().getArtifact());
        if (jiraArtifact != null) {
            artifactUsages.add(new ArtifactUsage(jiraArtifact.getKey(), Usage.WF_INPUT, evt.getInput().getRole(), evt.getInput().getArtifactType()));
        }
    }

    @EventSourcingHandler
    public void on(AddedOutputToWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        IJiraArtifact jiraArtifact = checkIfJiraArtifactInside(evt.getOutput().getArtifact());
        if (jiraArtifact != null) {
            artifactUsages.add(new ArtifactUsage(jiraArtifact.getKey(), Usage.WF_OUTPUT, evt.getOutput().getRole(), evt.getOutput().getArtifactType()));
        }
    }

    private IJiraArtifact checkIfJiraArtifactInside(Artifact artifact) {
        if (artifact instanceof ArtifactWrapper) {
            ArtifactWrapper artifactWrapper = (ArtifactWrapper) artifact;
            if (artifactWrapper.getWrappedArtifact() instanceof IJiraArtifact) {
                return (IJiraArtifact) artifactWrapper.getWrappedArtifact();
            }
        }
        return null;
    }

    @EventSourcingHandler
    public void on(AddedArtifactUsageEvt evt) {
        log.debug("[AGG] applying {}", evt);
        artifactUsages.add(new ArtifactUsage(evt.getKey(), Usage.RESOURCE, evt.getCorrId()));
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
