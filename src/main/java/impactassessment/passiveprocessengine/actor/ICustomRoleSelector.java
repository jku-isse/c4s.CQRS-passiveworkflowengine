package impactassessment.passiveprocessengine.actor;

import impactassessment.passiveprocessengine.workflowmodel.TaskDefinition;
import impactassessment.passiveprocessengine.workflowmodel.WorkflowTask;

public interface ICustomRoleSelector {
	public Role getRoleForTaskState(WorkflowTask wt, TaskDefinition td);
}
