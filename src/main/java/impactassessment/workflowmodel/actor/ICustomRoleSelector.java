package impactassessment.workflowmodel.actor;

import impactassessment.workflowmodel.TaskDefinition;
import impactassessment.workflowmodel.WorkflowTask;

public interface ICustomRoleSelector {
	public Role getRoleForTaskState(WorkflowTask wt, TaskDefinition td);
}
