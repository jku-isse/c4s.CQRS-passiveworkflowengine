package impactassessment.passiveprocessengine.workflowmodel;

public interface IBranchDefinition {

	String getName();

	TaskDefinition getTask();

	boolean hasActivationCondition();

	boolean hasDataFlow();

	IBranchInstance createInstance(WorkflowInstance wfi);

}