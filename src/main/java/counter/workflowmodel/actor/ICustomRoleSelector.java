package counter.workflowmodel.actor;

import counter.workflowmodel.TaskDefinition;
import counter.workflowmodel.WorkflowTask;

public interface ICustomRoleSelector {
	public Role getRoleForTaskState(WorkflowTask wt, TaskDefinition td);
}
