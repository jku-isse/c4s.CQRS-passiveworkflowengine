package impactassessment.passiveprocessengine;


import artifactapi.*;
import impactassessment.api.Events.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.*;
import passiveprocessengine.instance.QACheckDocument.QAConstraint.EvaluationState;
import passiveprocessengine.instance.WorkflowChangeEvent.ChangeType;
import passiveprocessengine.instance.events.ConstraintEvaluationEvent;
import passiveprocessengine.instance.events.WFOCreatedEvent;

import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class WorkflowInstanceWrapper {

    protected WorkflowInstance wfi;
    protected IArtifactRegistry artReg;

    private @Getter @Setter String parentWfiId;
    private @Getter @Setter String parentWftId;

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
    
    protected List<WorkflowChangeEvent> setInputArtifacts(Map<IArtifact, String> inputs, WorkflowChangeEvent rootWCE) {
    	if (wfi != null) {
    	    // use LazyLoadingArtifactInput here? --> fine, as we have the artifacts already available
    		return inputs.entrySet().stream().flatMap(entry -> wfi.addInput(new ArtifactInput(entry.getKey(), entry.getValue()),rootWCE).stream()).collect(Collectors.toList());
    	}
    	return Collections.emptyList();
    }

    public WorkflowInstance getWorkflowInstance() {
        return wfi;
    }

    public List<WorkflowChangeEvent> handle(CreatedWorkflowEvt evt) {    
    	WorkflowDefinition wfd = evt.getWfd();
        return initWfi(evt.getId(), wfd, evt.getArtifacts(), evt.getParentCauseRef());
    }

    public List<WorkflowChangeEvent> handle(CreatedSubWorkflowEvt evt) {
        WorkflowDefinition wfd = evt.getWfd();
        return initWfi(evt.getId(), wfd, evt.getArtifacts(), evt.getParentCauseRef());
    }

    protected List<WorkflowChangeEvent> initWfi(String id, WorkflowDefinition wfd, Map<ArtifactIdentifier, String> art, String root) {
        wfd.setTaskStateTransitionEventPublisher(event -> {/*No Op*/}); // NullPointer if event publisher is not set
        wfi = wfd.createInstance(id);
        WorkflowChangeEvent rootWCE = new RootWorkflowChangeEvent(root, wfi);
        Map<IArtifact, String> artifacts = art.entrySet().stream()
        		.map(entry -> new AbstractMap.SimpleEntry<>(artReg.get(entry.getKey(), id), entry.getValue()))
        		.filter(entry -> entry.getKey().isPresent())
        		.collect(Collectors.toMap(k -> k.getKey().get(), v -> v.getValue()));
        List<WorkflowChangeEvent> changes = new LinkedList<>();
        changes.add(new WFOCreatedEvent(wfi, wfi, rootWCE));
        changes.addAll(setInputArtifacts(artifacts, rootWCE));        
        changes.addAll(wfi.enableWorkflowTasksAndDecisionNodes(rootWCE));        
        return changes;
    }


    public List<WorkflowChangeEvent> handle(AddedConstraintsEvt evt) {
    	
    	IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
    	List<WorkflowChangeEvent> awos = new ArrayList<>();
        QACheckDocument qa = getQACDocOfWft(wft);
        if (qa == null) {
            qa = new QACheckDocument("QA-" + wft.getType().getId() + "-" + wft.getWorkflow().getId(), wft.getWorkflow());
            ArtifactOutput ao = new ArtifactOutput(qa, ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT);
            addConstraint(evt, qa, wft, awos, rootWCE);
            awos.addAll(wft.addOutput(ao, rootWCE));           
            //awos.add((WorkflowTask)wft); //  fix for nested workflow, --> we assume a nested workflow task doesn have its own QAchecks but rather the steps inside the nested workflow have, thus not an issue here
        }  else { //We require that all constraints are set at once in a single command, ==> not possible
            addConstraint(evt, qa, wft, awos, rootWCE);
         }        
        return awos;
    }

    private void addConstraint(AddedConstraintsEvt evt, QACheckDocument qa, IWorkflowTask wft, List<WorkflowChangeEvent> awos, WorkflowChangeEvent rootWCE ) {
        CorrelationTuple corr = wft.getWorkflow().getLastChangeDueTo().orElse(new CorrelationTuple(qa.getId(), "INITIAL_TRIGGER"));
        qa.setLastChangeDueTo(corr);
        Map<String, String> rules = evt.getRules();
        for (Map.Entry<String, String> e : rules.entrySet()) {
            String rebcId = e.getKey()+"_"+wft.getType().getId()+"_"+ wft.getWorkflow().getId(); // TODO pull id creation inside constructor!
            // check if constraint exists already
            if (!qa.getConstraintsReadonly().parallelStream().anyMatch(qac -> qac.getId().equals(rebcId))) {
            	// if not, then add
            	RuleEngineBasedConstraint rebc = new RuleEngineBasedConstraint(rebcId, qa, e.getKey(), wft.getWorkflow(), e.getValue());
            	rebc.setEvaluationStatus(EvaluationState.NOT_YET_EVALUATED);
            	qa.addConstraint(rebc);
            	awos.add(new WFOCreatedEvent(rebc, wft, rootWCE));
            }
        }
    }

    public List<WorkflowChangeEvent> handle(AddedEvaluationResultToConstraintEvt evt) {
    	
    	List<WorkflowChangeEvent> awos = new LinkedList<>();
        getRebc(evt.getWftId(), evt.getQacId()).ifPresent(rebc -> {
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
                rebc.setEvaluationStatus(QACheckDocument.QAConstraint.EvaluationState.FAILURE); // TODO make explicit failed command
            } else {
                rebc.setEvaluationStatus(QACheckDocument.QAConstraint.EvaluationState.SUCCESS);
            }
            // output state may change because QA constraints may be all fulfilled now
            // FIXME: why trigger all of them and not just the one we received a trigger for??? as now implemented below
            if (hasChanged) {
            	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
            	awos.add(new ConstraintEvaluationEvent((QACheckDocument) rebc.getParentArtifact(), rebc, wfi.getWorkflowTask(evt.getWftId()), rootWCE)); 
            	wfi.getWorkflowTasksReadonly()
            		.stream()
            		.filter(wft -> wft.getId().equals(evt.getWftId()))    
            		.forEach(wft -> awos.addAll(wft.triggerQAConstraintsEvaluatedSignal(rootWCE)));         
            }
        });
        return awos;
    }

    public void handle(UpdatedEvaluationTimeEvt evt) {
        getRebc(evt.getWftId(), evt.getQacId()).ifPresent(rebc -> {
            rebc.setLastEvaluated(evt.getTime());
            rebc.setEvaluated(evt.getCorr());
            rebc.setEvaluationStatus(QACheckDocument.QAConstraint.EvaluationState.SUCCESS);
        });
    }

    public List<WorkflowChangeEvent> handle(AddedInputEvt evt) {
    	
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        return wft.addInput(new LazyLoadingArtifactInput(evt.getArtifact(), artReg, wfi.getId(), evt.getRole()), rootWCE);
    }

    public List<WorkflowChangeEvent> handle(AddedOutputEvt evt) {
    	IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
    	 WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        return wft.addOutput(new LazyLoadingArtifactOutput(evt.getArtifact(), artReg, wfi.getId(), evt.getRole()), rootWCE);
    }

    public List<WorkflowChangeEvent> handle(AddedInputToWorkflowEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
        return wfi.addInput(new LazyLoadingArtifactInput(evt.getArtifact(), artReg, wfi.getId(), evt.getRole()), rootWCE);
    }

    public List<WorkflowChangeEvent> handle(AddedOutputToWorkflowEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
        return wfi.addOutput(new LazyLoadingArtifactOutput(evt.getArtifact(), artReg, wfi.getId(), evt.getRole()), rootWCE);
    }
    
    public List<WorkflowChangeEvent> handle(UpdatedArtifactsEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), getWorkflowInstance()) : null;
    	List<WorkflowChangeEvent> awos = new ArrayList<>();
		for (ArtifactIdentifier updatedArtifact : evt.getArtifacts()) {
			Optional<IArtifact> artOpt = artReg.get(updatedArtifact, evt.getId());
			artOpt.ifPresent(art -> {
				// check inputs and outputs of workflow instance (as this is also used for triggering premature activation/completion of tasks
				List<ArtifactIO> wfiInOuts = new LinkedList<>();
				wfiInOuts.addAll(getWorkflowInstance().getInput());
				wfiInOuts.addAll(getWorkflowInstance().getOutput());
				for (ArtifactIO io : wfiInOuts) {
					if (io.containsArtifact(art)) {
						awos.addAll(io.addOrReplaceArtifact(art, rootWCE));
					}
				}
				// check inputs and outputs of all workflow tasks
				for (WorkflowTask wft : getWorkflowInstance().getWorkflowTasksReadonly()) {
					List<ArtifactIO> wftInOuts = new LinkedList<>();
					wftInOuts.addAll(wft.getInput());
					wftInOuts.addAll(wft.getOutput());
					for (ArtifactIO io : wftInOuts) {
						if (io.containsArtifact(art)) {
							awos.addAll(io.addOrReplaceArtifact(art, rootWCE));
							//awos.add(wft); // WFTs must be updated in the kieSession
						}
					}
				}							
			});
		}
		return awos;
    }

    public List<WorkflowChangeEvent> handle(SetPreConditionsFulfillmentEvt evt) {
    	
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        if (wft == null) {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptyList();
        } else {
        	if (evt.isFulfilled()) 
        		return wft.preConditionsFulfilled(rootWCE);
        	else
        		return wft.preConditionsFailed(rootWCE);
        }
    }

    public List<WorkflowChangeEvent> handle(SetPostConditionsFulfillmentEvt evt) {
    	
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        if (wft == null) {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptyList();
        } else {
        	if (evt.isFulfilled()) 
        		return wft.postConditionsFulfilled(rootWCE);
        	else
        		return wft.postConditionsFailed(rootWCE);
        }
    }

    public List<WorkflowChangeEvent> handle(ActivatedTaskEvt evt) {
   
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
     	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        if (wft == null) {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptyList();
        } else {
        	return wft.activate(rootWCE);
        }
    }

    public List<WorkflowChangeEvent> handle(ChangedCanceledStateOfTaskEvt evt) {
    	
    	IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wft) : null;
        if (wft != null) {
        	return wft.setCanceled(evt.isCanceled(), rootWCE);
        } else if (evt.getWftId().equals(wfi.getId())){
        	return wfi.setCanceled(evt.isCanceled(), rootWCE);
        } else {
        	log.warn("{} - workflowtask not found", evt.getWftId());
        	return Collections.emptyList();
        }
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

    public List<WorkflowChangeEvent> handle(InstantiatedTaskEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
    	List<WorkflowChangeEvent> awos = new LinkedList<>();

        // check if task already exists
        boolean taskAlreadyExists = wfi.getWorkflowTasksReadonly().stream()
                .map(WorkflowTask::getType)
                .anyMatch(td -> td.getId().equals(evt.getTaskDefinitionId()));

        if (!taskAlreadyExists) {
            wfi.getType().getWorkflowTaskDefinitions().stream()
                    .filter(td -> td.getId().equals(evt.getTaskDefinitionId()))
                    .findAny()
                    .ifPresent(taskDefinition -> awos.addAll(wfi.createAndWireTask(taskDefinition, rootWCE)));
            // find WFT
            Optional<WorkflowTask> optWft = awos.stream()
                    .filter(cevt -> cevt.getChangeType().equals(ChangeType.CREATED))
                    .map(cevt -> cevt.getChangedObject())
                    .distinct()
            		.filter(x -> x instanceof WorkflowTask)
                    .map(x -> (WorkflowTask) x)
                    .filter(x -> x.getId().startsWith(evt.getTaskDefinitionId())) // should not be necessary as creating on task should not lead to multple tasks being created downstream, TODO: this match here is BRITTLE
                    .findAny();
            // add inputs/outputs
            if (optWft.isPresent()) {
                WorkflowTask wft = optWft.get();
                awos.addAll(wft.activate(rootWCE)); // activate task
                for (ArtifactInput in : evt.getOptionalInputs()) {
                    if (in instanceof LazyLoadingArtifactInput) {
                    	((LazyLoadingArtifactInput) in).reinjectRegistry(artReg);
                    } else {
                        for (IArtifact a : in.getArtifacts()) {
                            artReg.injectArtifactService(a, evt.getId());
                        }
                    }
                    awos.addAll(wft.addInput(in, rootWCE)); 
                }
                for (ArtifactOutput out : evt.getOptionalOutputs()) {
                   if (out instanceof LazyLoadingArtifactOutput) {
                	   ((LazyLoadingArtifactOutput) out).reinjectRegistry(artReg);
                   } else {
                       for (IArtifact a : out.getArtifacts()) {
                           artReg.injectArtifactService(a, evt.getId());
                       }
                   }
                   awos.addAll(wft.addOutput(out, rootWCE));
                }
            }
        }
        return awos;
    }

    public List<WorkflowChangeEvent> handle(RemovedInputEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
    	ArtifactInput ai = new LazyLoadingArtifactInput(evt.getArtifact() == null ? Collections.emptySet() : Set.of(evt.getArtifact()), artReg, wfi.getId(), evt.getRole());
    	List<WorkflowChangeEvent> awos = new LinkedList<>();
    	//List<ArtifactInput> inputs;
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
            if (evt.getId().equals(evt.getWftId())) { // we remove inputs from the workflow instance
                //inputs = wfi.getInput();
            	 awos.addAll(wfi.removeInput(ai, rootWCE));
            } else {
            	log.info("No Task or Process found to apply event: "+evt.toString());
                return Collections.emptyList();
            }
        } else {
          //  inputs = wft.getInput();
        	awos.addAll(wft.removeInput(ai, rootWCE));
        }
//        Optional<ArtifactInput> opt = inputs.stream()
//                .filter(i -> i.getRole().equals(evt.getRole()))
//                .findAny();
//        if (opt.isPresent()) {
//            ArtifactInput in = opt.get();
//            artReg.get(evt.getArtifact(), evt.getId()).ifPresent(in::removeArtifact);
//            if (in.getArtifacts().size() == 0 && wft != null) {
//                awos.addAll(wft.removeInput(in)); //FIXME: this should return the events
//            }
//        }
        return awos;
    }

    public List<WorkflowChangeEvent> handle(RemovedOutputEvt evt) {
    	WorkflowChangeEvent rootWCE = evt.getParentCauseRef() != null ? new RootWorkflowChangeEvent(evt.getParentCauseRef(), wfi) : null;
    	ArtifactOutput ao = new LazyLoadingArtifactOutput(evt.getArtifact() == null ? Collections.emptySet() : Set.of(evt.getArtifact()), artReg, wfi.getId(), evt.getRole());
    	List<WorkflowChangeEvent> awos = new LinkedList<>();
    	//List<ArtifactOutput> outputs;
        IWorkflowTask wft = wfi.getWorkflowTask(evt.getWftId());
        if (wft == null) {
            if (evt.getId().equals(evt.getWftId())) {
                //outputs = wfi.getOutput();
                awos.addAll(wfi.removeOutput(ao, rootWCE));
            } else {
            	log.info("No Task or Process found to apply event: "+evt.toString());
                return Collections.emptyList();
            }
        } else {
        	awos.addAll(wft.removeOutput(ao, rootWCE));
            //outputs = wft.getOutput();
        }
//        Optional<ArtifactOutput> opt = outputs.stream()
//                .filter(o -> o.getRole().equals(evt.getRole()))
//                .findAny();
//        if (opt.isPresent()) {
//            ArtifactOutput out = opt.get();
//            awos.addAll(opt.get().re)
//            
//            artReg.get(evt.getArtifact(), evt.getId()).ifPresent(out::removeArtifact); 
//            if (out.getArtifacts().size() == 0 && wft != null) { 
//                awos.addAll(wft.removeOutput(out)); 
//            }
//        }
        return awos;
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
        } else if (evt instanceof SetPostConditionsFulfillmentEvt) {
            handle((SetPostConditionsFulfillmentEvt) evt);
        } else if (evt instanceof ActivatedTaskEvt) {
            handle((ActivatedTaskEvt) evt);
        } else if (evt instanceof ChangedCanceledStateOfTaskEvt) {
            handle((ChangedCanceledStateOfTaskEvt) evt);
        } else if (evt instanceof SetPropertiesEvt) {
            handle((SetPropertiesEvt) evt);
        } else if (evt instanceof InstantiatedTaskEvt) {
            handle((InstantiatedTaskEvt) evt);
        } else if (evt instanceof RemovedInputEvt) {
            handle((RemovedInputEvt) evt);
        } else if (evt instanceof RemovedOutputEvt) {
            handle((RemovedOutputEvt) evt);
        } else if (evt instanceof UpdatedEvaluationTimeEvt) {
            handle((UpdatedEvaluationTimeEvt) evt);
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

    public Optional<RuleEngineBasedConstraint> getRebc(String rebcId) {
        for (WorkflowTask wft : wfi.getWorkflowTasksReadonly()) {
            Optional<RuleEngineBasedConstraint> opt = getRebc(wft, rebcId);
            if (opt.isPresent()) {
                return opt;
            }
        }
        return Optional.empty();
    }

    public Optional<RuleEngineBasedConstraint> getRebc(String wftId, String rebcId) {
        IWorkflowTask wft = wfi.getWorkflowTask(wftId);
        if (wft != null) {
//            Optional<RuleEngineBasedConstraint> opt = getRebc(wft, rebcId);
//            if (opt.isEmpty())
//                log.debug("EMPTY!!! ({})", rebcId);
            return getRebc(wft, rebcId);
        }
        return Optional.empty();
    }

    private Optional<RuleEngineBasedConstraint> getRebc(IWorkflowTask wft, String rebcId) {
        return wft.getOutput().stream()
                .map(ArtifactIO::getArtifacts)
                .flatMap(Collection::stream)
                .filter(a -> a instanceof QACheckDocument)
                .map(a -> ((QACheckDocument)a).getConstraintsReadonly())
                .flatMap(Collection::stream)
                .filter(qac -> qac.getId().equals(rebcId))
                .filter(qac -> qac instanceof RuleEngineBasedConstraint)
                .map(qac -> (RuleEngineBasedConstraint)qac)
                .findAny();
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
