package impactassessment.command;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
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
public class Workflow {

    @AggregateIdentifier
    private String id;
    private WorkflowInstance wfi;

    public Workflow() {
        log.debug("empty constructor invoked");
    }

    // Command Handlers

    @CommandHandler
    public Workflow(CreateWorkflowCmd cmd) {
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

        // TODO use WorkflowModel to prevent duplicate code

        log.debug("applying {}", evt);
        id = evt.getId();
        WPManagementWorkflow workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = workflow.createInstance(id);
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(wfi);
        }
    }

    @EventSourcingHandler
    public void on(EnabledEvt evt) {

        // TODO use WorkflowModel to prevent duplicate code

        log.debug("applying {}", evt);
        List<AbstractWorkflowInstanceObject> awos = wfi.enableWorkflowTasksAndDecisionNodes();
        ArtifactWrapper ticketArt1 = new ArtifactWrapper("TICKET1", WPManagementWorkflow.ARTIFACT_TYPE_JIRA_TICKET, null, null);
        DecisionNodeInstance dni = (DecisionNodeInstance) awos.get(evt.getDniNumber());
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForNonDisabledOutBranchesWithUnresolvedTasks();
        tds.stream().forEach(td -> {
            WorkflowTask wft = wfi.instantiateTask(td);
            wft.addInput(new WorkflowTask.ArtifactInput(ticketArt1, WPManagementWorkflow.INPUT_ROLE_WPTICKET));
            wft.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
            Set<AbstractWorkflowInstanceObject> newDNIs = wfi.activateDecisionNodesFromTask(wft);
            dni.consumeTaskForUnconnectedOutBranch(wft); // connect this task to the decision node instance on one of the outbranches
        });
    }

    @EventSourcingHandler
    public void on(CompletedEvt evt, ReplayStatus status, RuleBaseService ruleBaseService) {

        // TODO use WorkflowModel to prevent duplicate code

        QACheckDocument qa = new QACheckDocument("QA1", wfi);
        int itemId = 1;
        RuleEngineBasedConstraint srsConstraint = new RuleEngineBasedConstraint("REBC2", qa, "CheckSWRequirementReleased", wfi, "Have all SRSs of the WP been released?");
        qa.addConstraint(srsConstraint);
        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=11", "self", "", "html", "SRS 11"));
        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=12", "self", "", "html", "SRS 12"));
        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=13", "self", "", "html", "SRS 13"));
        srsConstraint.addAs(false, new ResourceLink("SRS", "http://testjama.frequentis/item=14", "self", "", "html", "SRS 14"));
        srsConstraint.addAs(false, new ResourceLink("SRS", "http://testjama.frequentis/item=15", "self", "", "html", "SRS 15"));
        // add to WFTask
        wfi.getWorkflowTasksReadonly().stream().forEach(wft -> {
            // there is only one here, so lets use this to add the QA document
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qa, "QA_PROCESS_CONSTRAINTS_CHECK");
            wft.addOutput(ao);
        });
        ConstraintTrigger ct = new ConstraintTrigger(wfi, new CorrelationTuple(evt.getId(), "QualityCheckRequest"));
        ct.addConstraint("*");
        if (!status.isReplay()) {
            ruleBaseService.insertAndFire(ct);
        }
    }

}
