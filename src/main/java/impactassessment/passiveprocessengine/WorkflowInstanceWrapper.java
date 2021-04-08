package impactassessment.passiveprocessengine;

import artifactapi.ArtifactIdentifier;
import artifactapi.ArtifactType;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
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
    private IArtifactRegistry artReg;
    
    public WorkflowInstanceWrapper(IArtifactRegistry artReg) {
    	this.artReg = artReg;
    }
    
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

    private List<AbstractWorkflowInstanceObject> initWfi(String id, WorkflowDefinition wfd, Collection<Entry<String,ArtifactIdentifier>> art) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(id);
        List<Entry<String,IArtifact>> artifacts = art.stream()
        		.map(entry -> new AbstractMap.SimpleEntry<String, Optional<IArtifact>>(entry.getKey(), artReg.get(entry.getValue(), id)))
        		.filter(entry -> entry.getValue().isPresent())
        		.map(entry -> new AbstractMap.SimpleEntry<String, IArtifact>(entry.getKey(), entry.getValue().get()))
        		.collect(Collectors.toList());
        setInputArtifacts(artifacts);
       // addWorkflowInputsToWfProps(artifacts.stream().map(Entry::getValue).collect(Collectors.toList()));
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }

//    private void addWorkflowInputsToWfProps(Collection<IArtifact> artifacts) {
//        for (Entry<String, String> entry : wfi.getPropertiesReadOnly()) {
//            wfi.addOrReplaceProperty(entry.getKey(), "removed from this process"); THIS WOULD REMOVE EVERY PROPERTY VALUE
//        }
//        for (IArtifact a : artifacts) {
//            if (a.getArtifactIdentifier().getType().equals(IJiraArtifact.class.getSimpleName())) {
//                IJiraArtifact jiraArtifact = (IJiraArtifact) a;
//                wfi.addOrReplaceProperty(jiraArtifact.getKey() + " (" + jiraArtifact.getId() + ")", jiraArtifact.getIssueType().getName());
//            } else if (a.getArtifactIdentifier().getType().equals(IJamaArtifact.class.getSimpleName())) {
//                IJamaArtifact jamaArtifact = (IJamaArtifact) a;
//                wfi.addOrReplaceProperty(jamaArtifact.getDocumentKey(), String.valueOf(jamaArtifact.getId()));
//            }
//        }
//    }

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
            awos.addAll(wft.addOutput(ao));
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
       // Optional<IArtifact> art = artReg.get(evt.getArtifact(), evt.getId());
       // if (art.isPresent())
        replaceInput(evt.getArtifact(), evt.getType(), evt.getRole(), wft);
        return wft;
    }

    public List<IWorkflowInstanceObject> handle(AddedOutputEvt evt) {
    	IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
    	//Optional<IArtifact> art = artReg.get(evt.getArtifact(), evt.getId());
    	//if (art.isPresent()) {
    		ArtifactOutput output = replaceOutput(evt.getArtifact(), evt.getType(), evt.getRole(), wft);
    		List<IWorkflowInstanceObject> awos = new ArrayList<>();
    		awos.addAll(wft.addOutput(output));
    		awos.add(wft);
    		return awos; 
//    	}
//    	else return Collections.emptyList();
    }

    public void handle(AddedInputToWorkflowEvt evt) {
    	//Optional<IArtifact> art = artReg.get(evt.getArtifact(), evt.getId());
        //if (art.isPresent()) {
        replaceInput(evt.getArtifact(), evt.getType(), evt.getRole(), wfi);
        //	addWorkflowInputsToWfProps(List.of(art.get()));
       // }
    }

    public void handle(AddedOutputToWorkflowEvt evt) {
    	//Optional<IArtifact> art = artReg.get(evt.getArtifact(), evt.getId());
        //if (art.isPresent()) {
        	ArtifactOutput output = replaceOutput(evt.getArtifact(), evt.getType(), evt.getRole(), wfi);
        	wfi.addOutput(output);
        //}
    }

    private void replaceInput(ArtifactIdentifier artifact, String type, String role, IWorkflowTask iwft) {
        ArtifactInput input = new LazyLoadingArtifactInput(artifact, artReg, wfi.getId(), new ArtifactType(type), role);
        iwft.getInput().stream()
                .filter(o -> o.getRole().equals(role))
                .filter(o -> o.getArtifactType().getArtifactType().equals(type))
                .findAny()
                .ifPresent(iwft::removeInput); //TODO: check if this doesnt result in concurrent modification exception
        iwft.addInput(input);
    }

    private ArtifactOutput replaceOutput(ArtifactIdentifier artifact, String type, String role, IWorkflowTask iwft) {
        ArtifactOutput output = new LazyLoadingArtifactOutput(artifact, artReg, wfi.getId(), new ArtifactType(type), role);
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
                //for (Entry<String, String> property : wfi.getPropertiesReadOnly()) {
                //    if (property.getKey().equals(entry.getKey())) {
                wfi.addOrReplaceProperty(entry.getKey(), entry.getValue());
                //        break;
                //    }
               // }
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
                            log.warn("Setting Property {} on a WorkflowTask is not supported!", entry.getKey());
                    }
                }
            } else {
                log.warn("Handling {} coudln't get processed because WFT with ID {} wasn't found in workflow {}", evt.getClass().getSimpleName(), evt.getIwftId(), evt.getId());
            }
        }
    }

    public Set<AbstractWorkflowInstanceObject> handle(InstantiatedTaskEvt evt) {
        Set<AbstractWorkflowInstanceObject> awos = new HashSet<>();

        // check if task already exists
        boolean taskAlreadyExists = wfi.getWorkflowTasksReadonly().stream()
                .map(WorkflowTask::getType)
                .anyMatch(td -> td.getId().equals(evt.getTaskDefinitionId()));

        if (!taskAlreadyExists) {
            wfi.getType().getWorkflowTaskDefinitions().stream()
                    .filter(td -> td.getId().equals(evt.getTaskDefinitionId()))
                    .findAny()
                    .ifPresent(taskDefinition -> awos.addAll(wfi.createAndWireTask(taskDefinition)));
            // find WFT
            Optional<WorkflowTask> optWft = awos.stream()
                    .filter(x -> x instanceof WorkflowTask)
                    .map(x -> (WorkflowTask) x)
                    .filter(x -> x.getId().startsWith(evt.getTaskDefinitionId()))
                    .findAny();
            // add inputs/outputs
            if (optWft.isPresent()) {
                WorkflowTask wft = optWft.get();
                awos.addAll(wft.activate()); // activate task
                for (ArtifactInput in : evt.getOptionalInputs()) {
                    if (in instanceof LazyLoadingArtifactInput) {
                    	((LazyLoadingArtifactInput) in).reinjectRegistry(artReg);
                    } else {
                    	artReg.injectArtifactService(in.getArtifact(), evt.getId());
                    }
                	wft.addInput(in);
                }
                for (ArtifactOutput out : evt.getOptionalOutputs()) {
                   if (out instanceof LazyLoadingArtifactOutput) {
                	   ((LazyLoadingArtifactOutput) out).reinjectRegistry(artReg);
                   } else {
                	   artReg.injectArtifactService(out.getArtifact(), evt.getId());
                   }
                	wft.addOutput(out);
                }
            }
        }
        return awos;
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
