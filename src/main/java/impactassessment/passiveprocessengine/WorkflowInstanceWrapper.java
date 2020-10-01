package impactassessment.passiveprocessengine;

import impactassessment.SpringUtil;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.passiveprocessengine.definition.*;
import impactassessment.passiveprocessengine.instance.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class WorkflowInstanceWrapper {

    private WorkflowInstance wfi;
    private IJiraArtifact artifact;

    public IJiraArtifact getArtifact() {
        return artifact;
    }

    public void setArtifact(IJiraArtifact artifact) {
        this.artifact = artifact;
    }

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    public static final String PROP_ID = "ID";
    public static final String PROP_ISSUE_TYPE = "Issue Type";
    public static final String PROP_PRIORITY = "Priority";

    public List<AbstractWorkflowInstanceObject> handle(ImportedOrUpdatedArtifactEvt evt) {
        AbstractWorkflowDefinition wfd = SpringUtil.getBean(AbstractWorkflowDefinition.class);
        return initWfi(wfd, evt.getArtifact());
    }

    public List<AbstractWorkflowInstanceObject> handle(ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt evt) {
        return initWfi(evt.getWfd(), evt.getArtifact());
    }

    public List<AbstractWorkflowInstanceObject> handle(CreatedChildWorkflowEvt evt) {
        AbstractWorkflowDefinition wfd = evt.getWfd();
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(evt.getId());
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private List<AbstractWorkflowInstanceObject> initWfi(AbstractWorkflowDefinition wfd, IJiraArtifact artifact) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(artifact.getKey()); // TODO use internal ID
        wfi.addOrReplaceProperty(PROP_ID, artifact.getId());
        wfi.addOrReplaceProperty(PROP_ISSUE_TYPE, artifact.getIssueType().getName());
        wfi.addOrReplaceProperty(PROP_PRIORITY, artifact.getPriority() == null ? "" : artifact.getPriority().getName());
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }

    public Map<WorkflowTask, ArtifactInput> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        dni.completedDataflowInvolvingActivationPropagation();
        // TODO can be removed? already added in ActivateOutBranch
//        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
//        dni.getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks().stream()
//            .forEach(td -> {
//                log.debug("[MOD] Upon DNI {} completion, trigger progress by Instantiating Tasktype {}", dni.getDefinition().getId(), td.getId());
//                WorkflowTask wt = wfi.instantiateTask(td);
//                awos.add(wt);
//                awos.addAll(wfi.activateDecisionNodesFromTask(wt));
//                dni.consumeTaskForUnconnectedOutBranch(wt); // connect this task to the decision node instance on one of the outbranches
//                log.debug("[MOD] Input Conditions for task fullfilled: "+wt.toString());
//            });
        return dni.executeMapping();
    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedInBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null && wft != null) {
            awos.addAll(dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft)));
        }
        return awos;
    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedOutBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null) {
            awos.addAll(dni.activateOutBranch(evt.getBranchId()));
        }
        return awos;
    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedInOutBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null && wft != null) {
            awos.addAll(dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft)));
            awos.addAll(dni.activateOutBranch(evt.getBranchId()));
        }
        return awos;
    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedInOutBranchesEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null && wft != null) {
            awos.addAll(dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft)));
            String[] branchIds = new String[evt.getBranchIds().size()];
            awos.addAll(dni.activateOutBranches(evt.getBranchIds().toArray(branchIds)));
        }
        return awos;
    }

    public List<RuleEngineBasedConstraint> handle(AddedConstraintsEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<RuleEngineBasedConstraint> rebcs = new ArrayList<>();
        if (wft != null) {
            QACheckDocument qa = new QACheckDocument("QA-"+wft.getType().getId()+"-" + wft.getWorkflow().getId(), wft.getWorkflow());
            ArtifactOutput ao = new ArtifactOutput(qa, "QA_PROCESS_CONSTRAINTS_CHECK", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
            wft.addOutput(ao);
            CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
            qa.setLastChangeDueTo(corr);
            Map<String, String> rules = evt.getRules();
            for (Map.Entry<String, String> e : rules.entrySet()) {
                String rebcId = e.getKey()+"_"+wft.getType().getId()+"_"+ wft.getWorkflow().getId();
                RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, e.getKey(), wft.getWorkflow(), e.getValue());
                qa.addConstraint(rebc);
                rebcs.add(rebc);
            }
        }
        return rebcs;
    }

    public RuleEngineBasedConstraint handle(AddedEvaluationResultToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        if (rebc != null) {
            for (Map.Entry<ResourceLink, Boolean> entry : evt.getRes().entrySet()) {
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
        return rebc;
    }

    public WorkflowTask handle(AddedAsInputEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactInput input = new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType());
        // TODO check if input is expected
        wft.addInput(input);
        return wft;
    }

    public WorkflowTask handle(AddedAsOutputEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactOutput output = new ArtifactOutput(evt.getArtifact(), evt.getRole(), evt.getType());
        wft.addOutput(output);
        return wft;
    }

    public void handle(AddedAsInputToWfiEvt evt) {
        wfi.addInput(evt.getInput());
    }

    public void handle(AddedAsOutputToWfiEvt evt) {
        wfi.addOutput(evt.getOutput());
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof ImportedOrUpdatedArtifactEvt) {
            handle((ImportedOrUpdatedArtifactEvt) evt);
        } else if (evt instanceof ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt) {
            handle((ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt) evt);
        } else if (evt instanceof CreatedChildWorkflowEvt) {
            handle((CreatedChildWorkflowEvt) evt);
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
        } else if (evt instanceof AddedAsInputEvt) {
            handle((AddedAsInputEvt) evt);
        } else if (evt instanceof AddedAsOutputEvt) {
            handle((AddedAsOutputEvt) evt);
        } else if (evt instanceof AddedAsInputToWfiEvt) {
            handle((AddedAsInputToWfiEvt) evt);
        } else if (evt instanceof AddedAsOutputToWfiEvt) {
            handle((AddedAsOutputToWfiEvt) evt);
        } else {
            log.error("[MOD] Ignoring message of type: "+evt.getClass().getSimpleName());
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
            for (ArtifactOutput ao : wft.getOutput()) {
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
