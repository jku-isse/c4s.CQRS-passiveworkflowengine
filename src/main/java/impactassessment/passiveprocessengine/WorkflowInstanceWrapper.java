package impactassessment.passiveprocessengine;

import impactassessment.SpringUtil;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.passiveprocessengine.definition.*;
import impactassessment.passiveprocessengine.instance.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
        AbstractWorkflowDefinition wfd = SpringUtil.getBean(AbstractWorkflowDefinition.class);
        initWfi(wfd, evt.getArtifact());
    }

    private void handle(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt) {
        initWfi(evt.getWfd(), evt.getArtifact());
    }

    private void initWfi(AbstractWorkflowDefinition wfd, IJiraArtifact artifact) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(artifact.getKey()); // TODO use internal ID
        wfi.addOrReplaceProperty(PROP_ID, artifact.getId());
        wfi.addOrReplaceProperty(PROP_ISSUE_TYPE, artifact.getIssueType().getName());
        wfi.addOrReplaceProperty(PROP_PRIORITY, artifact.getPriority() == null ? "" : artifact.getPriority().getName());
        wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private void handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        dni.completedDataflowInvolvingActivationPropagation();
        dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks().stream()
            .forEach(td -> {
                log.debug("[MOD] Upon DNI {} completion, trigger progress by Instantiating Tasktype {}", dni.getDefinition().getId(), td.getId());
                WorkflowTask wt = wfi.instantiateTask(td);
                wfi.activateDecisionNodesFromTask(wt);
                dni.consumeTaskForUnconnectedOutBranch(wt); // connect this task to the decision node instance on one of the outbranches
                log.debug("[MOD] Input Conditions for task fullfilled: "+wt.toString());
            });
        dni.executeMapping();
    }

    private void handle(ActivatedInBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (dni != null && wft != null) {
            dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft));
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
            dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft));
            dni.activateOutBranch(evt.getBranchId());
        }
    }

    private void handle(ActivatedInOutBranchesEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (dni != null && wft != null) {
            dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft));
            String[] branchIds = new String[evt.getBranchIds().size()];
            dni.activateOutBranches(evt.getBranchIds().toArray(branchIds));
        }
    }

    private void handle(AddedConstraintsEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft != null) {
            QACheckDocument qa = new QACheckDocument("QA-"+wft.getTaskType().getId()+"-" + wft.getWorkflow().getId(), wft.getWorkflow());
            WorkflowTask.ArtifactOutput ao = new WorkflowTask.ArtifactOutput(qa, "QA_PROCESS_CONSTRAINTS_CHECK", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
            wft.addOutput(ao);
            CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
            qa.setLastChangeDueTo(corr);
            Map<String, String> rules = evt.getRules();
            for (Map.Entry<String, String> e : rules.entrySet()) {
                String rebcId = e.getKey()+"_"+wft.getTaskType().getId()+"_"+ wft.getWorkflow().getId();
                RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, e.getKey(), wft.getWorkflow(), e.getValue());
                qa.addConstraint(rebc);
            }
        }
    }

    private void handle(AddedEvaluationResultToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        for (Map.Entry<ResourceLink, Boolean> entry : evt.getRes().entrySet()){
            if (!entry.getValue() && !rebc.getUnsatisfiedForReadOnly().contains(entry.getKey())) {
                rebc.addAs(entry.getValue(), entry.getKey());
                rebc.setLastChanged(evt.getTime());
            }
            if (entry.getValue() && !rebc.getFulfilledForReadOnly().contains(entry.getKey())) {
                rebc.addAs(entry.getValue(), entry.getKey());
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
        } else if (evt instanceof ActivatedInOutBranchesEvt) {
            handle((ActivatedInOutBranchesEvt) evt);
        } else if (evt instanceof AddedConstraintsEvt) {
            handle((AddedConstraintsEvt) evt);
        } else if (evt instanceof AddedEvaluationResultToConstraintEvt) {
            handle((AddedEvaluationResultToConstraintEvt) evt);
        } else {
            log.error("[MOD] Unknown message type: "+evt.getClass().getSimpleName());
        }
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

    @Override
    public String toString() {
        return wfi.toString();
    }

}
