package impactassessment.query;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.workflowmodel.*;
import impactassessment.workflowmodel.definition.ConstraintTrigger;
import impactassessment.workflowmodel.definition.QACheckDocument;
import impactassessment.workflowmodel.definition.RuleEngineBasedConstraint;
import impactassessment.workflowmodel.definition.WPManagementWorkflow;
import lombok.extern.slf4j.XSlf4j;

import java.util.*;

@XSlf4j
public class WorkflowModel {

    private WorkflowInstance wfi;

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    public void handle(CreatedWorkflowInstanceOfEvt evt){
        WorkflowDefinition wfd = evt.getWfd();
        wfi = wfd.createInstance(evt.getId());
    }

    public void handle(EnabledTasksAndDecisionsEvt evt){
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    public void handle(CompletedDataflowOfDecisionNodeInstanceEvt evt){
        List<DecisionNodeInstance> dnis = new ArrayList<>(wfi.getDecisionNodeInstancesReadonly());
        DecisionNodeInstance dni = dnis.get(evt.getDniIndex());
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForNonDisabledOutBranchesWithUnresolvedTasks();
        tds.stream().forEach(td -> {
            WorkflowTask wft = wfi.instantiateTask(td);
            wft.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
            wfi.activateDecisionNodesFromTask(wft);
            dni.consumeTaskForUnconnectedOutBranch(wft);
        });
    }

    public ConstraintTrigger handle(AddedQACheckDocumentsArtifactOutputsEvt evt){
        wfi.getWorkflowTasksReadonly().stream().forEach(wft -> {
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(evt.getQacd(), "QA_PROCESS_CONSTRAINTS_CHECK");
            wft.addOutput(ao);
        });
        ConstraintTrigger ct = new ConstraintTrigger(wfi, new CorrelationTuple(evt.getId(), "QualityCheckRequest"));
        ct.addConstraint("*");
        return ct;
    }

//    QACheckDocument qa = new QACheckDocument("QA1", wfi);
//    int itemId = 1;
//    RuleEngineBasedConstraint srsConstraint = new RuleEngineBasedConstraint("REBC2", qa, "CheckSWRequirementReleased", wfi, "Have all SRSs of the WP been released?");
//        qa.addConstraint(srsConstraint);
//        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=11", "self", "", "html", "SRS 11"));
//        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=12", "self", "", "html", "SRS 12"));
//        srsConstraint.addAs(true, new ResourceLink("SRS", "http://testjama.frequentis/item=13", "self", "", "html", "SRS 13"));
//        srsConstraint.addAs(false, new ResourceLink("SRS", "http://testjama.frequentis/item=14", "self", "", "html", "SRS 14"));
//        srsConstraint.addAs(false, new ResourceLink("SRS", "http://testjama.frequentis/item=15", "self", "", "html", "SRS 15"));

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof CreatedWorkflowInstanceOfEvt) {
            handle((CreatedWorkflowInstanceOfEvt) evt);
        }  else if (evt instanceof EnabledTasksAndDecisionsEvt) {
            handle((EnabledTasksAndDecisionsEvt) evt);
        } else if (evt instanceof CompletedDataflowOfDecisionNodeInstanceEvt) {
            handle((CompletedDataflowOfDecisionNodeInstanceEvt) evt);
        } else if (evt instanceof AddedQACheckDocumentsArtifactOutputsEvt) {
            handle((AddedQACheckDocumentsArtifactOutputsEvt) evt);
        } else {
            log.error("Unknown message type: "+evt.getClass().getSimpleName());
        }
    }

    public void reset() {
        wfi = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowModel)) return false;
        WorkflowModel that = (WorkflowModel) o;
        return Objects.equals(wfi, that.wfi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfi);
    }

    public String toString() {
        return wfi.toString();
    }


}
