package impactassessment.passiveprocessengine;

import impactassessment.api.Events.*;
import impactassessment.jiraartifact.IJiraArtifact;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.Artifact;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.AbstractWorkflowInstanceObject;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.ArtifactWrapper;
import passiveprocessengine.instance.CorrelationTuple;
import passiveprocessengine.instance.DecisionNodeInstance;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.ResourceLink;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.WorkflowInstance;
import passiveprocessengine.instance.WorkflowTask;

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
                ArtifactWrapper aw = new ArtifactWrapper(artifact.getKey(), ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK, wfi, artifact);
                // TODO add as input (enable input to input mapping!)
                //wfi.addOutput(new ArtifactOutput(aw, "INPUT", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET)));
                wfi.addInput(new ArtifactInput(aw, "ROLE_WPTICKET", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK)));
            }
        }
    }

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
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
        List<AbstractWorkflowInstanceObject> awos = wfi.enableWorkflowTasksAndDecisionNodes();
        return awos;
    }

    public Map<IWorkflowTask, ArtifactInput> handle(CompletedDataflowEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        if (dni != null) {
            dni.completedDataflowInvolvingActivationPropagation();
            return dni.executeMapping();
        } else {
            log.error("{} caused an error. Couldn't be found in current WFI (present DNIs: {})", evt, wfi.getDecisionNodeInstancesReadonly().stream().map(DecisionNodeInstance::toString).collect(Collectors.joining( "," )));
            return Collections.emptyMap();
        }

    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedInBranchEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
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
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null && wft != null) {
            awos.addAll(dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft)));
            awos.addAll(dni.activateOutBranch(evt.getBranchId()));
        }
        return awos;
    }

    public List<AbstractWorkflowInstanceObject> handle(ActivatedInOutBranchesEvt evt) {
        DecisionNodeInstance dni = wfi.getDecisionNodeInstance(evt.getDniId());
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        if (dni != null && wft != null) {
            awos.addAll(dni.activateInBranch(dni.getInBranchIdForWorkflowTask(wft)));
            String[] branchIds = new String[evt.getBranchIds().size()];
            awos.addAll(dni.activateOutBranches(evt.getBranchIds().toArray(branchIds)));
        }
        return awos;
    }

    public List<RuleEngineBasedConstraint> handle(AddedConstraintsEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<RuleEngineBasedConstraint> rebcs = new ArrayList<>();
        if (wft != null) {
            QACheckDocument qa = getQACDocOfWft(wft);
            if (qa == null) {
                qa = new QACheckDocument("QA-" + wft.getType().getId() + "-" + wft.getWorkflow().getId(), wft.getWorkflow());
                ArtifactOutput ao = new ArtifactOutput(qa, "ROLE_DOCUMENTATION", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
                addConstraint(evt, qa, wft, rebcs);
                wft.addOutput(ao);
            } else {
                addConstraint(evt, qa, wft, rebcs);
            }

        }
        return rebcs;
    }

    private void addConstraint(AddedConstraintsEvt evt, QACheckDocument qa, IWorkflowTask wft, List<RuleEngineBasedConstraint> rebcs) {
        CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
        qa.setLastChangeDueTo(corr);
        Map<String, String> rules = evt.getRules();
        for (Map.Entry<String, String> e : rules.entrySet()) {
            String rebcId = e.getKey()+"_"+wft.getType().getId()+"_"+ wft.getWorkflow().getId();
            RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, e.getKey(), wft.getWorkflow(), e.getValue());
            rebc.addAs(false, ResourceLinkFactory.getMock());
            qa.addConstraint(rebc);
            rebcs.add(rebc);
        }
    }

    public RuleEngineBasedConstraint handle(AddedEvaluationResultToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getQAC(evt.getQacId());
        if (rebc != null) {
            rebc.removeAllResourceLinks();
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

    public IWorkflowTask handle(AddedInputEvt evt) {
        setWfi(evt.getArtifact());
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactInput input = new ArtifactInput(evt.getArtifact(), evt.getRole(), evt.getType());
        // TODO check if input is expected
        wft.addInput(input);
        return wft;
    }

    public IWorkflowTask handle(AddedOutputEvt evt) {
        setWfi(evt.getArtifact());
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactOutput output = new ArtifactOutput(evt.getArtifact(), evt.getRole(), evt.getType());
        wft.addOutput(output);
        return wft;
    }

    public void handle(AddedInputToWorkflowEvt evt) {
        if (wfi == null) return;
        setWfi(evt.getInput().getArtifact());
//        wfi.getInput().stream()
//                .filter(in -> in.getArtifact().getId().equals(evt.getInput().getArtifact().getId()))
//                .forEach(in -> in.setArtifact(evt.getInput().getArtifact()));
        wfi.addInput(evt.getInput());
    }

    private void setWfi(Artifact a) {
        if (a instanceof ArtifactWrapper) {
            if (((ArtifactWrapper)a).getWorkflow() == null) {
                ((ArtifactWrapper)a).setWorkflow(wfi);
            }
        }
    }

    public void handle(AddedOutputToWorkflowEvt evt) {
        if (wfi == null) return;
        setWfi(evt.getOutput().getArtifact());
        wfi.addOutput(evt.getOutput());
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof CreatedWorkflowEvt) {
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
        IWorkflowTask wft = wfi.getWorkflowTask(wftId);
        return getQACDocOfWft(wft);
    }

    public QACheckDocument getQACDocOfWft(IWorkflowTask wft) {
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
        if (wfi == null) return null;
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
