package impactassessment.workflowmodel;

import org.neo4j.ogm.annotation.RelationshipEntity;


@RelationshipEntity(type="BRANCH")
public class DefaultBranchDefinition extends AbstractBranchDefinition {
	
	@Deprecated
	public DefaultBranchDefinition() {
		super();
	}
	
	public DefaultBranchDefinition(String name, TaskDefinition task, boolean hasActivationCondition, boolean hasDataFlow, DecisionNodeDefinition dnd) {
		super(name, task, dnd, hasActivationCondition, hasDataFlow);
	}

	@Override
	public IBranchInstance createInstance(WorkflowInstance wfi) {
		IBranchInstance abi = new DefaultBranchInstance(this, wfi);
		if (!hasActivationCondition()) {
			abi.setConditionsFulfilled();
		}
		return abi;
	}
	
}
