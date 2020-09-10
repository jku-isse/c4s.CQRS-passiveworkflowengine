package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.IBranchInstance;
import impactassessment.passiveprocessengine.instance.WorkflowInstance;

public interface IBranchDefinition {

	String getName();

	TaskDefinition getTask();

	boolean hasActivationCondition();

	boolean hasDataFlow();

	IBranchInstance createInstance(WorkflowInstance wfi);

}