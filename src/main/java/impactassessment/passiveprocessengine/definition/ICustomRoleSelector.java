package impactassessment.passiveprocessengine.definition;

import impactassessment.passiveprocessengine.instance.WorkflowTask;

public interface ICustomRoleSelector {
	public Role getRoleForTaskState(WorkflowTask wt, TaskDefinition td);
}
