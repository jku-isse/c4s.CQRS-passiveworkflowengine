package impactassessment.passiveprocessengine;

import artifactapi.ArtifactType;
import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;
import impactassessment.api.Events.*;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.*;
import passiveprocessengine.instance.*;
import passiveprocessengine.instance.QACheckDocument.QAConstraint.EvaluationState;

import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class WorkflowInstanceWrapper {

    private WorkflowInstance wfi;

    public List<IArtifact> getArtifacts() {
        List<IArtifact> artifacts = new ArrayList<>();
        if (wfi != null) {
            artifacts.addAll(wfi.getInput().stream()
                    .map(ArtifactIO::getArtifact)
                    .collect(Collectors.toList()));
            artifacts.addAll(wfi.getOutput().stream()
                    .map(ArtifactIO::getArtifact)
                    .collect(Collectors.toList()));
        }
        return artifacts;
    }
    
    private void setInputArtifacts(Collection<Entry<String,IArtifact>> inputs) {
    	if (wfi != null) {
    		inputs.forEach(in -> wfi.addInput(new ArtifactInput(in.getValue(), in.getKey(), new ArtifactType(in.getValue().getArtifactIdentifier().getType()))));
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
        return initWfi(evt.getId(), wfd, evt.getArtifacts());
    }

    private List<AbstractWorkflowInstanceObject> initWfi(String id, WorkflowDefinition wfd, Collection<Entry<String,IArtifact>> artifacts) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(id);
        setInputArtifacts(artifacts);
        addWorkflowInputsToWfProps(artifacts.stream().map(Entry::getValue).collect(Collectors.toList()));
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }

    private void addWorkflowInputsToWfProps(Collection<IArtifact> artifacts) {
        for (Entry<String, String> entry : wfi.getPropertiesReadOnly()) {
            wfi.addOrReplaceProperty(entry.getKey(), "removed from this process");
        }
        for (IArtifact a : artifacts) {
            if (a.getArtifactIdentifier().getType().equals(IJiraArtifact.class.getSimpleName())) {
                IJiraArtifact jiraArtifact = (IJiraArtifact) a;
                wfi.addOrReplaceProperty(jiraArtifact.getKey() + " (" + jiraArtifact.getId() + ")", jiraArtifact.getIssueType().getName());
            } else if (a.getArtifactIdentifier().getType().equals(IJamaArtifact.class.getSimpleName())) {
                IJamaArtifact jamaArtifact = (IJamaArtifact) a;
                wfi.addOrReplaceProperty(jamaArtifact.getDocumentKey(), String.valueOf(jamaArtifact.getId()));
            }
        }
    }

//    public Map<IWorkflowTask, ArtifactInput> handle(CompletedDataflowEvt evt) {
//        return null;
//    }

//    public List<AbstractWorkflowInstanceObject> handle(ActivatedInBranchEvt evt) {
//        return null;
//    }

//    public List<AbstractWorkflowInstanceObject> handle(ActivatedOutBranchEvt evt) {
//        return null;
//    }

//    public List<AbstractWorkflowInstanceObject> handle(ActivatedInOutBranchEvt evt) {
//        return null;
//    }

//    public List<AbstractWorkflowInstanceObject> handle(ActivatedInOutBranchesEvt evt) {
//        return null;
//    }

    public List<AbstractWorkflowInstanceObject> handle(AddedConstraintsEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        QACheckDocument qa = getQACDocOfWft(wft);
        if (qa == null) {
            qa = new QACheckDocument("QA-" + wft.getType().getId() + "-" + wft.getWorkflow().getId(), wft.getWorkflow());
            ArtifactOutput ao = new ArtifactOutput(qa, ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
            addConstraint(evt, qa, wft, awos);
            wft.addOutput(ao);
        } else {
            addConstraint(evt, qa, wft, awos);
        }
        awos.add((WorkflowTask)wft); // TODO: fix for nested workflow
        return awos;
    }

    private void addConstraint(AddedConstraintsEvt evt, QACheckDocument qa, IWorkflowTask wft, List<AbstractWorkflowInstanceObject> awos) {
        CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
        qa.setLastChangeDueTo(corr);
        Map<String, String> rules = evt.getRules();
        for (Map.Entry<String, String> e : rules.entrySet()) {
            String rebcId = e.getKey()+"_"+wft.getType().getId()+"_"+ wft.getWorkflow().getId();
            RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, e.getKey(), wft.getWorkflow(), e.getValue());
            rebc.setEvaluationStatus(EvaluationState.NOT_YET_EVALUATED);
            qa.addConstraint(rebc);
            awos.add(rebc);
        }
    }

    public Set<AbstractWorkflowInstanceObject> handle(AddedEvaluationResultToConstraintEvt evt) {
        RuleEngineBasedConstraint rebc = getRebc(evt.getQacId());
        Set<AbstractWorkflowInstanceObject> awos = new HashSet<>();
        if (rebc != null) {
            boolean hasChanged = false;
            Instant oldTime = rebc.getLastChanged();
            for (Map.Entry<ResourceLink, Boolean> entry : evt.getRes().entrySet()) {
                if ((!entry.getValue() && !rebc.getUnsatisfiedForReadOnly().contains(entry.getKey())) ||
                        (entry.getValue() && !rebc.getFulfilledForReadOnly().contains(entry.getKey()))) {
                    hasChanged = true;
                    break;
                }
            }
            rebc.removeAllResourceLinks();
            for (Map.Entry<ResourceLink, Boolean> entry : evt.getRes().entrySet()) {
                if ((!entry.getValue() && !rebc.getUnsatisfiedForReadOnly().contains(entry.getKey())) ||
                        (entry.getValue() && !rebc.getFulfilledForReadOnly().contains(entry.getKey()))) {
                    rebc.addAs(entry.getValue(), entry.getKey());
                }
            }
            if (hasChanged) {
                rebc.setLastChanged(evt.getTime());
            } else {
                rebc.setLastChanged(oldTime);
            }
            rebc.setLastEvaluated(evt.getTime());
            rebc.setEvaluated(evt.getCorr());
            if (evt.getRes().isEmpty()) {
            	rebc.setEvaluationStatus(QACheckDocument.QAConstraint.EvaluationState.FAILURE);
            } else {
            	rebc.setEvaluationStatus(QACheckDocument.QAConstraint.EvaluationState.SUCCESS);
            }
            // output state may change because QA constraints may be all fulfilled now
            wfi.getWorkflowTasksReadonly()
                .forEach(wft -> awos.addAll(wft.triggerQAConstraintsEvaluatedSignal()));
        }
        return awos;
    }

    public IWorkflowTask handle(AddedInputEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        replaceInput(evt.getArtifact(), evt.getType(), evt.getRole(), wft);
        return wft;
    }

    public List<IWorkflowInstanceObject> handle(AddedOutputEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        ArtifactOutput output = replaceOutput(evt.getArtifact(), evt.getType(), evt.getRole(), wft);
        List<IWorkflowInstanceObject> awos = new ArrayList<>();
        awos.addAll(wft.addOutput(output));
        awos.add(wft);
        return awos;
    }

    public void handle(AddedInputToWorkflowEvt evt) {
        replaceInput(evt.getArtifact(), evt.getType(), evt.getRole(), wfi);
        addWorkflowInputsToWfProps(List.of(evt.getArtifact()));
    }

    public void handle(AddedOutputToWorkflowEvt evt) {
        ArtifactOutput output = replaceOutput(evt.getArtifact(), evt.getType(), evt.getRole(), wfi);
        wfi.addOutput(output);
    }

    private void replaceInput(IArtifact artifact, String type, String role, IWorkflowTask iwft) {
        ArtifactInput input = new ArtifactInput(artifact, role, new ArtifactType(type));
        iwft.getInput().stream()
                .filter(o -> o.getRole().equals(role))
                .filter(o -> o.getArtifactType().getArtifactType().equals(type))
                .findAny()
                .ifPresent(iwft::removeInput);
        iwft.addInput(input);
    }

    private ArtifactOutput replaceOutput(IArtifact artifact, String type, String role, IWorkflowTask iwft) {
        ArtifactOutput output = new ArtifactOutput(artifact, role, new ArtifactType(type));
        iwft.getOutput().stream()
                .filter(o -> o.getRole().equals(role))
                .filter(o -> o.getArtifactType().getArtifactType().equals(type))
                .findAny()
                .ifPresent(iwft::removeOutput);
        return output;
    }

    public void handle(SetPreConditionsFulfillmentEvt evt) {
        // TODO implement
        log.warn("{} - handler not implemented", evt.getClass().getSimpleName());
    }

    public void handle(SetPostConditionsFulfillmentEvt evt) {
        // TODO implement
        log.warn("{} - handler not implemented", evt.getClass().getSimpleName());
    }

    public void handle(ActivatedTaskEvt evt) {
        // TODO implement
        log.warn("{} - handler not implemented", evt.getClass().getSimpleName());
    }

    public void handle(SetPropertiesEvt evt) {
        if (evt.getIwftId().equals(wfi.getId())) { // WorkflowInstance is targeted
            for (Entry<String, String> entry : evt.getProperties().entrySet()) {
                // name of the WorkflowInstance
                if (entry.getKey().equals("name")) {
                    wfi.setName(entry.getValue());
                }
                // wfProps
                for (Entry<String, String> property : wfi.getPropertiesReadOnly()) {
                    if (property.getKey().equals(entry.getKey())) {
                        wfi.addOrReplaceProperty(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
        } else { // WorkflowTask is targeted
            Optional<WorkflowTask> opt = wfi.getWorkflowTasksReadonly().stream()
                    .filter(wft -> wft.getId().equals(evt.getIwftId()))
                    .findAny();
            if (opt.isPresent()) {
                for (Entry<String, String> entry : evt.getProperties().entrySet()) {
                    switch (entry.getKey()) {
                        case "name":
                            opt.get().setName(entry.getValue());
                            break;
                         // TODO: insert additional properties
                        default:
                            log.warn("Property {} is not supported and cannot be set!", entry.getKey());
                    }
                }
            } else {
                log.warn("Handling {} coudln't get processed because WFT with ID {} wasn't found in workflow {}", evt.getClass().getSimpleName(), evt.getIwftId(), evt.getId());
            }
        }
    }

    public WorkflowTask handle(InstantiatedTaskEvt evt) {
        log.warn("Event handler for 'InstantiatedTaskEvt' not implemented!");
        // TODO implement
        return null;
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof CreatedWorkflowEvt) {
            handle((CreatedWorkflowEvt) evt);
        } else if (evt instanceof CreatedSubWorkflowEvt) {
            handle((CreatedSubWorkflowEvt) evt);
//        } else if (evt instanceof CompletedDataflowEvt) {
//            handle((CompletedDataflowEvt) evt);
//        } else if (evt instanceof ActivatedInBranchEvt) {
//            handle((ActivatedInBranchEvt) evt);
//        } else if (evt instanceof ActivatedOutBranchEvt) {
//            handle((ActivatedOutBranchEvt) evt);
//        } else if (evt instanceof ActivatedInOutBranchEvt) {
//            handle((ActivatedInOutBranchEvt) evt);
//        } else if (evt instanceof ActivatedInOutBranchesEvt) {
//            handle((ActivatedInOutBranchesEvt) evt);
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
        } else if (evt instanceof SetPreConditionsFulfillmentEvt) {
            handle((SetPreConditionsFulfillmentEvt) evt);
        } else if (evt instanceof SetPostConditionsFulfillmentEvt) {
            handle((SetPostConditionsFulfillmentEvt) evt);
        } else if (evt instanceof ActivatedTaskEvt) {
            handle((ActivatedTaskEvt) evt);
        } else if (evt instanceof SetPropertiesEvt) {
            handle((SetPropertiesEvt) evt);
        } else if (evt instanceof InstantiatedTaskEvt) {
            handle((InstantiatedTaskEvt) evt);
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

    public RuleEngineBasedConstraint getRebc(String rebcId) {
        if (wfi == null) return null;
        List<WorkflowTask> wfts = wfi.getWorkflowTasksReadonly();
        for (WorkflowTask wft : wfi.getWorkflowTasksReadonly()) {
            for (ArtifactOutput ao : wft.getOutput()) {
                if (ao.getArtifact() instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) ao.getArtifact();
                    for (QACheckDocument.QAConstraint rebc : qacd.getConstraintsReadonly()) {
                        if (rebc.getId().equals(rebcId)) {
                            if (rebc instanceof RuleEngineBasedConstraint) {
                                return (RuleEngineBasedConstraint) rebc;
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
