package impactassessment.passiveprocessengine.instance;

import java.util.*;
import java.util.stream.Collectors;

import impactassessment.passiveprocessengine.definition.*;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.github.oxo42.stateless4j.StateMachine;

import impactassessment.neo4j.DNIStatemachineConverter;
import impactassessment.passiveprocessengine.definition.DecisionNodeDefinition.Events;
import impactassessment.passiveprocessengine.definition.DecisionNodeDefinition.States;
import impactassessment.passiveprocessengine.instance.IBranchInstance.BranchState;

@Slf4j
public class DecisionNodeInstance extends AbstractWorkflowInstanceObject {

	@Relationship(type="SPECIFIED_BY")
	private DecisionNodeDefinition ofType;
	
	@Relationship(type="BRANCH_INSTANCE", direction=Relationship.INCOMING)
	private HashSet<IBranchInstance> inBranches = new HashSet<IBranchInstance>();
	//private transient HashMap<String, AbstractBranchInstance> inBranches = new HashMap<String, AbstractBranchInstance>();
	
	@Relationship(type="BRANCH_INSTANCE", direction=Relationship.OUTGOING)
	private HashSet<IBranchInstance> outBranches = new HashSet<IBranchInstance>();
	//protected transient HashMap<String, AbstractBranchInstance> outBranches = new HashMap<String,AbstractBranchInstance>();
	
//	private transient static AbstractBranchInstance dummy = new DefaultBranchInstance(null, null);
	@Property
	boolean taskCompletionConditionsFullfilled = false;
	@Property
	boolean taskActivationConditionsFullfilled = false;
	@Property
	boolean contextConditionsFullfilled = false;
	@Property
	boolean activationPropagationCompleted = false;
	@Convert(DNIStatemachineConverter.class)
	private StateMachine<DecisionNodeDefinition.States, DecisionNodeDefinition.Events> sm;

	private List<MappingReport> mappingReports = new ArrayList<>();

	public List<MappingReport> getMappingReports() {
		return mappingReports;
	}

	@Deprecated
	public DecisionNodeInstance() {
		super();
	}
	
	public DecisionNodeInstance(DecisionNodeDefinition ofType, WorkflowInstance workflow, StateMachine<DecisionNodeDefinition.States, DecisionNodeDefinition.Events> sm) {
		//super(ofType.getId()+"#"+UUID.randomUUID().toString(), workflow);
		super(ofType.getId()+"#"+workflow.getId().toString(), workflow);
		this.ofType = ofType;
		this.sm = sm;
		if (!ofType.hasExternalInBranchRules  || ofType.getInBranches().isEmpty()) {
			this.taskCompletionConditionsFullfilled = true;
			sm.fire(Events.INBRANCHES_FULFILLED);
		}
		if (!ofType.hasExternalContextRules) {
			this.contextConditionsFullfilled = true;
			if (sm.canFire(Events.CONTEXT_FULFILLED)) // only when also inbranch conditions were already fulfilled, other wise transition triggered later
				sm.fire(Events.CONTEXT_FULFILLED);
		}
		if (!ofType.hasExternalOutBranchRules) {
			this.taskActivationConditionsFullfilled = true;
			if (sm.canFire(Events.OUTBRANCHES_FULFILLED)) // only when also context conditions were already fulfilled, other wise transition triggered later
				sm.fire(Events.OUTBRANCHES_FULFILLED);
		}
	}
	
	public void addInBranches(Collection<IBranchInstance> inBranches) {
		this.inBranches.addAll(inBranches);
	}
	
	public void addOutBranches(Collection<IBranchInstance> outBranches) {
		this.outBranches.addAll(outBranches);
	}

	public Set<IBranchInstance> getOutBranches() {
		return Collections.unmodifiableSet(outBranches);
	}

	public Set<IBranchInstance> getInBranches() {
		return Collections.unmodifiableSet(inBranches);
	}
	
	public DecisionNodeDefinition getDefinition() {
		return ofType;
	}
	
	public States getState() {
		return sm.getState();
	}

	public long getInConditionsOpenUnfullfilled() {
		return inBranches.stream()
			.filter(b -> (b.getState() == IBranchInstance.BranchState.Waiting))
			.count();
	}
		
//	public void setInConditionsFullfilled() {
//		inBranches.values().stream()
//			.forEach(b -> b.setConditionsFulfilled());
//		tryInConditionsFullfilled();
//	}
	
	public Set<AbstractWorkflowInstanceObject> tryInConditionsFullfilled() {
		switch(ofType.getInBranchingType()) {
		case AND: 
			// fallthrough: OR also waits for all activated tasks, thus activated branches to have their completion condition set == TransitionEnabled
		case OR:
			if (inBranches.stream()
					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
					.allMatch(b -> b.getState()==BranchState.TransitionEnabled)) {
				inBranches.stream()
					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
					.forEach(b -> b.setBranchUsedForProgress()); // not sure if we should set progress already here (wait for outbranch activiation)
				return this.setTaskCompletionConditionsFullfilled();
			}
			break;
		case XOR:
			if (inBranches.stream()
					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
					.anyMatch(b -> b.getState()==BranchState.TransitionEnabled)) {
					Optional<IBranchInstance> selectedB = inBranches.stream()
						.filter(b -> b.getState()!=BranchState.TransitionEnabled)
						.findAny() 
						.map(b -> {
							b.setBranchUsedForProgress();
							return b;
						});
					if (selectedB.isPresent()) {
						// we need to mark other branch tasks as: disabled (if they are available or active), or canceled otherwise 
						inBranches.stream()
							.filter(b -> b.getState()!=BranchState.TransitionPassed) // filter out the branch we just tagged as used
							.map(b -> b.getTask())
							.forEach(t -> { 
								t.signalEvent(TaskLifecycle.Events.IGNORE_FOR_PROGRESS);
//								switch(t.getLifecycleState()) {
//								case AVAILABLE:
//								case ENABLED:
//									t.setLifecycleState(TaskLifecycle.State.DISABLED);
//									break;
//								case DISABLED:
//								case CANCELED:
//									break;
//								default:
//									t.setLifecycleState(TaskLifecycle.State.CANCELED);
//									break;
//								}
							}); 
						return this.setTaskCompletionConditionsFullfilled();
					}
			}
			break;
		default:
			break;
		}
		return Collections.emptySet();
	}
	
	public Set<AbstractWorkflowInstanceObject> setTaskCompletionConditionsFullfilled() {
	// set externally from Rule Engine, or internally once in branches signal sufficient
		this.taskCompletionConditionsFullfilled = true;
		// advance state machine
		sm.fire(Events.INBRANCHES_FULFILLED);
		return tryContextConditionsFullfilled();
	}
	
	public Set<AbstractWorkflowInstanceObject> setTaskCompletionConditionsNoLongerHold() {
		// set externally from Rule Engine, or internally once in branches signal sufficient
			this.taskCompletionConditionsFullfilled = false;
			// advance state machine
			sm.fire(Events.INCONDITIONS_NO_LONGER_HOLD);
			// TODO: check what needs to be done
			//return tryContextConditionsFullfilled();
			return Collections.emptySet();
		}

	public Set<AbstractWorkflowInstanceObject> tryContextConditionsFullfilled() {
		if (isContextConditionsFullfilled() || !ofType.hasExternalContextRules) {// if context conditions already evaluated to true, OR DND claims there are no external rules
			return setContextConditionsFullfilled(); 
		}
		return Collections.emptySet();
	}
	
	public boolean isTaskCompletionConditionsFullfilled() {
		return taskCompletionConditionsFullfilled;
	}

	
	
//	public void setOutConditionsFullfilled() {
//		outBranches.values().stream()
//			.forEach(b -> b.setConditionsFulfilled());
//	}
	
	public boolean isActivationPropagationCompleted() {
		return activationPropagationCompleted;
	}

	public boolean isContextConditionsFullfilled() {
		return contextConditionsFullfilled;
	}

	public Set<AbstractWorkflowInstanceObject> setContextConditionsFullfilled() { 
		this.contextConditionsFullfilled = true; // in any case set context to true again
		if (sm.canFire(Events.CONTEXT_FULFILLED)) { // only when in right state, do we progress, e.g., if just to signal context ok, but in task completion not, then dont progress
			sm.fire(Events.CONTEXT_FULFILLED);
			return tryOutConditionsFullfilled(); 
		}
		return Collections.emptySet();
	}

	public Set<AbstractWorkflowInstanceObject> setContextConditionsNoLongerHold() {
		this.contextConditionsFullfilled = false;
		if (sm.canFire(Events.CONTEXT_NO_LONGER_HOLD)) { // only when in right state, i.e., when transition matters
			sm.fire(Events.CONTEXT_NO_LONGER_HOLD);
		}
		// TODO: check what needs to be done
		//return tryOutConditionsFullfilled(); 
		return Collections.emptySet();
	}
	
	public Set<AbstractWorkflowInstanceObject> tryOutConditionsFullfilled() {
		switch(ofType.getOutBranchingType()) {
		case AND: 
			if (outBranches.stream()
					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
					.allMatch(b -> b.getState()==BranchState.TransitionEnabled)) {
//				outBranches.values().stream()
//					.forEach(b -> b.setBranchUsedForProgress()); // if we
				this.taskActivationConditionsFullfilled = true;
				if (sm.canFire(Events.OUTBRANCHES_FULFILLED))
					sm.fire(Events.OUTBRANCHES_FULFILLED);
				return tryDataflowFreeActivationPropagation();
			}
			break;
		case OR:
			// fallthrough, we only need at least one to continue
		case XOR:
			if (outBranches.stream()
					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
					.anyMatch(b -> b.getState()==BranchState.TransitionEnabled)) {
				this.taskActivationConditionsFullfilled = true;
				if (sm.canFire(Events.OUTBRANCHES_FULFILLED))
					sm.fire(Events.OUTBRANCHES_FULFILLED);
				return tryDataflowFreeActivationPropagation();
			}
			break;
		default:
		}
		return Collections.emptySet();
	}
	
	public boolean isTaskActivationConditionsFullfilled() {
		return taskActivationConditionsFullfilled;
	}

	public Set<AbstractWorkflowInstanceObject> activateOutBranch(String branchId) {
		//outBranches.getOrDefault(branchId, dummy).setConditionsFulfilled();
		outBranches.stream()
		.filter(b -> b.getBranchDefinition().getName().equals(branchId))
		.findAny()
		.ifPresent(b -> b.setConditionsFulfilled());
		return tryOutConditionsFullfilled();
	}
	
	public Set<AbstractWorkflowInstanceObject> activateOutBranches(String... branchIds) {
		for (String id : branchIds) {
			outBranches.stream()
			.filter(b -> b.getBranchDefinition().getName().equals(id))
			.findAny()
			.ifPresent(b -> b.setConditionsFulfilled());
		}
		return tryOutConditionsFullfilled();
	}
	
	public Set<AbstractWorkflowInstanceObject> activateInBranch(String branchId) {
		//inBranches.getOrDefault(branchId, dummy).setConditionsFulfilled();
		inBranches.stream()
		.filter(b -> b.getBranchDefinition().getName().equals(branchId))
		.findAny()
		.ifPresent(b -> b.setConditionsFulfilled());
		return tryInConditionsFullfilled();
	}
	
	public Map<IBranchInstance, WorkflowTask> calculatePossibleActivationPropagation() {
		// checks whether output tasks can be activated
		// can only activate tasks but not provide dataflow
		if (inBranches.stream()
			.allMatch(b -> b.getState() == BranchState.TransitionEnabled)
			&& this.contextConditionsFullfilled) { // only if input and context conditions fulfilled
			return outBranches.stream() // return for each enabled outbranch the respective workflow task instance (not part of process yet)
				.filter(b -> b.getState() == BranchState.TransitionEnabled && !b.hasTask() ) // && !b.getBranchDefinition().hasDataFlow()
				.collect(Collectors.toMap(b -> b, b -> workflow.prepareTask(b.getBranchDefinition().getTask())));
		}
		return Collections.emptyMap();	
	}
	
	public Set<AbstractWorkflowInstanceObject> tryDataflowFreeActivationPropagation() { // TODO add automatic dataflow propagation
		// checks whether output tasks can be activated
		// can only activate tasks but not provide dataflow
		if (sm.isInState(States.PASSED_OUTBRANCH_CONDITIONS) &&  // only if input, context, and activation conditions fulfilled
			// THIS ACTIVATES ALL TASKS ON ENABLED BRANCHES, EVEN FOR XOR: the user can then choose what to execute, thus deactivation needs to happen upon activation/work output by user
			// WHILE NO USER WORKS/ACTIVATES, remaining branches might still be activated
			// continued if condition:
			// works only if no branch has dataflow and no task associated to it yet, otherwise rule needs to activate tasks and branches
			outBranches.stream()
					.filter(b -> b.getState() == BranchState.TransitionEnabled && !b.hasTask())
					.allMatch(b -> !b.getBranchDefinition().hasDataFlow())) 
		{		
			Set<AbstractWorkflowInstanceObject> awfos =
				outBranches.stream() // return for each enabled outbranch the respective workflow task instance (not part of process yet)
				.filter(b -> b.getState() == BranchState.TransitionEnabled && !b.hasTask() &&  !b.getBranchDefinition().hasDataFlow())
				.flatMap(b -> { List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
								WorkflowTask wft = workflow.instantiateTask(b.getBranchDefinition().getTask());
								awos.add(wft);
								awos.addAll(workflow.activateDecisionNodesFromTask(wft));
								return awos.stream();
					})
				.collect(Collectors.toSet());
			if (!awfos.isEmpty()) {
				sm.fire(Events.PROGRESS_TRIGGERED);
				return awfos;
			}
		}
		return Collections.emptySet();	
	}
	
	public void completedDataflowInvolvingActivationPropagation() {
		outBranches.stream()
			.forEach(IBranchInstance::setBranchUsedForProgress);
		// not necessary for outbranches as we set them via task assignment --> no we dont
		activationPropagationCompleted = true;
		if (sm.canFire(Events.PROGRESS_TRIGGERED))
			sm.fire(Events.PROGRESS_TRIGGERED);

//		inBranches.values().stream()
//			.filter(b -> b.getState() != BranchState.Disabled)
//			.forEach(b -> b.setBranchUsedForProgress());
		// not necessary as inbranches progress set when checking inbranch conditions
	}
	
//	public boolean acceptsTaskForUnconnectedInBranch(WorkflowTask wti) {
//		// checks if any inBranch yet has not an associated Task,
//		// this check is specific to the DecisionNodeDefinition or the DecisionNodeInstance,
//		// for now we only assume one tasktype per branch, and fixed branch number
//		Optional<AbstractBranchInstance> branch = inBranches.values().stream()
//			.filter(b -> b.hasTask())
//			.filter(b -> b.bd.getTask().equals(wti.getTaskType()))
//			.findFirst();
//		return branch.isPresent();
//	}
	
	public DecisionNodeInstance consumeTaskForUnconnectedInBranch(WorkflowTask wti) {
		// checks if any inBranch yet has not an associated Task,
		// this check is specific to the DecisionNodeDefinition or the DecisionNodeInstance,
		// for now we only assume one tasktype per branch, and fixed branch number
		Optional<IBranchInstance> branch = inBranches.stream()
			.filter(b -> !b.hasTask())
			.filter(b -> b.getBranchDefinition().getTask().equals(wti.getType()))
			.findFirst();
		branch.ifPresent(b -> { b.setTask(wti); 
								this.getWorkflow().registerTaskAsInToDNI(this, wti); 
							  });
		// REQUIRES CHANGE LISTENER TO LET THE RULE ENGINE KNOW, WE UPDATEd THE BRANCH
		if (branch.isPresent())
			return this;
		else
			return null;
	}
	
	public DecisionNodeInstance consumeTaskForUnconnectedOutBranch(WorkflowTask wti) {
		Optional<IBranchInstance> branch = outBranches.stream()
				.filter(b -> b.getState() != BranchState.Disabled)
				.filter(b -> !b.hasTask())
				.filter(b -> b.getBranchDefinition().getTask().getId().equals(wti.getType().getId()))
				.findFirst();
			branch.ifPresent(b -> { 
					b.setTask(wti);
					//b.setBranchUsedForProgress(); 
					this.getWorkflow().registerTaskAsOutOfDNI(this, wti);
				});
			// REQUIRES CHANGE LISTENER TO LET THE RULE ENGINE KNOW, WE UPDATEd THE BRANCH
			if (branch.isPresent())
				return this;
			else
				return null;
	}
	
//	public void defineInBranch(String branchName, WorkflowTask wft) {
//		inBranches.put(branchName, new Branch(branchName, wft));
//	}
//	
//	public void defineOutBranch(String branchName, WorkflowTask wft) {
//		outBranches.put(branchName, new Branch(branchName, wft));
//	}
	
	public List<TaskDefinition> getTaskDefinitionsForNonDisabledOutBranchesWithUnresolvedTasks() {
		return outBranches.stream()
				.filter(b -> b.getState()!=BranchState.Disabled)				
				.filter(b -> b.getTask() == null)
				.map(b -> b.getBranchDefinition().getTask())
				.filter(td -> td != null)
				.collect(Collectors.toList());
	}

	public List<TaskDefinition> getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks() {
		List<TaskDefinition> tds = outBranches.stream()
				.filter(b -> b.getState()==BranchState.TransitionPassed)
				.filter(b -> b.getTask() == null)
				.map(b -> b.getBranchDefinition().getTask())
				.filter(td -> td != null)
				.collect(Collectors.toList());
		return tds;
	}
	
	public List<WorkflowTask> getNonDisabledTasksByInBranchName(String branchName) {
		return inBranches.stream()
				.filter(b -> b.getState()!=BranchState.Disabled)
				.filter(b -> b.getBranchDefinition().getName().equals(branchName))
				.filter(b -> b.hasTask())
				.map(b -> b.getTask())
				.collect(Collectors.toList());
	}
	
	public List<WorkflowTask> getNonDisabledTasksByOutBranchName(String branchName) {
		return outBranches.stream()
				.filter(b -> b.getState()!=BranchState.Disabled)
				.filter(b -> b.getBranchDefinition().getName().equals(branchName))
				.filter(b -> b.hasTask())
				.map(b -> b.getTask())
				.collect(Collectors.toList());
	}
	
	public String getInBranchIdForWorkflowTask(WorkflowTask task) {
		Optional<IBranchInstance> branch = inBranches.stream()
				.filter(b -> b.getTask() != null)
				.filter(b -> b.getTask().equals(task))
				.findFirst();
		return branch.isPresent() ? branch.get().getBranchDefinition().getName() : null;
	}
	
	public String getOutBranchIdForWorkflowTask(WorkflowTask task) {
		Optional<IBranchInstance> branch = outBranches.stream()
			.filter(b -> b.getTask().equals(task))
			.findFirst();
		return branch.isPresent() ? branch.get().getBranchDefinition().getName() : null;
	}

	public IBranchInstance getInBranchForWorkflowTask(WorkflowTask task) {
		Optional<IBranchInstance> branch = inBranches.stream()
				.filter(b -> b.getTask().equals(task))
				.findFirst();
		return branch.orElse(null);
	}

	public IBranchInstance getOutBranchForWorkflowTask(WorkflowTask task) {
		Optional<IBranchInstance> branch = outBranches.stream()
				.filter(b -> b.getTask().equals(task))
				.findFirst();
		return branch.orElse(null);
	}

	public void executeMapping() {
		log.debug("execute mapping");
		for (MappingDefinition m : getDefinition().getMappings()) {
			List<WorkflowTask> fromTasks = getWorkflowTasksFromTaskDefinitionIds(m.getFrom());
			List<WorkflowTask> toTasks = getWorkflowTasksFromTaskDefinitionIds(m.getTo());
			if (toTasks.size() == 0) break;
			for (WorkflowTask preWft : fromTasks) {
				for (ArtifactOutput ao : preWft.getOutput()) {
					if (m.getMappingType().equals(MappingDefinition.MappingType.ANY)) {
						// fitting toTask is selected (if possible)
						WorkflowTask subWft = findBestFit(ao, toTasks);
						if (subWft != null) {
							executeMappingIfNotMappedPrior(preWft, ao, subWft);
						}
					} else if (m.getMappingType().equals(MappingDefinition.MappingType.ALL)) {
						// every toTask is selected
						for (WorkflowTask subWft : toTasks) {
							executeMappingIfNotMappedPrior(preWft, ao, subWft);
						}
					}
				}
			}
		}
	}

	private void executeMappingIfNotMappedPrior(WorkflowTask preWft, ArtifactOutput ao, WorkflowTask subWft) {
		if (mappingReports.stream().noneMatch(r -> r.getFrom().equals(preWft.getId()) && r.getTo().equals(subWft.getId()))) {
			subWft.addInput(new ArtifactInput(ao));
			mappingReports.add(new MappingReport(preWft.getId(), ao.getArtifactType(), ao.getRole(), subWft.getId(), subWft.getType().getExpectedInput()));
		}
	}

	private List<WorkflowTask> getWorkflowTasksFromTaskDefinitionIds(List<String> tdIds) {
		return getWorkflow().getWorkflowTasksReadonly().stream()
				.filter(wft -> tdIds.contains(wft.getType().getId()))
				.collect(Collectors.toList());
	}

	private WorkflowTask findBestFit(ArtifactOutput ao, List<WorkflowTask> subsequentTasks) {
		WorkflowTask onlyTypeMatched = null;
		for (WorkflowTask wft : subsequentTasks) {
			for (Map.Entry<String, ArtifactType> entry : wft.getType().getExpectedInput().entrySet()) {
				if (ao.getArtifactType() != null && entry.getValue().getArtifactType().equals(ao.getArtifactType().getArtifactType())) {
					if (entry.getKey().equals(ao.getRole())) {
						return wft; // type and role matched, so its a perfect fit
					}
					onlyTypeMatched = wft;
				}
			}
		}
		if (onlyTypeMatched == null) {
			return null;
		} else {
			return onlyTypeMatched;
		}
	}

	public boolean isConnected(String workflowTaskID) {
		return inBranches.stream()
			.anyMatch(b -> b.getTask().getId().equals(workflowTaskID))
				||
				outBranches.stream()
				.anyMatch(b -> b.getTask().getId().equals(workflowTaskID));

	}

	@Override
	public String toString() {
		return "DNI [" + ofType + ", " /*+ "("+getState()+")"*/ + workflow + ", "
				+ ", taskCompletionConditionsFullfilled=" + taskCompletionConditionsFullfilled + ", contextConditionsFullfilled=" + contextConditionsFullfilled 
				+ ", taskActivationConditionsFullfilled=" + taskActivationConditionsFullfilled + ", activationPropagationCompleted=" + activationPropagationCompleted
				+ " inBranches=" + inBranches + ", outBranches=" + outBranches  				
				+ "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof DecisionNodeInstance)) return false;
		if (!super.equals(o)) return false;
		DecisionNodeInstance that = (DecisionNodeInstance) o;
		return taskCompletionConditionsFullfilled == that.taskCompletionConditionsFullfilled &&
				taskActivationConditionsFullfilled == that.taskActivationConditionsFullfilled &&
				contextConditionsFullfilled == that.contextConditionsFullfilled &&
				activationPropagationCompleted == that.activationPropagationCompleted;
	}


}
