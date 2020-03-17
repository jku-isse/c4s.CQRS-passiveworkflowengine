package impactassessment.query;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.rulebase.RuleBaseFactory;
import impactassessment.rulebase.RuleBaseService;
import impactassessment.workflowmodel.*;
import impactassessment.workflowmodel.definition.ConstraintTrigger;
import impactassessment.workflowmodel.definition.QACheckDocument;
import impactassessment.workflowmodel.definition.RuleEngineBasedConstraint;
import impactassessment.workflowmodel.definition.WPManagementWorkflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@XSlf4j
public class WorkflowModel {

    private WorkflowInstance wfi;

    private RuleBaseService ruleService;

    @Autowired
    public void setRuleService(RuleBaseService ruleService) {
        this.ruleService = ruleService;
    }

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    public void handle(CreatedWorkflowEvt evt) {
        WPManagementWorkflow workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = workflow.createInstance(evt.getId());
        ruleService.insertAndFire(wfi); // TODO how to ensure KB does't fire commands when it is replayed?
    }

    public void handle(EnabledEvt evt) {
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

    public void handle(CompletedEvt evt) {
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
        ruleService.insertAndFire(ct); // TODO how to ensure KB does't fire commands when it is replayed?
    }

    public void handle(TrackedEventMessage<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof CreatedWorkflowEvt) {
            handle((CreatedWorkflowEvt)payload);
        } else if (payload instanceof EnabledEvt) {
            handle((EnabledEvt)payload);
        } else if (payload instanceof CompletedEvt) {
            handle((CompletedEvt)payload);
        } else {
            log.error("unknown event: {}", payload.getClass().getSimpleName());
        }
    }

    public void reset() {
        wfi = null;
    }

    public String toString() {
        return wfi.toString();
    }
}
