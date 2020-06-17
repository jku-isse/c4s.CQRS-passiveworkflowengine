package impactassessment.model;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.artifact.base.IArtifact;
import impactassessment.artifact.mock.MockService;
import impactassessment.model.definition.DronologyWorkflow;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.model.workflowmodel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class WorkflowInstanceWrapper {

    private WorkflowInstance wfi;

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    private void handle(AddedMockArtifactEvt evt) {
        IArtifact artifact = evt.getArtifact();
        DronologyWorkflow wfd = new DronologyWorkflow();
        wfd.initWorkflowSpecification();
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = wfd.createInstance(artifact.getId().toString());
        wfi.addOrReplaceProperty("ID", artifact.getId().toString());
        wfi.addOrReplaceProperty("Issue Type", artifact.getIssueType().getName());
        if (!artifact.getIssueType().getName().equals("Hazard")) {
            wfi.addOrReplaceProperty("Priority", "" + artifact.getPriority().getName());
        }
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private void handle(AddedArtifactEvt evt) {
        IArtifact artifact = evt.getArtifact();
        DronologyWorkflow wfd = new DronologyWorkflow();
        wfd.initWorkflowSpecification();
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = wfd.createInstance(artifact.getId().toString());
        wfi.addOrReplaceProperty("ID", artifact.getId().toString());
        wfi.addOrReplaceProperty("Issue Type", artifact.getIssueType().getName());
        if (!artifact.getIssueType().getName().equals("Hazard")) {
            wfi.addOrReplaceProperty("Priority", "" + artifact.getPriority().getName());
        }
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private List<String> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = getDecisionNodeInstance(evt.getDniId()).get();
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        var newDNIs = new ArrayList<AbstractWorkflowInstanceObject>();
        tds.stream()
            .forEach(td -> {
                log.debug(String.format("[MOD] Upon DNI %s completion, trigger progress by Instantiating Tasktype %s ", dni.getDefinition().getId(), td.toString()));
                WorkflowTask wt = wfi.instantiateTask(td);
                wt.addOutput(new WorkflowTask.ArtifactOutput(ResourceLink.of(evt.getArtifact()), DronologyWorkflow.INPUT_ROLE_WPTICKET ));
                wt.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
                newDNIs.addAll(wfi.activateDecisionNodesFromTask(wt));
                dni.consumeTaskForUnconnectedOutBranch(wt); // connect this task to the decision node instance on one of the outbranches
                log.debug("[MOD] Input Conditions for task fullfilled: "+wt.toString());
            });
        List<String> newDNIIds = newDNIs.stream().map(d -> d.getId()).collect(Collectors.toList());
        return newDNIIds;
    }

    private void handle(ActivatedInBranchEvt evt) {
        Optional<DecisionNodeInstance> optDni = getDecisionNodeInstance(evt.getDniId());
        Optional<WorkflowTask> optWft = getWorkflowTask(evt.getWftId());
        if (optDni.isPresent() && optWft.isPresent()) {
            DecisionNodeInstance dni = optDni.get();
            WorkflowTask wft = optWft.get();
            dni.activateInBranch(dni.getInBranchForWorkflowTask(wft));
        }
    }

    private void handle(ActivatedOutBranchEvt evt) {
        Optional<DecisionNodeInstance> optDni = getDecisionNodeInstance(evt.getDniId());
        optDni.ifPresent(dni -> dni.activateOutBranch(evt.getBranchId()));
    }

    private void handle(AddedQAConstraintEvt evt) {
        Optional<WorkflowTask> optWft = getWorkflowTask(evt.getWftId());
        if (optWft.isPresent()) {
            WorkflowTask wft = optWft.get();
            QACheckDocument qa = getQACDocOfWft(evt.getWftId());
            if (qa == null) {
                //create and append new QACheckDocument to WFT
                qa = new QACheckDocument("QA-"+evt.getState()+"-" + wft.getWorkflow().getId(), wft.getWorkflow());
                WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qa, "QA_PROCESS_CONSTRAINTS_CHECK");
                wft.addOutput(ao);
                CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
                qa.setLastChangeDueTo(corr);
            }
            RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(evt.getConstrPrefix() + wft.getWorkflow().getId(), qa, evt.getRuleName(), wft.getWorkflow(), evt.getDescription());
            qa.addConstraint(rebc);
        }
    }

    private void handle(AddedResourceToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        if (!evt.getFulfilled() && !rebc.getUnsatisfiedForReadOnly().contains(evt.getRes())) {
            rebc.addAs(evt.getFulfilled(), evt.getRes());
            rebc.setLastChanged(evt.getTime());
        }
        if (evt.getFulfilled() && !rebc.getFulfilledForReadOnly().contains(evt.getRes())) {
            rebc.addAs(evt.getFulfilled(), evt.getRes());
            rebc.setLastChanged(evt.getTime());
        }
        rebc.setLastEvaluated(evt.getTime());
        rebc.setEvaluated(evt.getCorr());
    }

    private void handle(AddedResourcesToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        for (ResourceLink rl : evt.getRes()){
            if (!evt.getFulfilled() && !rebc.getUnsatisfiedForReadOnly().contains(rl)) {
                rebc.addAs(evt.getFulfilled(), rl);
                rebc.setLastChanged(evt.getTime());
            }
            if (evt.getFulfilled() && !rebc.getFulfilledForReadOnly().contains(rl)) {
                rebc.addAs(evt.getFulfilled(), rl);
                rebc.setLastChanged(evt.getTime());
            }
        }
        rebc.setLastEvaluated(evt.getTime());
        rebc.setEvaluated(evt.getCorr());
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof AddedMockArtifactEvt) {
            handle((AddedMockArtifactEvt) evt);
        } else if (evt instanceof AddedArtifactEvt) {
            handle((AddedArtifactEvt) evt);
        } else if (evt instanceof CompletedDataflowEvt) {
            handle((CompletedDataflowEvt) evt);
        } else if (evt instanceof ActivatedInBranchEvt) {
            handle((ActivatedInBranchEvt) evt);
        } else if (evt instanceof ActivatedOutBranchEvt) {
            handle((ActivatedOutBranchEvt) evt);
        } else if (evt instanceof AddedQAConstraintEvt) {
            handle((AddedQAConstraintEvt) evt);
        } else if (evt instanceof AddedResourceToConstraintEvt) {
            handle((AddedResourceToConstraintEvt) evt);
        } else if (evt instanceof AddedResourcesToConstraintEvt) {
            handle((AddedResourcesToConstraintEvt) evt);
        } else {
            log.error("[MOD] Unknown message type: "+evt.getClass().getSimpleName());
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

    public QACheckDocument getQACDocOfWft(String wftId) {
        Optional<WorkflowTask> optWft = getWorkflowTask(wftId);
        Optional<QACheckDocument> optQACD = Optional.empty();
        if (optWft.isPresent()){
            optQACD = optWft.get().getOutput().stream()
                    .map(ao -> ao.getArtifact())
                    .filter(ao -> ao instanceof QACheckDocument)
                    .map(a -> (QACheckDocument) a)
                    .findAny();
        }
        return optQACD.orElse(null);
    }

    public RuleEngineBasedConstraint getQAC(String qacId) {
        for (WorkflowTask wft : wfi.getWorkflowTasksReadonly()) {
            for (WorkflowTask.ArtifactOutput ao : wft.getOutput()) {
                if (ao.getArtifact() instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) ao.getArtifact();
                    for (QACheckDocument.QAConstraint qac : qacd.getConstraintsReadonly()) {
                        if (qac.getId().equals(qacId)) {
                            if (qac instanceof RuleEngineBasedConstraint) {
                                return (RuleEngineBasedConstraint) qac;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowInstanceWrapper)) return false;
        WorkflowInstanceWrapper that = (WorkflowInstanceWrapper) o;
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
