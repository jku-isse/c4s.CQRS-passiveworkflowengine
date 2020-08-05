package impactassessment.model.workflowmodel;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.definition.type.Modifies;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.delegates.Action2;

import impactassessment.model.workflowmodel.TaskLifecycle.Events;
import impactassessment.model.workflowmodel.TaskLifecycle.InputState;
import impactassessment.model.workflowmodel.TaskLifecycle.OutputState;
import impactassessment.model.workflowmodel.TaskLifecycle.State;
import impactassessment.model.actor.Participant;

@NodeEntity
public class WorkflowTask extends AbstractWorkflowInstanceObject implements java.io.Serializable{
	/**
	 * 
	 */
	private static Logger log = LogManager.getLogger("WorkflowTask");
	private static final long serialVersionUID = -928007034328373520L;
	
	@Relationship(type="SPECIFIED_BY")
	TaskDefinition taskType;
	transient Participant responsibleEngineer;
	@Property
	protected OutputState os = TaskLifecycle.OutputState.OUTPUT_UNKNOWN;
	@Property
	protected InputState is = TaskLifecycle.InputState.INPUT_UNKNOWN;
	@Property
	protected TaskLifecycle.State lifecycleState;// = TaskLifecycle.State.AVAILABLE;
	transient private StateMachine<TaskLifecycle.State, TaskLifecycle.Events> sm;
	transient private TaskStateTransitionEventPublisher pub;
	transient private TaskStateTransitionEvent nextEvent;
	
	@Relationship(type="TASK_IO", direction=Relationship.OUTGOING)
	List<ArtifactOutput> output = new ArrayList<ArtifactOutput>();
	//ObservableList<ArtifactOutput> output = FXCollections.observableList(new ArrayList<ArtifactOutput>());	
	
	@Relationship(type="TASK_IO", direction=Relationship.INCOMING)
	List<ArtifactInput> input = new ArrayList<ArtifactInput>();
	//ObservableList<ArtifactInput> input = FXCollections.observableList(new ArrayList<ArtifactInput>());
	
	@Deprecated
	public WorkflowTask() {
		super();
	}
	
	public WorkflowTask(String taskId, WorkflowInstance wfi, StateMachine<TaskLifecycle.State, TaskLifecycle.Events> sm, TaskStateTransitionEventPublisher pub) {
		super(taskId, wfi);
		this.sm = sm;
		this.pub = pub;
		this.nextEvent = new TaskStateTransitionEvent(lifecycleState, null, this);
		sm.onUnhandledTrigger(new Action2<State,Events>() {
			@Override
			public void doIt(State arg1, Events arg2) {
				log.warn(String.format("Task %s experienced unexpected Event %s for State %s", taskId,  arg2,  arg1));
			}});
		
//		output.addListener(new ListChangeListener<WorkflowTask.ArtifactOutput>(){
//			@Override
//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends ArtifactOutput> c) {
//				while (c.next()) {
//					if (c.getRemovedSize() > 0 || c.getAddedSize() > 0) {
//						os = calcOutputState();
//					} else
//					if (c.wasUpdated()) {
//						boolean doUpdate = false;
//						for (int i = c.getFrom(); i < c.getTo(); i++) {
//							if (c.getList().get(i).getArtifact().isRemovedAtOrigin())
//								doUpdate = true;
//						}
//						if (doUpdate) 
//							os = calcOutputState();
//					}
//				}
//			}			
//		});
//		input.addListener(new ListChangeListener<WorkflowTask.ArtifactInput>(){
//			@Override
//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends ArtifactInput> c) {
//				while (c.next()) {
//					if (c.getRemovedSize() > 0 || c.getAddedSize() > 0) {
//						is = calcInputState();
//					} else
//						if (c.wasUpdated()) {
//							boolean doUpdate = false;
//							for (int i = c.getFrom(); i < c.getTo(); i++) {
//								if (c.getList().get(i).getArtifact().isRemovedAtOrigin())
//									doUpdate = true;
//							}
//							if (doUpdate) 
//								is = calcInputState();
//						}
//				}
//			}			
//		});
	}
	
	
	



	public String getTaskId() {
		return id;
	}

	public TaskDefinition getTaskType() {
		return taskType;
	}

	public TaskLifecycle.State getLifecycleState() {
		if (sm == null) return null;
		lifecycleState = sm.getState();
		return lifecycleState;
	}

//	public void setLifecycleState(TaskLifecycle.State lifecycleState) {
//		this.lifecycleState = lifecycleState;
//	}

//	private void triggerIfPossible(Events event) {
//		if (sm.canFire(event)) {
//			sm.fire(event);
//			lifecycleState = sm.getState();
//		}
//	}
	
	public boolean isInSuperEndState() {
		boolean inSuperState = sm.isInState(State.SUPERSTATE_ENDED);
		return inSuperState;
	}
	
	private void trigger(Events event) {				
		if (sm.canFire(event)) {
			nextEvent.setFromState(sm.getState());
			sm.fire(event);
			lifecycleState = sm.getState();
			if (lifecycleState != nextEvent.getFromState()) { // state transition
				nextEvent.setToState(lifecycleState);
				pub.publishEvent(nextEvent);
				nextEvent = new TaskStateTransitionEvent(lifecycleState, null, this);
			}
		} else {
			
			log.warn(String.format("Task %s received (and ignored) unexpected Event %s for State %s ", this.id,  event,  sm.getState()));			
		}		
	}
	
	public void setTaskType(TaskDefinition taskType) {
		this.taskType = taskType;
	}

	public Participant getResponsibleEngineer() {
		return responsibleEngineer;
	}

	public void setResponsibleEngineer(Participant responsibleEngineer) {
		this.responsibleEngineer = responsibleEngineer;
	}

	public List<ArtifactOutput> getOutput() {
		return Collections.unmodifiableList(output);
	}
	
	private boolean removeOutput(ArtifactOutput ao) {
		boolean result = output.remove(ao);
		os = calcOutputState();
		return result;
	}
	
	@Modifies( { "lifecycleState", "outputState" } )
	public void addOutput(ArtifactOutput ao) {
		ao.setContainer(this);
		output.add(ao);
		os = calcOutputState();
	}

	public List<ArtifactInput> getInput() {
		return Collections.unmodifiableList(input); 
	}
	
	private boolean removeInput(ArtifactInput ai) {
		boolean result = input.remove(ai);
		is = calcInputState();
		return result;
	}
	
	@Modifies( { "lifecycleState", "inputState" } )
	public void addInput(ArtifactInput ai) {
		ai.setContainer(this);
		input.add(ai);
		is = calcInputState();
	}

	public Artifact removeInputArtifactById(String artifactId) {
		Optional<ArtifactInput> opArt = getInput().stream()
				.filter(input -> input.getArtifact().getId().equals(artifactId))
				.findAny();
			if (opArt.isPresent()) {
				removeInput(opArt.get());
				return opArt.get().getArtifact();
			} else 
				return null;
	}
	
	public boolean hasInputArtifactWithId(String artifactId) {
		Optional<ArtifactInput> opArt = getInput().stream()
				.filter(input -> input.getArtifact().getId().equals(artifactId))
				.findAny();
			if (opArt.isPresent())
				return true;
			else 
				return false;		
	}
	
	public boolean hasOutputArtifactOfRole(String outputRole) {
		return (getAnyOneOutputByRole(outputRole) != null) ? true : false;			
	}
	
	public boolean hasOutputArtifactWithId(String artifactId) {
		Optional<ArtifactOutput> opArt = getOutput().stream()
				.filter(io -> io.getArtifact().getId().equals(artifactId))
				.findAny();
			if (opArt.isPresent())
				return true;
			else 
				return false;		
	}
	
	public int countOutputOfType(String artifactType) {		
		long count = 0;
		count = getOutput().stream()
			.filter(io -> io.getArtifact().getType().getArtifactType().equals(artifactType))
			.filter(io -> !(io.getArtifact().isRemovedAtOrigin()))
			.count();
		return (int)count;
	}
	
	public int countInputOfType(String artifactType) {		
		long count = 0;
		count = getInput().stream()
			.filter(io -> io.getArtifact().getType().getArtifactType().equals(artifactType))
			.filter(io -> !(io.getArtifact().isRemovedAtOrigin()))
			.count();	
		return (int)count;
	}
	
	public boolean hasInputArtifactOfRole(String inputRole) {
		return (getAnyOneInputByRole(inputRole) != null) ? true : false;			
	}
	

	
	// considers only artifacts that are not removedAtOrigin
	public Artifact getAnyOneInputByType(String artifactType) {
		Optional<ArtifactInput> opArt = getInput().stream()
			.filter(input -> input.getArtifact().getType().getArtifactType().equals(artifactType))
			.filter(io -> !io.getArtifact().isRemovedAtOrigin())
			.findAny();
		if (opArt.isPresent())
			return opArt.get().getArtifact();
		else 
			return null; 
	}
	
	// considers only artifacts that are not removedAtOrigin	
	public Artifact getAnyOneInputByRole(String inputRole) {
		Optional<ArtifactInput> opArt = getInput().stream()
				.filter(input -> input.getRole().equals(inputRole))
				.filter(io -> !io.getArtifact().isRemovedAtOrigin())
				.findAny();
			if (opArt.isPresent())
				return opArt.get().getArtifact();
			else 
				return null;
	}
	
	public Set<Map.Entry<String, ArtifactType>> getMissingInput() {
		return getTaskType().getMissingInput(this);
	}
	
	// considers only artifacts that are not removedAtOrigin
	public Artifact getAnyOneOutputByType(String artifactType) {
		Optional<ArtifactOutput> opArt = getOutput().stream()
			.filter(io -> io.getArtifact().getType().getArtifactType().equals(artifactType))
			.filter(io -> !io.getArtifact().isRemovedAtOrigin())
			.findAny();
		if (opArt.isPresent())
			return opArt.get().getArtifact();
		else 
			return null; 
	}
	
	// considers only artifacts that are not removedAtOrigin
	public Artifact getAnyOneOutputByRole(String outputRole) {
		Optional<ArtifactOutput> opArt = getOutput().stream()
				.filter(io -> io.getRole().equals(outputRole))
				.filter(io -> !io.getArtifact().isRemovedAtOrigin())
				.findAny();
			if (opArt.isPresent())
				return opArt.get().getArtifact();
			else 
				return null;
	}
	
	public Set<Map.Entry<String, ArtifactType>> getMissingOutput() {
		return getTaskType().getMissingOutput(this);
	}
	
	public OutputState getOutputState() {
		if (os == OutputState.OUTPUT_UNKNOWN)
			os = calcOutputState();
		return os;
	}
	
	public InputState getInputState() {
		if (is == InputState.INPUT_UNKNOWN)
			is = calcInputState();
		return is;
	}

	public InputState recalcInputState() {
		is = calcInputState();
		return getInputState();
	}
	
	public OutputState recalcOutputState() {
		os = calcOutputState();
		return getOutputState();
	}
	
	private InputState calcInputState() {		
		return taskType.calcInputState(this);		
	}
	
	private OutputState calcOutputState() {
		OutputState osPrior = os;
		OutputState osNow = taskType.calcOutputState(this);
		switch(osNow) {
		case OUTPUT_UNKNOWN: 
			break; // should not happen, as we calcOutputState
		case OUTPUT_MISSING:
			if (osPrior == OutputState.OUTPUT_SUFFICIENT) { // no longer sufficient
				trigger(Events.OUTPUT_REMOVED); // OUTPUT 
			}
			break;
		case OUTPUT_PARTIAL:
			if (osPrior == OutputState.OUTPUT_MISSING || osPrior == OutputState.OUTPUT_UNKNOWN) { // now some output is there
				trigger(Events.ADD_OUTPUT);
			} else if (osPrior == OutputState.OUTPUT_SUFFICIENT) { // no longer sufficient
				trigger(Events.OUTPUT_REMOVED); // OUTPUT 
			}
			break;
		case OUTPUT_SUFFICIENT:
			if (osPrior == OutputState.OUTPUT_MISSING || osPrior == OutputState.OUTPUT_PARTIAL || osPrior == OutputState.OUTPUT_UNKNOWN) { // now some output is there
				if (getInputState() != InputState.INPUT_SUFFICIENT) { // state is not active
					trigger(Events.ADD_OUTPUT);
				}
				trigger(Events.OUTPUTCONDITIONS_FULFILLED);
			}
			break;
		default:
			break; 
		}
		return osNow;
	}
	
	public void signalEvent(Events event) {
		trigger(event);
	}
	
	@Override
	public String toString() {
		return "WFTask (" + getLifecycleState() + "|"+getInputState()+"|"+getOutputState()+") [ id="+getId()+", tType=" + taskType + ", " + workflow.getId() + ", respEngineer="
				+ responsibleEngineer + ", output=" + output + ", input=" + input + "]";
	}

	@RelationshipEntity
	public static abstract class ArtifactIO implements java.io.Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		@Id
		String id;
//		@GeneratedValue
//		private Long id; //not used outside of OGM neo4j
		
		@EndNode
		Artifact artifact;
		@Property
		String role;
		@StartNode
		WorkflowTask container;
		
		protected void setContainer(WorkflowTask wt) {
			this.container = wt;
			//FIXME: brittle setting/overriding of id
			this.id = role+"#"+artifact.getId()+"#"+wt.getId();
		}
		
		public String getRole() {
			return role;
		}

		public void setRole(String outputRole) {
			this.role = outputRole;
		}

		public Artifact getArtifact() {
			return artifact;
		}

		public void setArtifact(Artifact artifact) {
			this.artifact = artifact;
		}		
		
		public ArtifactIO(Artifact artifact) {
			super();
			this.artifact = artifact;
		}

		@Deprecated
		public ArtifactIO() {}
		
		public ArtifactIO(Artifact artifact, String role) {
			super();
			this.id = role+"#"+artifact.getId();
			this.artifact = artifact;
			this.role = role;
		}
		

		@Override
		public String toString() {
			if (artifact.isRemovedAtOrigin())
				return "[DEL "+ artifact.getId() +"::"+ artifact.getType() + "]";
			else
				return "["+ artifact.getId() +"::"+ artifact.getType() + "]";
		}

	}
	
	@RelationshipEntity(type="TASK_IO")
	public static class ArtifactOutput extends ArtifactIO {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ArtifactOutput(Artifact artifact, String role) {
			super(artifact, role);
		}

		public ArtifactOutput(Artifact artifact) {
			super(artifact);
		}

		public ArtifactOutput(ArtifactInput ai) {
			id = ai.id;
			role = ai.role;
			container = ai.container;
			artifact = ai.artifact;
		}
		
		@Deprecated
		public ArtifactOutput() {
			super();
		}
	}
	
	@RelationshipEntity(type="TASK_IO")
	public static class ArtifactInput extends ArtifactIO {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		

		public ArtifactInput(Artifact artifact, String role) {
			super(artifact, role);
		}

		public ArtifactInput(Artifact artifact) {
			super(artifact);
		}

		public ArtifactInput(ArtifactOutput ao) {
			id = ao.id;
			role = ao.role;
			container = ao.container;
			artifact = ao.artifact;
		}
		
		@Deprecated
		public ArtifactInput(){
			super();
		}
	}
	
//	@RelationshipEntity
//	public static class ArtifactInput implements java.io.Serializable {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		@EndNode
//		Artifact artifact;
//		@Property
//		String inputRole;
//		@StartNode
//		WorkflowTask container;
//		
//		
//		public String getInputRole() {
//			return inputRole;
//		}
//
//		public void setInputRole(String inputRole) {
//			this.inputRole = inputRole;
//		}
//
//		public Artifact getArtifact() {
//			return artifact;
//		}
//
//		public void setArtifact(Artifact artifact) {
//			this.artifact = artifact;
//		}
//
//		public ArtifactInput(Artifact artifact) {
//			super();
//			this.artifact = artifact;
//		}		
//		
//		public ArtifactInput(Artifact artifact, String inputRole) {
//			super();
//			this.artifact = artifact;
//			this.inputRole = inputRole;
//		}
//		
//		protected void setContainer(WorkflowTask wt) {
//			this.container = wt;
//		}
//		
//
//		@Override
//		public String toString() {
//			if (artifact.isRemovedAtOrigin())
//				return "[ DEL!"+ artifact.getId() +"::"+ artifact.getType() + "]";
//			else
//				return "["+ artifact.getId() +"::"+ artifact.getType() + "]";
//		}
//	}
}
