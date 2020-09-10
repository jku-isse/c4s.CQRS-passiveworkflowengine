package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.IBranchInstance;
import impactassessment.passiveprocessengine.instance.WorkflowInstance;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity
public abstract class AbstractBranchDefinition implements IBranchDefinition {
	@Id
	private String name;
	@EndNode
	private TaskDefinition task;
	@StartNode
	private DecisionNodeDefinition dnd;
	
	// when used as outbranch: no activation condition means, branch is fullfilled once the DNI context is fullfilled, we can activate task at end
	// when used as inbranch: no condition on output of branch: as soon as task is set to output complete, branch is fulfilled
	@Property
	private boolean hasActivationCondition = false;
		
	// when used as outbranch: task at end receives no input set from the rule part, optionally task input data is set out of bounds
	// when used as inbranch: task at end produces not output that is relevant for the process, optionally output data is propagated out of bounds
	@Property
	private boolean hasDataFlow = false;
	
	@Deprecated
	public AbstractBranchDefinition() {
		
	}
	
	public AbstractBranchDefinition(String name, TaskDefinition task, DecisionNodeDefinition dnd) {
		this.name = name;
		this.task = task;
		this.dnd = dnd;
	}
	
	public AbstractBranchDefinition(String name, TaskDefinition task, DecisionNodeDefinition dnd, boolean hasActivationCondition, boolean hasDataFlow) {
		this(name, task, dnd);
		this.hasActivationCondition = hasActivationCondition;
		this.hasDataFlow = hasDataFlow;
	}


	/* (non-Javadoc)
	 * @see c4s.impactassessment.workflowmodel.workflowmodel.IBranchDefinition#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see c4s.impactassessment.workflowmodel.workflowmodel.IBranchDefinition#getTask()
	 */
	@Override
	public TaskDefinition getTask() {
		return task;
	}
	
	/* (non-Javadoc)
	 * @see c4s.impactassessment.workflowmodel.workflowmodel.IBranchDefinition#hasActivationCondition()
	 */
	@Override
	public boolean hasActivationCondition() {
		return hasActivationCondition;
	}

	/* (non-Javadoc)
	 * @see c4s.impactassessment.workflowmodel.workflowmodel.IBranchDefinition#hasDataFlow()
	 */
	@Override
	public boolean hasDataFlow() {
		return hasDataFlow;
	}
	
	
	
	@Override
	public String toString() {
		return "BranchDefinition [name=" + name + ", task=" + task.getId() + ", hasActivationCondition="
				+ hasActivationCondition + ", hasDataFlow=" + hasDataFlow + "]";
	}

	/* (non-Javadoc)
	 * @see c4s.impactassessment.workflowmodel.workflowmodel.IBranchDefinition#createInstance()
	 */
	@Override
	public abstract IBranchInstance createInstance(WorkflowInstance wfi);

}