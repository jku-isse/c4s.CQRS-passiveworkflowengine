package impactassessment.model.workflowmodel;

public interface IBranchDefinition {

	String getName();

	TaskDefinition getTask();

	boolean hasActivationCondition();

	boolean hasDataFlow();

	IBranchInstance createInstance(WorkflowInstance wfi);

}