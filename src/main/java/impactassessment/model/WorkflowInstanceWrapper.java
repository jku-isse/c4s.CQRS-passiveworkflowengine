package impactassessment.model;

import impactassessment.analytics.CorrelationTuple;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.model.definition.ArtifactTypes;
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

    public static final String PROP_ID = "ID";
    public static final String PROP_ISSUE_TYPE = "Issue Type";
    public static final String PROP_PRIORITY = "Priority";

    private void handle(ImportedOrUpdatedArtifactEvt evt) {
        initWfi(new DronologyWorkflow(), evt.getArtifact());
    }

    private void handle(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt) {
        initWfi(evt.getWfd(), evt.getArtifact());
    }

    private void initWfi(AbstractWorkflowDefinition wfd, IJiraArtifact artifact) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(artifact.getKey()); // TODO internal ID
        wfi.addOrReplaceProperty(PROP_ID, artifact.getId());
        wfi.addOrReplaceProperty(PROP_ISSUE_TYPE, artifact.getIssueType().getName());
        if (!artifact.getIssueType().getName().equals("Hazard")) {
            wfi.addOrReplaceProperty(PROP_PRIORITY, "" + artifact.getPriority().getName());
        }
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private List<String> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        dni.completedDataflowInvolvingActivationPropagation();
        List<TaskDefinition> tds = dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks();
        var newDNIs = new ArrayList<AbstractWorkflowInstanceObject>();
        tds.stream()
            .forEach(td -> {
                log.debug(String.format("[MOD] Upon DNI %s completion, trigger progress by Instantiating Tasktype %s ", dni.getDefinition().getId(), td.toString()));
                WorkflowTask wt = wfi.instantiateTask(td);
                wt.addOutput(new WorkflowTask.ArtifactOutput(evt.getRes(), DronologyWorkflow.ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK))); // TODO remove hardcoded Dronology Workflow
                wt.signalEvent(TaskLifecycle.Events.INPUTCONDITIONS_FULFILLED);
                newDNIs.addAll(wfi.activateDecisionNodesFromTask(wt));
                dni.consumeTaskForUnconnectedOutBranch(wt); // connect this task to the decision node instance on one of the outbranches
                log.debug("[MOD] Input Conditions for task fullfilled: "+wt.toString());
            });
        dni.executeMapping();
        List<String> newDNIIds = newDNIs.stream().map(d -> d.getId()).collect(Collectors.toList());
        return newDNIIds;
    }

    private void handle(ActivatedInBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (dni != null && wft != null) {
            dni.activateInBranch(dni.getInBranchForWorkflowTask(wft));
        }
    }

    private void handle(ActivatedOutBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        if (dni != null) {
            dni.activateOutBranch(evt.getBranchId());
        }
    }

    private void handle(ActivatedInOutBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (dni != null && wft != null) {
            dni.activateInBranch(dni.getInBranchForWorkflowTask(wft));
            dni.activateOutBranch(evt.getBranchId());
        }
    }

    private void handle(AddedQAConstraintEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft != null) {
            QACheckDocument qa = getQACDocOfWft(evt.getWftId());
            if (qa == null) {
                //create and append new QACheckDocument to WFT
                qa = new QACheckDocument("QA-"+evt.getStatus()+"-" + wft.getWorkflow().getId(), wft.getWorkflow());
                WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qa, "QA_PROCESS_CONSTRAINTS_CHECK", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
                wft.addOutput(ao);
                CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
                qa.setLastChangeDueTo(corr);
            }
            String rebcId = evt.getRuleName() +"_"+evt.getStatus() +"_"+ wft.getWorkflow().getId();
            RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, evt.getRuleName(), wft.getWorkflow(), evt.getDescription());
            qa.addConstraint(rebc);
        }
    }

    private void handle(AddedResourceToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        if (rebc != null) {
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
        if (evt instanceof ImportedOrUpdatedArtifactEvt) {
            handle((ImportedOrUpdatedArtifactEvt) evt);
        } else if (evt instanceof ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt) {
            handle((ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt) evt);
        } else if (evt instanceof CompletedDataflowEvt) {
            handle((CompletedDataflowEvt) evt);
        } else if (evt instanceof ActivatedInBranchEvt) {
            handle((ActivatedInBranchEvt) evt);
        } else if (evt instanceof ActivatedOutBranchEvt) {
            handle((ActivatedOutBranchEvt) evt);
        } else if (evt instanceof ActivatedInOutBranchEvt) {
            handle((ActivatedInOutBranchEvt) evt);
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

    public QACheckDocument getQACDocOfWft(String wftId) {
        WorkflowTask wft = wfi.getWorkflowTask(wftId);
        Optional<QACheckDocument> optQACD = Optional.empty();
        if (wft != null){
            optQACD = wft.getOutput().stream()
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
