package impactassessment.ltlcheck;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

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

	/** fields **/
	private static ConcurrentHashMap<String, HashMap<String, Stack<LTLProcessInstanceObject>>> wfProcessInstances = new ConcurrentHashMap<>();

	/**
	 * Builds a representation of the current workflow instance that allows testing
	 * for sufficient input and output states of the individual tasks.
	 *
	 * @return instance of {@link LTLProcessInstanceObject} used in building a
	 *         process log
	 * @param workflowID      The ID of the workflow the new
	 *                        {@link LTLProcessInstanceObject} belongs to.
	 * @param formulaName     The name of the formula which requires the new
	 *                        {@link LTLProcessInstanceObject} for evaluation.
	 * @param transitionEvent The transition event containing the desired
	 *                        information.
	 */
	public static LTLProcessInstanceObject extractWorkflowInformation(String workflowID, String formulaName,
			TaskStateTransitionEvent transitionEvent) {
		synchronized (wfProcessInstances) {
			WorkflowInstance wfi = transitionEvent.getTask().getWorkflow();
			WorkflowDefinition wfd = wfi.getType();
			ArrayList<TaskDefinition> taskDefinitions = (ArrayList<TaskDefinition>) wfd.getWorkflowTaskDefinitions();

			// new internal process instance object
			LTLProcessInstanceObject pi = new LTLProcessInstanceObject(wfd.getId());

			// attributes for the current process instance
			HashMap<String, String> piAttributes = new HashMap<>();

			Timestamp piTimestamp = new Timestamp(System.currentTimeMillis());
			SimpleDateFormat sdf = new SimpleDateFormat(LTLWorkflowConstants.PROCESS_LOG_TIMESTAMP_FORMAT);
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

				// obtain previous task state
				String prevTaskState = LTLWorkflowConstants.DEFAULT_PREV_TASK_STATE;
				if (wfProcessInstances.containsKey(workflowID)) {
					if (wfProcessInstances.get(workflowID).containsKey(formulaName)) {
						LTLProcessInstanceObject top = wfProcessInstances.get(workflowID).get(formulaName).peek();
						LTLTaskObject task = top.getAuditTrailEntries().stream()
								.filter(obj -> obj.getWorkflowModelElement().equals(td.getId())).findFirst().get();
						prevTaskState = task.getAttributes().get(LTLWorkflowConstants.ACTUAL_TASK_STATE);
					}
				}
				attributes.put(LTLWorkflowConstants.PREVIOUS_TASK_STATE, prevTaskState);

				LTLTaskObject currTask = new LTLTaskObject(taskId, eventType, originator, attributes);
				ates.add(currTask);
			}

			pi.setAuditTrailEntries(ates);

			appendWfProcessInstances(workflowID, formulaName, pi);
			return pi;
		}
	}

	/**
	 * Store the created process instance object for the associated workflow.
	 *
	 * @param workflowID  The ID of the workflow the new
	 *                    {@link LTLProcessInstanceObject} belongs to.
	 * @param formulaName The name of the formula which requires the new
	 *                    {@link LTLProcessInstanceObject} for evaluation.
	 * @param piObj       The {@link LTLProcessInstanceObject} to store.
	 *
	 */
	private static void appendWfProcessInstances(String workflowID, String formulaName,
			LTLProcessInstanceObject piObj) {
		if (!wfProcessInstances.containsKey(workflowID)) {
			HashMap<String, Stack<LTLProcessInstanceObject>> tempMap = new HashMap<>();
			Stack<LTLProcessInstanceObject> tempStack = new Stack<>();
			tempStack.push(piObj);
			tempMap.put(formulaName, tempStack);
			wfProcessInstances.put(workflowID, tempMap);
		} else {
			if (!wfProcessInstances.get(workflowID).containsKey(formulaName)) {
				Stack<LTLProcessInstanceObject> tempStack = new Stack<>();
				tempStack.push(piObj);
				wfProcessInstances.get(workflowID).put(formulaName, tempStack);
			} else {
				wfProcessInstances.get(workflowID).get(formulaName).push(piObj);
			}
		}
	}
}