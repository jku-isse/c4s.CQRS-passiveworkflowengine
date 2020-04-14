package impactassessment.model;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.workflowmodel.*;
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

    public QACheckDocument.QAConstraint handle(AddedQAConstraintsAsArtifactOutputsEvt evt){
        QACheckDocument qacd = new QACheckDocument("QA-"+wfi.getId(), wfi);
        qacd.setWorkflow(wfi);
        QACheckDocument.QAConstraint qac = evt.getQac();
        qac.setWorkflow(wfi);
        qacd.addConstraint(qac);
        wfi.getWorkflowTasksReadonly().stream().forEach(wft -> {
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qacd, "QA_PROCESS_CONSTRAINTS_CHECK");
            wft.addOutput(ao);
        });
        return qac;
    }

    public ConstraintTrigger handle(CreatedConstraintTriggerEvt evt) {
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
        } else if (evt instanceof AddedQAConstraintsAsArtifactOutputsEvt) {
            handle((AddedQAConstraintsAsArtifactOutputsEvt) evt);
        } else if (evt instanceof CreatedConstraintTriggerEvt) {
            handle((CreatedConstraintTriggerEvt) evt);
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
