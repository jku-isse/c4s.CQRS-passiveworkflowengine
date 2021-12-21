package impactassessment.passiveprocessengine;

import passiveprocessengine.definition.IWorkflowTask;
import passiveprocessengine.instance.WorkflowChangeEvent;

public class RootWorkflowChangeEvent extends WorkflowChangeEvent{

	
	
	public RootWorkflowChangeEvent(String refId, IWorkflowTask wft) {
		super(ChangeType.EMPTY, wft, null);
		super.id = refId;
	}
	
}
