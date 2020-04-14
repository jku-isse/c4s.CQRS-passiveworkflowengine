package impactassessment.model.actor;

import impactassessment.model.workflowmodel.TaskDefinition;
import impactassessment.model.workflowmodel.WorkflowTask;

public interface ICustomRoleSelector {
	public Role getRoleForTaskState(WorkflowTask wt, TaskDefinition td);
}
