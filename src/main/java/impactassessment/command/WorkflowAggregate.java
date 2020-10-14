package impactassessment.command;

import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.mock.JiraMockService;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.passiveprocessengine.instance.*;
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
    public void handle(AddMockArtifactCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        IJiraArtifact a = JiraMockService.mockArtifact(cmd.getId(), cmd.getStatus(), cmd.getIssuetype(), cmd.getPriority(), cmd.getSummary());
        apply(new ImportedOrUpdatedArtifactEvt(cmd.getId(), a));
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ImportOrUpdateArtifactCmd cmd, IJiraArtifactService artifactService) {
        log.info("[AGG] handling {}", cmd);
        if (cmd.getSource().equals(Sources.JIRA)) {
            IJiraArtifact a = artifactService.get(cmd.getId());
            if (a != null) {
                apply(new ImportedOrUpdatedArtifactEvt(cmd.getId(), a));
            }
        } else {
            log.error("Unsupported Artifact source: "+cmd.getSource());
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ImportOrUpdateArtifactWithWorkflowDefinitionCmd cmd, IJiraArtifactService artifactService, WorkflowDefinitionRegistry registry) {
        log.info("[AGG] handling {}", cmd);
        if (cmd.getSource().equals(Sources.JIRA)) {
            IJiraArtifact a = artifactService.get(cmd.getId());
            if (a != null) {
                WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
                wfdContainer.setKieContainer(null);
                if (wfdContainer != null) {
                    apply(new ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(cmd.getId(), a, wfdContainer));
                } else {
                    log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
                }
            }
        } else {
            log.error("Unsupported Artifact source: "+cmd.getSource());
        }
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(CreateChildWorkflowCmd cmd, WorkflowDefinitionRegistry registry) {
        log.info("[AGG] handling {}", cmd);
        WorkflowDefinitionContainer wfdContainer = registry.get(cmd.getDefinitionName());
        if (wfdContainer != null)
            apply(new CreatedChildWorkflowEvt(cmd.getId(), cmd.getParentWfiId(), cmd.getParentWftId(), wfdContainer));
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
    public void handle(AddAsInputCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedAsInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()));
    }

    @CommandHandler
    public void handle(AddAsOutputCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedAsOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifact(), cmd.getRole(), cmd.getType()));
        if (parentWfiId != null && parentWftId != null) {
            apply(new AddedAsOutputEvt(parentWfiId, parentWftId, cmd.getArtifact(), cmd.getRole(), cmd.getType()));
        }
    }

    @CommandHandler
    public void handle(AddAsInputToWfiCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        log.error("******************* "+cmd.getId());
        apply(new AddedAsInputToWfiEvt(cmd.getId(), cmd.getInput()));

        if (cmd.getInput().getArtifact() instanceof ArtifactWrapper) {
            ArtifactWrapper artWrapper = (ArtifactWrapper) cmd.getInput().getArtifact();
            if (artWrapper.getWrappedArtifact() instanceof IJiraArtifact) {
                IJiraArtifact iJira = (IJiraArtifact) artWrapper.getWrappedArtifact();
                log.error("*******************2 "+cmd.getId());
                apply(new ImportedOrUpdatedArtifactEvt(cmd.getId(), iJira));
            }
        }
    }

    @CommandHandler
    public void handle(AddAsOutputToWfiCmd cmd) {
        log.info("[AGG] handling {}", cmd);
        apply(new AddedAsOutputToWfiEvt(cmd.getId(), cmd.getOutput()));
    }


    // -------------------------------- Event Handlers --------------------------------


    @EventSourcingHandler
    public void on(ImportedOrUpdatedArtifactEvt evt) {
        log.debug("[AGG] applying {}", evt);
        if (model == null || id == null) { // CREATE
            id = evt.getId();
            model = new WorkflowInstanceWrapper();
            model.handle(evt);
        }
        model.setArtifact(evt.getArtifact()); // UPDATE
    }

    @EventSourcingHandler
    public void on(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt) {
        log.debug("[AGG] applying {}", evt);
        if (model == null || id == null) { // CREATE
            id = evt.getId();
            model = new WorkflowInstanceWrapper();
            model.handle(evt);
        }
        model.setArtifact(evt.getArtifact()); // UPDATE
    }

    @EventSourcingHandler
    public void on(CreatedChildWorkflowEvt evt) {
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
