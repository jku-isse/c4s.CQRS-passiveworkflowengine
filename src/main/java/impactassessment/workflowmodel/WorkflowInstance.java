package impactassessment.workflowmodel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@NodeEntity
public class WorkflowInstance extends AbstractWorkflowInstanceObject implements java.io.Serializable{

    /**
     *
     */
    private static final long serialVersionUID = -5679379946036530198L;
    private transient static Logger log = LogManager.getLogger(WorkflowInstance.class);


    @Relationship(type="SPECIFIED_BY")
    private WorkflowDefinition workflowDefinition;

    @Relationship(type="DECISIONNODE_INSTANCES")
    private Set<DecisionNodeInstance> dnInst = new HashSet<DecisionNodeInstance>();
    @Relationship(type="TASK_INSTANCES")
    private Set<WorkflowTask> taskInst = new HashSet<WorkflowTask>();
    //private Set<DecisionNodeDefinition> dnDefs = new HashSet<DecisionNodeDefinition>(); // available via workflowdefinition

    @Properties
    private Map<String,String> wfProps = new HashMap<String,String>();

    private transient Map<WorkflowTask, DecisionNodeInstance> taskIntoDNI = new HashMap<WorkflowTask, DecisionNodeInstance>();
    private transient Map<WorkflowTask, DecisionNodeInstance> taskOutOfDNI = new HashMap<WorkflowTask, DecisionNodeInstance>();

    private transient TaskStateTransitionEventPublisher pub;

    @Deprecated
    public WorkflowInstance() {
        super();
    }

    public WorkflowInstance(String workflowId, WorkflowDefinition workflowDefinition, TaskStateTransitionEventPublisher pub) {
        super(workflowId, null);
        this.pub = pub;
        this.workflowDefinition = workflowDefinition;
    }

    public void initAfterPersistance(TaskStateTransitionEventPublisher pub, WorkflowDefinition wfd) {
        this.pub = pub;
        this.workflowDefinition = wfd;
        // // workflow instance uplink and task dnd mappings
        taskInst.stream().forEach(t -> {
            t.setWorkflow(this);
            taskIntoDNI.put(t, null);
            taskOutOfDNI.put(t, null);}
        );
        dnInst.stream().forEach(dni -> {
            dni.setWorkflow(this);
            dni.getInBranches().stream().forEach(b -> {
                if (b.hasTask()) {
                    taskIntoDNI.put(b.getTask(), dni);
                }
            });
            dni.getOutBranches().stream().forEach(b -> {
                if (b.hasTask()) {
                    taskOutOfDNI.put(b.getTask(), dni);
                }
            });
        });

    }

    @Override
    public String toString() {
        return "[WorkflowInstance: " + id + "]";
    }

    public WorkflowTask prepareTask(TaskDefinition td) {
        //WorkflowTask tf = new WorkflowTask(td.getId()+"#"+UUID.randomUUID().toString(), this, TaskLifecycle.buildStatemachine(), pub);
        WorkflowTask tf = new WorkflowTask(td.getId()+"#"+getId().toString(), this, TaskLifecycle.buildStatemachine(), pub);
        tf.setTaskType(td);
        return tf;
    }

    public WorkflowTask instantiateTask(TaskDefinition td) {
        WorkflowTask tf = prepareTask(td);
        taskInst.add(tf);
        taskIntoDNI.put(tf, null);
        taskOutOfDNI.put(tf, null);
        return tf;
    }



    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }

    public Set<WorkflowTask> getWorkflowTasksReadonly() {
        return Collections.unmodifiableSet(this.taskInst);
    }

    public Set<DecisionNodeInstance> getDecisionNodeInstancesReadonly() {
        return Collections.unmodifiableSet(this.dnInst);
    }
//	public void setWorkflowDefinitionId(WorkflowDefinition workflowDefinition) {
//		this.workflowDefinition = workflowDefinition;
//	}

    public List<AbstractWorkflowInstanceObject> enableWorkflowTasksAndDecisionNodes() {
        // enable tasks only via DNIs
        // check which dnd to enable: for now those without in-branch
        return this.workflowDefinition.getDecisionNodeDefinitions().stream()
                .filter(dnd -> dnd.getInBranches().size() == 0)
                .map(dnd -> dnd.createInstance(this))
                .flatMap(dni -> { registerDecisionNodeInstance(dni);
                    List<AbstractWorkflowInstanceObject> awos = new ArrayList<AbstractWorkflowInstanceObject>();
                    awos.add(dni);
                    awos.addAll(activateTasksFromDecisionNode(dni));
                    return awos.stream();
                })
                .collect(Collectors.toList());
    }

    private Set<AbstractWorkflowInstanceObject> activateTasksFromDecisionNode(DecisionNodeInstance dni) {
        return dni.calculatePossibleActivationPropagation().entrySet().stream()
                .filter(e -> !e.getKey().getBranchDefinition().hasDataFlow())
                .flatMap(e -> { taskInst.add(e.getValue()); // add to workflow managed elements
                    e.getKey().setTask(e.getValue()); // set to branch
                    registerTaskAsOutOfDNI(dni, e.getValue());
                    // TODO: set responsible engineer
                    // TODO: propagate beyond this initial task(s) : other Decision Nodes
                    List<AbstractWorkflowInstanceObject> awos = new ArrayList<AbstractWorkflowInstanceObject>();
                    awos.add(e.getValue());
                    awos.addAll(activateDecisionNodesFromTask(e.getValue()));
                    return awos.stream();
                })
                .collect(Collectors.toSet());
    }

    public Set<AbstractWorkflowInstanceObject> activateDecisionNodesFromTask(WorkflowTask wft) {
        // AIMING TO ACTIVATE DECISION NODES DOWNSTREAM: i.e., which decision nodes now become active as this task matches some of their inbranches
        Set<AbstractWorkflowInstanceObject> awos = new HashSet<AbstractWorkflowInstanceObject>();
        // only for WFT that have no outbranch connection
        if (this.taskIntoDNI.get(wft) != null) {
            return awos;
        }
        // find any dni or dnd with an accepting in branch
        Set<DecisionNodeInstance> dnis = dnInst.stream()
                .map(d -> d.consumeTaskForUnconnectedInBranch(wft)) // return the decision nodes that have a branch accepted the artifact
                .filter(dni -> dni != null)  // filter out null values
                .distinct() // unique decision nodes (should not happen anyway as each dni should only have one single inBranch per Tasktype
                .map(dni -> consumePrematureTasksForUnconnectedOutBranch(dni))
                //.map(dni -> this.registerTaskAsInToDNI(dni, wft))
                .collect(Collectors.toSet());
        if (dnis.size() > 1) {
            log.error("Activated more than one decision node instance from DNIs with task: "+ wft);
        }
        // a single DND should only be have a singel DNI per process (no loops supported yet)
        // to make sure, now we check which DND we could instantiate and take only these, that have no DNIs already active
        awos.addAll(dnis);
        Set<DecisionNodeInstance> dnis2 = this.workflowDefinition.getDecisionNodeDefinitions().stream()
                .filter(dnd -> dnd.acceptsWorkflowTaskForInBranches(wft))
                .filter(dnd -> { return !dnis.stream()
                        .filter(dni -> dni.getDefinition().getId() == dnd.getId()) // TODO: FIXME: filter dni of same type, needed when we have parallel in, otherwise we would generate another instance of this DNI whenever there second task becomes created
                        .findAny()
                        .isPresent();
                })
                .map(dnd -> { DecisionNodeInstance dni = dnd.createInstance(this); // we would need to check for any task that already exists (prematurely executed)
                    registerDecisionNodeInstance(dni);
                    return dni;
                })
                .map(dni -> consumePrematureTasksForUnconnectedOutBranch(dni))
                .map(dni -> dni.consumeTaskForUnconnectedInBranch(wft))
                .filter(dni -> dni != null)
                //.map(dni -> this.registerTaskAsInToDNI(dni, wft))
                .collect(Collectors.toSet());
        if (dnis2.size() > 1) {
            log.error("Activated more than one decision node instance from DNDs with task: "+ wft);
        }
        awos.addAll(dnis2);
        // In any case, not just if empty!!! ///////if awos is empty, it means this task didn;t activate any decision nodes, thus check for output
        //if (awos.isEmpty()) {
        Optional<DecisionNodeInstance> optDNI = consumePrematureTaskForUnconnectedOutBranch(wft);
        optDNI.orElseGet( () -> { if (awos.isEmpty())
        { log.warn("Premature task not usable for any DNI-branch: "+ wft); }
            return null;} );
        optDNI.ifPresent( dni -> awos.add(dni) );
        //}
        return awos;
    }

    public void registerDecisionNodeInstance(DecisionNodeInstance dni) {
        dnInst.add(dni);
    }

    protected void registerTaskAsInToDNI(DecisionNodeInstance dni, WorkflowTask wt) {
        taskIntoDNI.put(wt, dni);
        //return dni;
    }

    protected void registerTaskAsOutOfDNI(DecisionNodeInstance dni, WorkflowTask wt) {
        taskOutOfDNI.put(wt, dni);
        //return dni;
    }

    private Optional<DecisionNodeInstance> consumePrematureTaskForUnconnectedOutBranch(WorkflowTask wt) {
        return dnInst.stream()
                .map(d -> d.consumeTaskForUnconnectedOutBranch(wt)) // return the decision nodes that have a branch accepted the artifact
                .filter(dni -> dni != null)  // filter out null values
                .distinct() // unique decision nodes (should not happen anyway as each dni should only have one single inBranch per Tasktype
                .findAny();
    }

    private DecisionNodeInstance consumePrematureTasksForUnconnectedOutBranch(DecisionNodeInstance dni) {
        // determine which tasks can be used for this DecisionNode instance, and map accordingly
        taskOutOfDNI.entrySet().stream()
                .filter( tuple -> tuple.getValue() == null) // only tasks that are not yet assigned to an outbranch
                .forEach(tuple -> { if (dni.consumeTaskForUnconnectedOutBranch(tuple.getKey()) != null) {
                    tuple.setValue(dni); // basically registeringTaskAsOutOfDNI
                }
                });
//			.map( tuple -> tuple.getKey())
//			.map(t -> dni.consumeTaskForUnconnectedOutBranch(t) == null ? null : t) // doing this as a filter would be ugly as not sideeffect free
//			.collect(Collectors.toSet());
        return dni;
    }

    protected WorkflowTask getNewPlaceHolderTask(TaskDefinition td) {
        WorkflowTask placeHolder = new PlaceHolderTask(this, td, "PLACEHOLDER#"+UUID.randomUUID().toString());
        return placeHolder;
    }

    public String addOrReplaceProperty(String key, String value) {
        return wfProps.put(key, value);
    }

    public String getEntry(String key) {
        return wfProps.get(key);
    }

    public Set<Entry<String,String>> getPropertiesReadOnly() {
        return Collections.unmodifiableSet(wfProps.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowInstance)) return false;
        WorkflowInstance that = (WorkflowInstance) o;
        return Objects.equals(workflowDefinition, that.workflowDefinition) &&
                Objects.equals(dnInst, that.dnInst) &&
                Objects.equals(taskInst, that.taskInst) &&
                Objects.equals(wfProps, that.wfProps) &&
                Objects.equals(taskIntoDNI, that.taskIntoDNI) &&
                Objects.equals(taskOutOfDNI, that.taskOutOfDNI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowDefinition, dnInst, taskInst, wfProps, taskIntoDNI, taskOutOfDNI);
    }

    // METHOD BELOW NEED CHECKING WHETHER NECESSARY

//	public void signalNewWorkflowTask(WorkflowTask wti) {
//		// use this to activate decision nodes
//		// check amongst decisionNodeinstances whether matches existing dni
//		// TODO: check if we would have to check outBranches as well?!!
//		Optional<DecisionNodeInstance> dni = dnInst.stream()
//			.filter(d -> d.acceptsTaskForUnconnectedInBranch(wti))
//			.findFirst();
//		dni.ifPresent(d -> d.consumeTaskForUnconnectedInBranch(wti));
//		// returns affected DecisionNodeINstance
//		dni.orElseGet(() -> { return getNewDecisionNodeInstance(wti); });
//		// or whether to create a new dni from a nodedefinition
//	}
//
//	private DecisionNodeInstance getNewDecisionNodeInstance(WorkflowTask wti) {
//		DecisionNodeInstance dni = null;
//		Optional<DecisionNodeDefinition> dndOp = workflowDefinition.getDecisionNodeDefinitions().stream()
//			.filter(dn -> dn.acceptsWorkflowTask(wti))
//			.findFirst();
//		if (dndOp.isPresent()) {
//			dni = dndOp.get().createInstance(this);
//			dni.consumeTaskForUnconnectedInBranch(wti);
//			registerDecisionNodeInstance(dni);
//		}
//		return dni;
//	}


}
