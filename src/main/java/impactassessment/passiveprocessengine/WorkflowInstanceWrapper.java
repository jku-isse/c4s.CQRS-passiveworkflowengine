package impactassessment.passiveprocessengine;


import artifactapi.*;
import impactassessment.api.Events.*;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.definition.WorkflowDefinition;
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
                    .map(ArtifactIO::getArtifacts)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
            artifacts.addAll(wfi.getOutput().stream()
                    .map(ArtifactIO::getArtifacts)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
        }
        return artifacts;
    }
    
    private void setInputArtifacts(Collection<Entry<String,IArtifact>> inputs) {
    	if (wfi != null) {
    	    // TODO use LazyLoadingArtifactInput here?
    		inputs.forEach(in -> wfi.addInput(new ArtifactInput(in.getValue(), in.getKey())));
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
        return wfi.enableWorkflowTasksAndDecisionNodes();
    }


    public List<AbstractWorkflowInstanceObject> handle(AddedConstraintsEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
        QACheckDocument qa = getQACDocOfWft(wft);
        if (qa == null) {
            qa = new QACheckDocument("QA-" + wft.getType().getId() + "-" + wft.getWorkflow().getId(), wft.getWorkflow());
            ArtifactOutput ao = new ArtifactOutput(qa, ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT);
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

        addInput(evt.getId(), evt.getArtifact(), evt.getRole(), wft);
        return wft;
    }

    public List<IWorkflowInstanceObject> handle(AddedOutputEvt evt) {
    	IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        Optional<ArtifactOutput> opt = addOutput(evt.getId(), evt.getArtifact(), evt.getType(), evt.getRole(), wft);
        List<IWorkflowInstanceObject> awos = new ArrayList<>();
        opt.ifPresent(artifactOutput -> awos.addAll(wft.addOutput(artifactOutput)));
        awos.add(wft);
        return awos;

    }

    public void handle(AddedInputToWorkflowEvt evt) {

        addInput(evt.getId(), evt.getArtifact(), evt.getRole(), wfi);
    }

    public void handle(AddedOutputToWorkflowEvt evt) {
        Optional<ArtifactOutput> opt = addOutput(evt.getId(), evt.getArtifact(), evt.getType(), evt.getRole(), wfi);
        opt.ifPresent(out -> wfi.addOutput(out));
    }

    private void addInput(String id, ArtifactIdentifier artifact, String role, IWorkflowTask iwft) {
        Optional<ArtifactInput> opt = iwft.getInput().stream()
                .filter(o -> o.getRole().equals(role))
                .findAny();
        if (opt.isPresent()) { // if ArtifactOutput with correct role is present, IArtifact is added to Set
        	ArtifactInput ao = opt.get();
        	if (ao instanceof LazyLoadingArtifactInput) { // then lets just store the identifier
        		((LazyLoadingArtifactInput) ao).addOrReplaceArtifact(artifact);
        	} else { // otherwise fetch and store the artifacts
        		artReg.get(artifact, id).ifPresent(a -> opt.get().addOrReplaceArtifact(a)); 
        	}
        } else { // if no ArtifactInput with correct role is present, a new ArtifactInput is created
            ArtifactInput input = new LazyLoadingArtifactInput(artifact, artReg, wfi.getId(), role);
            iwft.addInput(input);
        }
    }

    private Optional<ArtifactOutput> addOutput(String id, ArtifactIdentifier artifact, String type, String role, IWorkflowTask iwft) {
        Optional<ArtifactOutput> opt = iwft.getOutput().stream()
                .filter(o -> o.getRole().equals(role))
                .findAny();
        if (opt.isPresent()) { // if ArtifactOutput with correct role is present, IArtifact is added to Set
        	ArtifactOutput ao = opt.get();
        	if (ao instanceof LazyLoadingArtifactOutput) { // then lets just store the identifier
        		((LazyLoadingArtifactOutput) ao).addOrReplaceArtifact(artifact);
        	} else { // otherwise fetch and store the artifacts
        		artReg.get(artifact, id).ifPresent(a -> opt.get().addOrReplaceArtifact(a)); 
        	}
            return Optional.empty();
        } else { // if no ArtifactOutput with correct role is present, a new ArtifactOutput is created
            ArtifactOutput output = new LazyLoadingArtifactOutput(artifact, artReg, wfi.getId(), role);
            return Optional.of(output);
        }
    }

    public Set<AbstractWorkflowInstanceObject> handle(SetPreConditionsFulfillmentEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptySet();
        } else {
        	if (evt.isFulfilled()) 
        		return wft.preConditionsFulfilled();
        	else
        		return wft.preConditionsFailed();
        }
    }

    public Set<AbstractWorkflowInstanceObject> handle(SetPostConditionsFulfillmentEvt evt) {
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptySet();
        } else {
        	if (evt.isFulfilled()) 
        		return wft.postConditionsFulfilled();
        	else
        		return wft.postConditionsFailed();
        }
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
                        for (IArtifact a : in.getArtifacts()) {
                            artReg.injectArtifactService(a, evt.getId());
                        }
                    }
                	wft.addInput(in);
                }
                for (ArtifactOutput out : evt.getOptionalOutputs()) {
                   if (out instanceof LazyLoadingArtifactOutput) {
                	   ((LazyLoadingArtifactOutput) out).reinjectRegistry(artReg);
                   } else {
                       for (IArtifact a : out.getArtifacts()) {
                           artReg.injectArtifactService(a, evt.getId());
                       }
                   }
                	wft.addOutput(out);
                }
            }
        }
        return awos;
    }

    public IWorkflowTask handle(RemovedInputEvt evt) {
        List<ArtifactInput> inputs;
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
            if (evt.getId().equals(evt.getWftId())) {
                inputs = wfi.getInput();
            } else {
                return null;
            }
        } else {
            inputs = wft.getInput();
        }
        Optional<ArtifactInput> opt = inputs.stream()
                .filter(i -> i.getRole().equals(evt.getRole()))
                .findAny();
        if (opt.isPresent()) {
            ArtifactInput in = opt.get();
            artReg.get(evt.getArtifact(), evt.getId()).ifPresent(in::removeArtifact);
            if (in.getArtifacts().size() == 0 && wft != null) {
                wft.removeInput(in);
            }
        }
        return wft;
    }

    public IWorkflowTask handle(RemovedOutputEvt evt) {
        List<ArtifactOutput> outputs;
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
            if (evt.getId().equals(evt.getWftId())) {
                outputs = wfi.getOutput();
            } else {
                return null;
            }
        } else {
            outputs = wft.getOutput();
        }
        Optional<ArtifactOutput> opt = outputs.stream()
                .filter(o -> o.getRole().equals(evt.getRole()))
                .findAny();
        if (opt.isPresent()) {
            ArtifactOutput out = opt.get();
            artReg.get(evt.getArtifact(), evt.getId()).ifPresent(out::removeArtifact);
            if (out.getArtifacts().size() == 0 && wft != null) {
                wft.removeOutput(out);
            }
        }
        return wft;
    }

    public void handle(IdentifiableEvt evt) {
        if (evt instanceof CreatedWorkflowEvt) {
            handle((CreatedWorkflowEvt) evt);
        } else if (evt instanceof CreatedSubWorkflowEvt) {
            handle((CreatedSubWorkflowEvt) evt);
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
        } else if (evt instanceof ActivatedTaskEvt) {
            handle((ActivatedTaskEvt) evt);
        } else if (evt instanceof SetPropertiesEvt) {
            handle((SetPropertiesEvt) evt);
        } else if (evt instanceof InstantiatedTaskEvt) {
            handle((InstantiatedTaskEvt) evt);
        } else if (evt instanceof RemovedInputEvt) {
            handle((RemovedInputEvt) evt);
        } else if (evt instanceof RemovedOutputEvt) {
            handle((RemovedOutputEvt) evt);
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
                    .map(ArtifactIO::getArtifacts)
                    .flatMap(Collection::stream)
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
                for (IArtifact a : ao.getArtifacts()) {
                    if (a instanceof QACheckDocument) {
                        QACheckDocument qacd = (QACheckDocument) a;
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
