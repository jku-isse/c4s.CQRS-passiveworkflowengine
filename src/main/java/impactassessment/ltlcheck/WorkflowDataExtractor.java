package impactassessment.ltlcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import impactassessment.ltlcheck.util.LTLTaskObject;
import impactassessment.ltlcheck.util.LTLWorkflowConstants;
import passiveprocessengine.definition.TaskDefinition;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.WorkflowInstance;

/**
 * @author chris
 */
public class WorkflowDataExtractor {

	/**
	 * Builds a representation of the current workflow instance that allows testing
	 * for sufficient input and output states of the individual tasks.
	 *
	 * @return instance of {@link LTLProcessInstanceObject} used in building a
	 *         process log
	 * @param wfi The workflow instance containing the desired information.
	 */
	public static LTLProcessInstanceObject extractBasicWorkflowInformation(WorkflowInstance wfi) {
		WorkflowDefinition wfd = wfi.getType();
		ArrayList<TaskDefinition> taskDefinitions = (ArrayList<TaskDefinition>) wfd.getWorkflowTaskDefinitions();

		// new internal process instance object
		LTLProcessInstanceObject pi = new LTLProcessInstanceObject(wfd.getId());

		// audit trail entries for the current process instance
		List<LTLTaskObject> ates = new ArrayList<>();

		for (TaskDefinition td : taskDefinitions) {
			String taskId = td.getId();
			String eventType = LTLWorkflowConstants.DEFAULT_EVENT_TYPE;
			String originator = wfd.getId();

			HashMap<String, String> attributes = new HashMap<>();
			attributes.put(LTLWorkflowConstants.INPUT_IDENTIFIER, td.calcInputState(wfi).toString());
			attributes.put(LTLWorkflowConstants.OUTPUT_IDENTIFIER, td.calcOutputState(wfi).toString());

			LTLTaskObject currTask = new LTLTaskObject(taskId, eventType, originator, attributes);
			ates.add(currTask);
		}

		pi.setAuditTrailEntries(ates);

		return pi;
	}
}