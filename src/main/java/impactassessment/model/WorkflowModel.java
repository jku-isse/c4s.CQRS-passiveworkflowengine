package impactassessment.model;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.DronologyWorkflow;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.WPManagementWorkflow;
import impactassessment.model.workflowmodel.*;
import lombok.extern.slf4j.XSlf4j;

import java.util.*;

@XSlf4j
public class WorkflowModel {

    private WorkflowInstance wfi;

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }
/*
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
*/
    public void handle(AddedArtifactEvt evt) {
        Artifact artifact = evt.getArtifact();
        log.debug("Artifact ID: "+artifact.getId());
        DronologyWorkflow wfd = new DronologyWorkflow();
        wfd.initWorkflowSpecification();
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = wfd.createInstance(artifact.getId());
        wfi.addOrReplaceProperty("ID", artifact.getId());
        wfi.addOrReplaceProperty("Issue Type", artifact.getField("issuetype"));
        if (!artifact.getField("issuetype").equals("Hazard")) {
            wfi.addOrReplaceProperty("Priority", "" + artifact.getField("priority"));
        }
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    public void handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = getDecisionNodeInstance(evt.getDniId()).get();
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        tds.stream()
            .forEach(td -> {
                log.debug(String.format("Upon DNI %s completion, trigger progress by Instantiating Tasktype %s ", dni.getDefinition().getId(), td.toString()));
                WorkflowTask wt = wfi.instantiateTask(td);
                wt.addOutput(new WorkflowTask.ArtifactOutput(MockService.getHumanReadableResourceLinkEndpoint(evt.getArtifact()), DronologyWorkflow.INPUT_ROLE_WPTICKET ));
                wt.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
                Set<AbstractWorkflowInstanceObject> newDNIs = wfi.activateDecisionNodesFromTask(wt);
                dni.consumeTaskForUnconnectedOutBranch(wt); // connect this task to the decision node instance on one of the outbranches
                log.debug("Input Conditions for task fullfilled: "+wt.toString());
            });
    }

    public void handle(ActivatedInBranchEvt evt) {
        Optional<DecisionNodeInstance> optDni = getDecisionNodeInstance(evt.getDniId());
        Optional<WorkflowTask> optWft = getWorkflowTask(evt.getWftId());
        if (optDni.isPresent() && optWft.isPresent()) {
            DecisionNodeInstance dni = optDni.get();
            WorkflowTask wft = optWft.get();
            dni.activateInBranch(dni.getInBranchForWorkflowTask(wft));
        }
    }

    public void handle(ActivatedOutBranchEvt evt) {
        Optional<DecisionNodeInstance> optDni = getDecisionNodeInstance(evt.getDniId());
        optDni.ifPresent(dni -> dni.activateOutBranch(evt.getBranchId()));
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof AddedArtifactEvt) {
            handle((AddedArtifactEvt) evt);
        } else if (evt instanceof CompletedDataflowEvt) {
            handle((CompletedDataflowEvt) evt);
        } else if (evt instanceof ActivatedInBranchEvt) {
            handle((ActivatedInBranchEvt) evt);
        } else if (evt instanceof ActivatedOutBranchEvt) {
            handle((ActivatedOutBranchEvt) evt);
        }

        /*else if (evt instanceof CreatedWorkflowInstanceOfEvt) {
            handle((CreatedWorkflowInstanceOfEvt) evt);
        } else if (evt instanceof EnabledTasksAndDecisionsEvt) {
            handle((EnabledTasksAndDecisionsEvt) evt);
        } else if (evt instanceof CompletedDataflowOfDecisionNodeInstanceEvt) {
            handle((CompletedDataflowOfDecisionNodeInstanceEvt) evt);
        } else if (evt instanceof AddedQAConstraintsAsArtifactOutputsEvt) {
            handle((AddedQAConstraintsAsArtifactOutputsEvt) evt);
        } else if (evt instanceof CreatedConstraintTriggerEvt) {
            handle((CreatedConstraintTriggerEvt) evt);
        } */else {
            log.error("Unknown message type: "+evt.getClass().getSimpleName());
        }
    }

    public void reset() {
        wfi = null;
    }

    private Optional<DecisionNodeInstance> getDecisionNodeInstance(String id) {
        return wfi.getDecisionNodeInstancesReadonly().stream().filter(x -> x.getId().equals(id)).findFirst();
    }

    private Optional<WorkflowTask> getWorkflowTask(String id) {
        return wfi.getWorkflowTasksReadonly().stream().filter(x -> x.getId().equals(id)).findFirst();
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
