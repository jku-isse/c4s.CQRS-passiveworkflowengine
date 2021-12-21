package impactassessment.command;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.api.Commands.*;
import impactassessment.api.Events.*;
import impactassessment.command.model.CmdConstraint;
import impactassessment.command.model.CmdTask;
import impactassessment.command.model.CmdWorkflow;
import impactassessment.passiveprocessengine.LazyLoadingArtifactInput;
import impactassessment.passiveprocessengine.LazyLoadingArtifactOutput;
import impactassessment.registry.WorkflowDefinitionContainer;
import impactassessment.registry.WorkflowDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate(snapshotTriggerDefinition="workflowSnapshotTrigger")
@Profile("command")
@Slf4j
public class WorkflowAggregate implements Serializable {

    @AggregateIdentifier
    String id;
    private String parentWfiId; // also parent aggregate id
    private String parentWftId;

    private CmdWorkflow model;

    public WorkflowAggregate() {
        log.info("[AGG] empty constructor WorkflowAggregate invoked");
    }

    private void applyList(Stream<TimedEvt> eventList) {
    	eventList.filter(Objects::nonNull).forEach(evt -> apply(evt));
    }
    
    // -------------------------------- Constructors -------------------------------
    
    @CommandHandler
    public WorkflowAggregate(CreateWorkflowCmd cmd, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
        log.info("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, workflowDefinitionRegistry));
    }
    
    
    protected static Stream<TimedEvt> mapToEvent(CreateWorkflowCmd cmd, WorkflowDefinitionRegistry workflowDefinitionRegistry){
    	 WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
    	if (wfdContainer != null) {
        	TimedEvt evt = new CreatedWorkflowEvt(cmd.getId(), cmd.getInput(), cmd.getDefinitionName(), wfdContainer.getWfd());
        	evt.setParentCauseRef(cmd.getParentCauseRef());
            return Stream.of(evt);
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
            return Stream.empty();
        }
    }
//    @CommandHandler
//    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
//    public void handle(CreateWorkflowCmd cmd, IArtifactRegistry artifactRegistry, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
//        log.info("[AGG] handling {}", cmd);
//        if (!this.isNew) {
//        	log.warn("Attempt to create workflow again ignored "+cmd.getId());
//        	return;
//        } 
//        Collection<Entry<String,IArtifact>> artifacts = mapWorkflowInput(cmd.getId(), artifactRegistry, cmd.getInput());
//        WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
//        if (wfdContainer != null) {
//            apply(new CreatedWorkflowEvt(cmd.getId(), artifacts
//            											.stream()
//            											.map(entry -> new AbstractMap.SimpleEntry<String, ArtifactIdentifier>(entry.getKey(), entry.getValue().getArtifactIdentifier())) 
//            											.collect(Collectors.toList())
//            								, cmd.getDefinitionName(), wfdContainer.getWfd()));
//        } else {
//            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
//        }
//    }
    
    @CommandHandler
    public WorkflowAggregate(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry workflowDefinitionRegistry) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, workflowDefinitionRegistry));
    }

    public String getId() {
        return id;
    }

    protected static Stream<TimedEvt> mapToEvent(CreateSubWorkflowCmd cmd, WorkflowDefinitionRegistry workflowDefinitionRegistry){
    	WorkflowDefinitionContainer wfdContainer = workflowDefinitionRegistry.get(cmd.getDefinitionName());
        if (wfdContainer != null) {
            TimedEvt evt = new CreatedSubWorkflowEvt(cmd.getId(), cmd.getParentWfiId(), cmd.getParentWftId(), cmd.getDefinitionName(), wfdContainer.getWfd(), cmd.getInput());
            evt.setParentCauseRef(cmd.getParentCauseRef());
            return Stream.of(evt);
        } else {
            log.error("Workflow Definition named {} not found in registry!", cmd.getDefinitionName());
            return Stream.empty();
        }
    }
    
    // -------------------------------- Command Handlers --------------------------------

    
    
    
    @CommandHandler
    public void handle(CompositeCmd cmd, IArtifactRegistry artifactRegistry) {
    	log.debug("[AGG] handling {}", cmd);
    	applyList(mapToEvent(cmd, artifactRegistry, parentWfiId, parentWftId));
    }
    
    protected static Stream<TimedEvt> mapToEvent(CompositeCmd cmd, IArtifactRegistry artifactRegistry, String parentWfiId, String parentWftId) {
    	//TODO: whenever this changes, also update MockCommandGateway!!!!
    	Map<String, List<TimedEvt>> perAggr =  cmd.getCommandList().stream()
        		.flatMap(subcmd -> {
        			if (subcmd instanceof DeleteCmd)
        				return mapToEvent((DeleteCmd)subcmd);
        			if (subcmd instanceof AddConstraintsCmd)
        				return mapToEvent((AddConstraintsCmd) subcmd);
        			if (subcmd instanceof AddEvaluationResultToConstraintCmd)
        				return mapToEvent((AddEvaluationResultToConstraintCmd) subcmd);
        			if (subcmd instanceof CheckConstraintCmd)
        				return mapToEvent((CheckConstraintCmd) subcmd); 
        			if (subcmd instanceof CheckAllConstraintsCmd)
        				return mapToEvent((CheckAllConstraintsCmd) subcmd); 
        			if (subcmd instanceof AddInputCmd)
        				return mapToEvent((AddInputCmd) subcmd, artifactRegistry); 
        			if (subcmd instanceof AddOutputCmd)
        				return  mapToEvent((AddOutputCmd) subcmd, artifactRegistry, parentWfiId, parentWftId);
        			if (subcmd instanceof AddInputToWorkflowCmd)
        				return  mapToEvent((AddInputToWorkflowCmd ) subcmd, artifactRegistry);
        			if (subcmd instanceof AddOutputToWorkflowCmd)
        				return  mapToEvent((AddOutputToWorkflowCmd ) subcmd, artifactRegistry);
        			if (subcmd instanceof UpdateArtifactsCmd)
        				return  mapToEvent(( UpdateArtifactsCmd) subcmd);
        			if (subcmd instanceof SetPreConditionsFulfillmentCmd)
        				return  mapToEvent((SetPreConditionsFulfillmentCmd ) subcmd);
        			if (subcmd instanceof SetPostConditionsFulfillmentCmd)
        				return  mapToEvent((SetPostConditionsFulfillmentCmd ) subcmd);
        			if (subcmd instanceof ActivateTaskCmd)
        				return  mapToEvent((ActivateTaskCmd ) subcmd);
        			if (subcmd instanceof ChangeCanceledStateOfTaskCmd)
        				return  mapToEvent((ChangeCanceledStateOfTaskCmd ) subcmd);
        			if (subcmd instanceof SetPropertiesCmd)
        				return  mapToEvent((SetPropertiesCmd ) subcmd);
        			if (subcmd instanceof InstantiateTaskCmd)
        				return  mapToEvent((InstantiateTaskCmd ) subcmd, artifactRegistry);
        			if (subcmd instanceof RemoveInputCmd)
        				return  mapToEvent(( RemoveInputCmd) subcmd);
        			if (subcmd instanceof RemoveOutputCmd)
        				return  mapToEvent((RemoveOutputCmd ) subcmd);
        			else 
        				return Stream.empty();
        		})
        		.collect(Collectors.groupingBy(event -> event.getId())); // produces Map<String, List<TimedEvt>>
        		return perAggr.entrySet().stream().map(entry -> { 
        								CompositeEvt ce = new CompositeEvt(entry.getKey(), entry.getValue()) ;
        								ce.setParentCauseRef(cmd.getParentCauseRef());
        								return ce;
        							});       	
    }
    
    @CommandHandler
    public void handle(DeleteCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList( mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(DeleteCmd cmd) {
    	return Stream.of(new DeletedEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddConstraintsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd)); 
    }
    
    protected static Stream<TimedEvt> mapToEvent(AddConstraintsCmd cmd) {
        return Stream.of(new AddedConstraintsEvt(cmd.getId(), cmd.getWftId(), cmd.getRules()));
    }

    @CommandHandler
    public void handle(AddEvaluationResultToConstraintCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        if (model.getConstraint(cmd.getWftId(), cmd.getQacId()).isEmpty() || model.getConstraint(cmd.getWftId(), cmd.getQacId()).get().hasChanged(cmd.getRes())) {
            log.debug("AddEvaluationResultToConstraintCmd caused --> AddedEvaluationResultToConstraintEvt");
            TimedEvt evt = new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime());
            evt.setParentCauseRef(cmd.getParentCauseRef());
            apply(evt);
        } else {
            log.debug("AddEvaluationResultToConstraintCmd caused --> UpdatedEvaluationTimeEvt");
            TimedEvt evt = new UpdatedEvaluationTimeEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getCorr(), cmd.getTime());
            evt.setParentCauseRef(cmd.getParentCauseRef());
            apply(evt);
        }
        //TODO: replace with mapped version 
    }
    
    protected static Stream<TimedEvt> mapToEvent(AddEvaluationResultToConstraintCmd cmd) {
       TimedEvt evt = new AddedEvaluationResultToConstraintEvt(cmd.getId(), cmd.getWftId(), cmd.getQacId(), cmd.getRes(), cmd.getCorr(), cmd.getTime());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }

    @CommandHandler
    public void handle(CheckConstraintCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(CheckConstraintCmd cmd) {
        return Stream.of(new CheckedConstraintEvt(cmd.getId(), cmd.getConstrId()));
    }

    @CommandHandler
    public void handle(CheckAllConstraintsCmd cmd) {
    	log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd)); 
    }
    
    protected static Stream<TimedEvt> mapToEvent(CheckAllConstraintsCmd cmd) {
        return Stream.of(new CheckedAllConstraintsEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(AddInputCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, artifactRegistry)); 
    }
    
    protected static Stream<TimedEvt> mapToEvent(AddInputCmd cmd, IArtifactRegistry artifactRegistry) {
        ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        if (opt.isPresent()) {
        	TimedEvt evt = new AddedInputEvt(cmd.getId(), cmd.getWftId(), opt.get().getArtifactIdentifier(), cmd.getRole());
            evt.setParentCauseRef(cmd.getParentCauseRef());
        	return Stream.of(evt);
        } else {
            log.warn("Artifact {} was not found.", cmd.getArtifactId());
            return Stream.empty();
        }
    }

    @CommandHandler
    public void handle(AddOutputCmd cmd, IArtifactRegistry artifactRegistry) {
    	 log.debug("[AGG] handling {}", cmd);
         applyList(mapToEvent(cmd, artifactRegistry, parentWfiId, parentWftId)); 
    }

    protected static Stream<TimedEvt> mapToEvent(AddOutputCmd cmd, IArtifactRegistry artifactRegistry, String parentWfiId, String parentWftId) {
    	
    	ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        List<TimedEvt> eventList = new ArrayList<>();
        if (opt.isPresent()) {
        	TimedEvt evt = new AddedOutputEvt(cmd.getId(), cmd.getWftId(), ai, cmd.getRole());
            evt.setParentCauseRef(cmd.getParentCauseRef());
            eventList.add(evt);
            if (parentWfiId != null && parentWftId != null) {
            	TimedEvt evt2 = new AddedOutputEvt(parentWfiId, parentWftId, ai, cmd.getRole());
                 evt2.setParentCauseRef(cmd.getParentCauseRef());
                 eventList.add(evt2);
            }
        } else {
            log.warn("Artifact {} was not found.", cmd.getArtifactId());
        }
        return eventList.stream();
    }
    
    @CommandHandler
    public void handle(AddInputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, artifactRegistry));
    }

    protected static Stream<TimedEvt> mapToEvent(AddInputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
    	ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());
        Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
        if (opt.isPresent()) {
        	TimedEvt evt = new AddedInputToWorkflowEvt(cmd.getId(), opt.get().getArtifactIdentifier(), cmd.getRole());
            evt.setParentCauseRef(cmd.getParentCauseRef());
        	return Stream.of(evt);
        }
        else return Stream.empty();
    }
    
    @CommandHandler
    public void handle(AddOutputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, artifactRegistry));
    }

    protected static Stream<TimedEvt> mapToEvent(AddOutputToWorkflowCmd cmd, IArtifactRegistry artifactRegistry) {
    	ArtifactIdentifier ai = new ArtifactIdentifier(cmd.getArtifactId(), cmd.getType());

    	Optional<IArtifact> opt = artifactRegistry.get(ai, cmd.getId());
    	if (opt.isPresent()) {
        	TimedEvt evt = new AddedOutputToWorkflowEvt(cmd.getId(), opt.get().getArtifactIdentifier(), cmd.getRole());
            evt.setParentCauseRef(cmd.getParentCauseRef());
        	return Stream.of(evt);
        }
        else return Stream.empty();
    }
    
    @CommandHandler
    public void handle(UpdateArtifactsCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }

    protected static Stream<TimedEvt> mapToEvent(UpdateArtifactsCmd cmd) {
    	TimedEvt evt = new UpdatedArtifactsEvt(cmd.getId(), cmd.getArtifacts().stream().map(IArtifact::getArtifactIdentifier).collect(Collectors.toList()));
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }
    
    @CommandHandler
    public void handle(SetPreConditionsFulfillmentCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(SetPreConditionsFulfillmentCmd cmd) {
        TimedEvt evt = new SetPreConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }

    @CommandHandler
    public void handle(SetPostConditionsFulfillmentCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }

    protected static Stream<TimedEvt> mapToEvent(SetPostConditionsFulfillmentCmd cmd) {
        TimedEvt evt = new SetPostConditionsFulfillmentEvt(cmd.getId(), cmd.getWftId(), cmd.isFulfilled());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }
    
    @CommandHandler
    public void handle(ActivateTaskCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(ActivateTaskCmd cmd) {
        TimedEvt evt = new ActivatedTaskEvt(cmd.getId(), cmd.getWftId());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }
    
    @CommandHandler
    public void handle(ChangeCanceledStateOfTaskCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(ChangeCanceledStateOfTaskCmd cmd) {
        TimedEvt evt = new ChangedCanceledStateOfTaskEvt(cmd.getId(), cmd.getWftId(), cmd.isCanceled());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }

    @CommandHandler
    public void handle(SetPropertiesCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }
    
    protected static Stream<TimedEvt> mapToEvent(SetPropertiesCmd cmd) {
        TimedEvt evt = new SetPropertiesEvt(cmd.getId(), cmd.getIwftId(), cmd.getProperties());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }

    @CommandHandler
    public void handle(InstantiateTaskCmd cmd, IArtifactRegistry artifactRegistry) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd, artifactRegistry));
    }
    
    protected static Stream<TimedEvt> mapToEvent(InstantiateTaskCmd cmd, IArtifactRegistry artifactRegistry) {
        TimedEvt evt = new InstantiatedTaskEvt(cmd.getId(), cmd.getTaskDefinitionId(),
				cmd.getOptionalInputs().stream().map(in -> LazyLoadingArtifactInput.generateFrom(in, artifactRegistry, cmd.getId())).collect(Collectors.toList())  , 
				cmd.getOptionalOutputs().stream().map(out -> LazyLoadingArtifactOutput.generateFrom(out, artifactRegistry, cmd.getId())).collect(Collectors.toList())   );
                evt.setParentCauseRef(cmd.getParentCauseRef());
       return Stream.of(evt);
    }

    @CommandHandler
    public void handle(RemoveInputCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }

    protected static Stream<TimedEvt> mapToEvent(RemoveInputCmd cmd) {
        TimedEvt evt = new RemovedInputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }
    
    @CommandHandler
    public void handle(RemoveOutputCmd cmd) {
        log.debug("[AGG] handling {}", cmd);
        applyList(mapToEvent(cmd));
    }

    protected static Stream<TimedEvt> mapToEvent(RemoveOutputCmd cmd) {
        TimedEvt evt = new RemovedOutputEvt(cmd.getId(), cmd.getWftId(), cmd.getArtifactId(), cmd.getRole());
        evt.setParentCauseRef(cmd.getParentCauseRef());
        return Stream.of(evt);
    }
    
    // -------------------------------- Event Handlers --------------------------------

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        model = new CmdWorkflow(id);
    }

    @EventSourcingHandler
    public void on(CreatedSubWorkflowEvt evt) {
        log.debug("[AGG] applying {}", evt);
        id = evt.getId();
        parentWfiId = evt.getParentWfiId();
        parentWftId = evt.getParentWftId();
    }

    @EventSourcingHandler
    public void on(DeletedEvt evt) {
        log.debug("[AGG] applying {}", evt);
        markDeleted();
    }

    @EventSourcingHandler
    public void on(AddedConstraintsEvt evt) {
        log.debug("[AGG] applying {}", evt);
        CmdTask task = new CmdTask(evt.getWftId());
        model.add(task);
    }

    @EventSourcingHandler
    public void on(AddedEvaluationResultToConstraintEvt evt) {
        log.debug("[AGG] applying {}", evt);
        model.getConstraint(evt.getWftId(), evt.getQacId())
                .ifPresentOrElse(
                        constr -> constr.update(evt.getRes()),
                        () -> model.getTask(evt.getWftId())
                                .ifPresent(task -> {
                                    CmdConstraint constr = new CmdConstraint(evt.getQacId());
                                    constr.update(evt.getRes());
                                    task.add(constr);
                                })
                );
    }

    // this event handler processes all events (if not already treated by above)
    // because every event inherits from IdentifiableEvt
    @EventSourcingHandler
    public void on(IdentifiableEvt evt) {
        log.debug("[AGG] applying {}", evt);
    }

}
