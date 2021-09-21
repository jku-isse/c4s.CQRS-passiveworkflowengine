package impactassessment.ltlcheck;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import impactassessment.ltlcheck.util.LTLProcessInstanceObject;
import impactassessment.ltlcheck.util.LTLTaskObject;
import impactassessment.ltlcheck.util.LTLWorkflowConstants;
import passiveprocessengine.definition.TaskDefinition;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.TaskStateTransitionEvent;
import passiveprocessengine.instance.WorkflowInstance;

/**
 * This class is responsible for extracting all information required for
 * building a valid process log from a {@link TaskStateTransitionEvent}.
 *
 * @author chris
 */
public class WorkflowDataExtractor {

	/**
	 * Builds a representation of the current workflow instance that allows testing
	 * for sufficient input and output states of the individual tasks.
	 *
	 * @return instance of {@link LTLProcessInstanceObject} used in building a
	 *         process log
	 * @param transitionEvent The transition event containing the desired
	 *                        information.
	 */
	public static LTLProcessInstanceObject extractWorkflowInformation(TaskStateTransitionEvent transitionEvent) {
		WorkflowInstance wfi = transitionEvent.getTask().getWorkflow();
		WorkflowDefinition wfd = wfi.getType();
		ArrayList<TaskDefinition> taskDefinitions = (ArrayList<TaskDefinition>) wfd.getWorkflowTaskDefinitions();

		// new internal process instance object
		LTLProcessInstanceObject pi = new LTLProcessInstanceObject(wfd.getId());

		// attributes for the current process instance
		HashMap<String, String> piAttributes = new HashMap<>();

		Timestamp piTimestamp = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat(LTLWorkflowConstants.CREATION_TIMESTAMP_FORMAT);
		String formattedTimestamp = sdf.format(piTimestamp);
		piAttributes.put(LTLWorkflowConstants.CREATION_TIMESTAMP_IDENTIFIER, formattedTimestamp);

		// if any workflow properties are present, store them as process instance
		// attributes
		if (wfi.getPropertiesReadOnly().size() != 0) {
			for (Entry<String, String> entry : wfi.getPropertiesReadOnly()) {
				piAttributes.put(entry.getKey(), entry.getValue());
			}
		}

		pi.setAttributes(piAttributes);

		// audit trail entries for the current process instance
		List<LTLTaskObject> ates = new ArrayList<>();

		for (TaskDefinition td : taskDefinitions) {
			String taskId = td.getId();
			String eventType = LTLWorkflowConstants.DEFAULT_EVENT_TYPE;
			String originator = wfd.getId();

			// collect all required attributes for every AuditTrailEntry
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put(LTLWorkflowConstants.INPUT_IDENTIFIER, td.calcInputState(wfi).toString());
			attributes.put(LTLWorkflowConstants.OUTPUT_IDENTIFIER, td.calcOutputState(wfi).toString());
			attributes.put(LTLWorkflowConstants.ACTUAL_TASK_STATE,
					transitionEvent.getTask().getActualLifecycleState().toString());
			attributes.put(LTLWorkflowConstants.EXPECTED_TASK_STATE,
					transitionEvent.getTask().getExpectedLifecycleState().toString());

			LTLTaskObject currTask = new LTLTaskObject(taskId, eventType, originator, attributes);
			ates.add(currTask);
		}

		pi.setAuditTrailEntries(ates);

		return pi;
	}
}