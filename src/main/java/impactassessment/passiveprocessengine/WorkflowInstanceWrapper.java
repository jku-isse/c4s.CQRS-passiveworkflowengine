package impactassessment.passiveprocessengine;

import impactassessment.SpringUtil;
import impactassessment.api.*;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.passiveprocessengine.definition.*;
import impactassessment.passiveprocessengine.instance.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class WorkflowInstanceWrapper {

    private WorkflowInstance wfi;

    public List<IJiraArtifact> getArtifacts() {
        List<IJiraArtifact> artifacts = new ArrayList<>();
        if (wfi != null) {
            artifacts.addAll(wfi.getInput().stream()
                    .filter(i -> i.getArtifact() instanceof ArtifactWrapper)
                    .filter(aw -> ((ArtifactWrapper)aw.getArtifact()).getWrappedArtifact() instanceof IJiraArtifact)
                    .map(j -> (IJiraArtifact)((ArtifactWrapper)j.getArtifact()).getWrappedArtifact())
                    .collect(Collectors.toList()));
            artifacts.addAll(wfi.getOutput().stream()
                    .filter(i -> i.getArtifact() instanceof ArtifactWrapper)
                    .filter(aw -> ((ArtifactWrapper)aw.getArtifact()).getWrappedArtifact() instanceof IJiraArtifact)
                    .map(j -> (IJiraArtifact)((ArtifactWrapper)j.getArtifact()).getWrappedArtifact())
                    .collect(Collectors.toList()));
        }
        return artifacts;
    }

    private void setArtifact(List<IJiraArtifact> artifacts) {
        if (wfi != null) {
            for (IJiraArtifact artifact : artifacts) {
                ArtifactWrapper aw = new ArtifactWrapper("Wrapped#" + artifact.getKey(), artifact.getClass().getSimpleName(), wfi, artifact);
                // TODO add as input (enable input to input mapping!)
                wfi.addOutput(new ArtifactOutput(aw, "INPUT", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET)));
            }
        }
    }

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    public List<AbstractWorkflowInstanceObject> handle(CreatedDefaultWorkflowEvt evt) {
        WorkflowDefinition wfd = SpringUtil.getBean(WorkflowDefinition.class); // use default workflow specified in SpringConfig
        return initWfi(evt.getId(), wfd, evt.getArtifacts());
    }

    public List<AbstractWorkflowInstanceObject> handle(CreatedWorkflowEvt evt) {
        WorkflowDefinition wfd = evt.getWfd();
        return initWfi(evt.getId(), wfd, evt.getArtifacts());
    }

    public List<AbstractWorkflowInstanceObject> handle(CreatedSubWorkflowEvt evt) {
        WorkflowDefinition wfd = evt.getWfd();
        return initWfi(evt.getId(), wfd, Collections.emptyList());
    }

    private List<AbstractWorkflowInstanceObject> initWfi(String id, WorkflowDefinition wfd, List<IJiraArtifact> artifacts) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(id);
        setArtifact(artifacts);
        for (IJiraArtifact artifact: artifacts) {
            wfi.addOrReplaceProperty(artifact.getKey() + " (" + artifact.getId() + ")", artifact.getIssueType().getName());
        }
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }

    public Map<IWorkflowTask, ArtifactInput> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        dni.completedDataflowInvolvingActivationPropagation();
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

    public WorkflowTask handle(AddedInputEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactInput input = new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType());
        // TODO check if input is expected
        wft.addInput(input);
        return wft;
    }

    public WorkflowTask handle(AddedOutputEvt evt) {
        WorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactOutput output = new ArtifactOutput(evt.getArtifact(), evt.getRole(), evt.getType());
        wft.addOutput(output);
        return wft;
    }

    public void handle(AddedInputToWorkflowEvt evt) {
        wfi.addInput(evt.getInput());
    }

    public void handle(AddedOutputToWorkflowEvt evt) {
        wfi.addOutput(evt.getOutput());
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof CreatedDefaultWorkflowEvt) {
            handle((CreatedDefaultWorkflowEvt) evt);
        } else if (evt instanceof CreatedWorkflowEvt) {
            handle((CreatedWorkflowEvt) evt);
        } else if (evt instanceof CreatedSubWorkflowEvt) {
            handle((CreatedSubWorkflowEvt) evt);
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
        } else if (evt instanceof AddedInputEvt) {
            handle((AddedInputEvt) evt);
        } else if (evt instanceof AddedOutputEvt) {
            handle((AddedOutputEvt) evt);
        } else if (evt instanceof AddedInputToWorkflowEvt) {
            handle((AddedInputToWorkflowEvt) evt);
        } else if (evt instanceof AddedOutputToWorkflowEvt) {
            handle((AddedOutputToWorkflowEvt) evt);
        } else {
            log.warn("[MOD] Ignoring message of type: "+evt.getClass().getSimpleName());
        }
    }

    public QACheckDocument getQACDocOfWft(String wftId) {
        WorkflowTask wft = wfi.getWorkflowTask(wftId);
        Optional<QACheckDocument> optQACD = Optional.empty();
        if (wft != null){
            optQACD = wft.getOutput().stream()
                    .map(ArtifactIO::getArtifact)
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
