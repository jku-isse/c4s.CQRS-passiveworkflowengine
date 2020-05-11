package impactassessment.model;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.mock.artifact.Artifact;
import impactassessment.mock.artifact.MockService;
import impactassessment.model.definition.DronologyWorkflow;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.model.workflowmodel.*;
import lombok.extern.slf4j.XSlf4j;

import java.util.*;
import java.util.stream.Collectors;

@XSlf4j
public class WorkflowInstanceWrapper {

    private WorkflowInstance wfi;

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    private void handle(AddedArtifactEvt evt) {
        Artifact artifact = evt.getArtifact();
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

    private List<String> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = getDecisionNodeInstance(evt.getDniId()).get();
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        var newDNIs = new ArrayList<AbstractWorkflowInstanceObject>();
        tds.stream()
            .forEach(td -> {
                log.debug(String.format("[MOD] Upon DNI %s completion, trigger progress by Instantiating Tasktype %s ", dni.getDefinition().getId(), td.toString()));
                WorkflowTask wt = wfi.instantiateTask(td);
                wt.addOutput(new WorkflowTask.ArtifactOutput(MockService.getHumanReadableResourceLinkEndpoint(evt.getArtifact()), DronologyWorkflow.INPUT_ROLE_WPTICKET ));
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

    private void handle(AppendedQACheckDocumentEvt evt) {
        Optional<WorkflowTask> optWft = getWorkflowTask(evt.getWftId());
        if (optWft.isPresent()) {
            WorkflowTask wft = optWft.get();
            QACheckDocument qa = new QACheckDocument("QA-"+evt.getState()+"-" + wft.getWorkflow().getId(), wft.getWorkflow());
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qa, "QA-"+evt.getState()+"-CONSTRAINTS-CHECK-" + wft.getWorkflow().getId());
            wft.addOutput(ao);
            CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
            qa.setLastChangeDueTo(corr);
        }
    }

    private void handle(AddedQAConstraintEvt evt) {
        Optional<WorkflowTask> optWft = getWorkflowTask(evt.getWftId());
        if (optWft.isPresent()) {
            WorkflowTask wft = optWft.get();
            QACheckDocument qa = getQACDocFor(evt.getWftId());
            RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(evt.getConstrPrefix() + wft.getWorkflow().getId(), qa, evt.getRuleName(), wft.getWorkflow(), evt.getDescription());
            qa.addConstraint(rebc);
        }

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
        } else if (evt instanceof AppendedQACheckDocumentEvt) {
            handle((AppendedQACheckDocumentEvt) evt);
        } else if (evt instanceof AddedQAConstraintEvt) {
            handle((AddedQAConstraintEvt) evt);
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

    public QACheckDocument getQACDocFor(String wfiId) {
        Optional<WorkflowTask> optWft = getWorkflowTask(wfiId);
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
