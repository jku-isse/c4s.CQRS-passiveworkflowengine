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
        QACheckDocument qacd = evt.getQacd();
        qacd.setWorkflow(wfi);
        wfi.getWorkflowTasksReadonly().stream().forEach(wft -> {
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qacd, "QA_PROCESS_CONSTRAINTS_CHECK");
            wft.addOutput(ao);
        });
        ConstraintTrigger ct = new ConstraintTrigger(wfi, new CorrelationTuple(evt.getId(), "QualityCheckRequest"));
        ct.addConstraint("*");
        return ct;
    }

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
