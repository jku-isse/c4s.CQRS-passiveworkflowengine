package impactassessment.ltlcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.instance.TaskStateTransitionEvent;

/**
 * @author chris
 */
@Slf4j
public class LTLValidationManager {

	/** {@link LTLValidationManager} instance **/
	private static LTLValidationManager instance = null;

	/** This scheduler allows validations to be conducted periodically. **/
	private ScheduledExecutorService validationScheduler;

	/**
	 * If a periodic task is to be executed, the most recent
	 * {@link TaskStateTransitionEvent} for the workflow identified by the key of
	 * this map must be used for the evaluation.
	 **/
	private ConcurrentHashMap<String, Stack<TaskStateTransitionEvent>> periodicTaskMap;

	/** max. allowed amount of periodic evaluation tasks **/
	private static final int MAX_TASK_COUNT = 10;

	/**
	 * This collection will contain one entry per workflow the
	 * {@link LTLValidationManager} is called for. An entry is identified by a
	 * string-value (i.e. a workflow-ID, etc.) which is paired with a HashMap. This
	 * HashMap contains entries which map a formula-name to a Stack of type
	 * ArrayList<ValidationResult> that in turn comprises lists with validation
	 * results (one list per validation invocation) associated with one single
	 * formula. This structure allows to store multiple validation results per
	 * formula for an arbitrary amounts of formulas validated for a workflow. As we
	 * use a stack, backwards comparison is done from top to bottom.
	 **/
	private ConcurrentHashMap<String, HashMap<String, Stack<ArrayList<ValidationResult>>>> checkResults;

	private LTLValidationManager() {
		checkResults = new ConcurrentHashMap<>();
		validationScheduler = Executors.newScheduledThreadPool(MAX_TASK_COUNT);
	}

	/**
	 * @return instance of {@link LTLValidationManager}
	 */
	public static LTLValidationManager getInstance() {
		if (instance == null) {
			instance = new LTLValidationManager();
		}
		return instance;
	}

	/**
	 * Register a validation task for periodic execution.
	 *
	 * @param workflowID     The ID of the workflow to be validated.
	 * @param af             The formula to be validated against the wfInstance.
	 * @param detailedOutput Decides if the evaluation output for the most recent
	 *                       validation result should also include detailed
	 *                       information (e.g. what audit trail entries (tasks)
	 *                       passed and which did not).
	 * @param initialDelay   The delay before the task is executed for the first
	 *                       time.
	 * @param delay          The delay between the periodic executions of the task.
	 */
	public void registerValidationTask(String workflowID, AvailableFormulas af, boolean detailedOutput,
			long initialDelay, long delay, TimeUnit timeUnit) {

		// register the validation task
		validationScheduler.scheduleAtFixedRate(new ValidationTask(workflowID, af, detailedOutput), initialDelay, delay,
				timeUnit);
	}

	/**
	 * Update the task stack with the most recent state of the workflow to be
	 * checked periodically.
	 *
	 * @param workflowID      The identifier of the passed workflow posing as key to
	 *                        find the correct workflow instance stack.
	 * @param transitionEvent Transition event (containing the workflow instance) to
	 *                        be pushed to the stack mapped to the workflow
	 *                        identifier.
	 */
	public void updateTaskStack(String workflowID, TaskStateTransitionEvent transitionEvent) {
		if (!periodicTaskMap.containsKey(workflowID)) {
			Stack<TaskStateTransitionEvent> tempStack = new Stack<>();
			tempStack.push(transitionEvent);
			periodicTaskMap.put(workflowID, tempStack);
		} else {
			periodicTaskMap.get(workflowID).push(transitionEvent);
		}
	}

	/**
	 * Check if the process log (processLogObj) of workflow with workflowID
	 * satisfies the LTL formula(s) denoted by ltlFormulaDefinitionKey.
	 *
	 * @param workflowID              The ID of the workflow the validation
	 *                                procedure is to be invoked for.
	 * @param transitionEvent         TaskStateTransitionEvent object from which all
	 *                                information necessary to derive a valid
	 *                                process log is extracted from.
	 * @param ltlFormulaDefinitionKey Enum-value identifying which of the formulas
	 *                                that have been defined in
	 *                                {@link LTLFormulaProvider} should be validated
	 *                                against a process log (this only allows the
	 *                                use of static formulas that are valid for
	 *                                every workflow and can thus be defined at
	 *                                design time).
	 */
	private void checkTraceAndStoreResult(String workflowID, TaskStateTransitionEvent transitionEvent,
			AvailableFormulas ltlFormulaDefinitionKey) {
		if (!checkResults.containsKey(workflowID)) {
			HashMap<String, Stack<ArrayList<ValidationResult>>> workflowMap = new HashMap<>();
			Stack<ArrayList<ValidationResult>> resultStack = new Stack<ArrayList<ValidationResult>>();
			resultStack.push(RuntimeParser.checkLTLTrace(ltlFormulaDefinitionKey, transitionEvent));
			workflowMap.put(ltlFormulaDefinitionKey.toString(), resultStack);
			checkResults.put(workflowID, workflowMap);
		} else {
			if (!checkResults.get(workflowID).containsKey(ltlFormulaDefinitionKey.toString())) {
				Stack<ArrayList<ValidationResult>> resultStack = new Stack<ArrayList<ValidationResult>>();
				resultStack.push(RuntimeParser.checkLTLTrace(ltlFormulaDefinitionKey, transitionEvent));
				checkResults.get(workflowID).put(ltlFormulaDefinitionKey.toString(), resultStack);
			} else {
				checkResults.get(workflowID).get(ltlFormulaDefinitionKey.toString())
						.push(RuntimeParser.checkLTLTrace(ltlFormulaDefinitionKey, transitionEvent));
			}
		}
	}

	/**
	 * Evaluate if the most recent execution of the validation algorithm for a
	 * certain formula resulted in the same set of failed and successful process
	 * instances or if anything changed for the better or worst.
	 *
	 * @param workflowID           The ID of the workflow whose most recent two
	 *                             validation results for a certain formula should
	 *                             be compared with one another.
	 * @param formulaDefinitionKey The identifier of the formula associated with the
	 *                             validation results to be compared.
	 * @param detailedOutput       Decides if the evaluation output for the most
	 *                             recent validation result should also include
	 *                             detailed information (e.g. what audit trail
	 *                             entries (tasks) passed and which did not).
	 */
	private void evaluateResults(String workflowID, AvailableFormulas formulaDefinitionKey, boolean detailedOutput) {
		Stack<ArrayList<ValidationResult>> tempStack = checkResults.get(workflowID)
				.get(formulaDefinitionKey.toString());

		if (tempStack == null) {
			log.debug("No trace validations of formula " + formulaDefinitionKey.toString()
					+ " have been conducted for workflow " + workflowID + " yet.");
			return;
		} else if (tempStack.size() < 2) {
			log.debug("Only one validation of formula " + formulaDefinitionKey.toString()
					+ " has been conducted for workflow '" + workflowID + "' yet. No result comparison possible.");
			return;
		}

		ArrayList<ValidationResult> topResult = tempStack.elementAt(0);
		ArrayList<ValidationResult> olderResult = tempStack.elementAt(1);

		if (topResult != null && olderResult != null) {
			Integer aggregateTop = topResult.stream().mapToInt(x -> x.getNrOfGoodResults()).sum();
			Integer aggregateTopBadResults = topResult.stream().mapToInt(x -> x.getNrOfBadResults()).sum();
			Integer aggregateOld = olderResult.stream().mapToInt(x -> x.getNrOfGoodResults()).sum();

			int cmp = aggregateTop.compareTo(aggregateOld);
			if (cmp > 0) {
				if (aggregateTopBadResults == 0) {
					log.debug("No trace violations detected in the most recent validation.");
				}
			} else if (cmp < 0) {
				log.debug("The most recent validation yields more violations than the previous.");
			} else {
				log.debug("The most recent validation yields the same result as the previous.");
			}

			if (detailedOutput) {
				log.debug("Tasks fulfilling formula " + formulaDefinitionKey + ":");
				for (ValidationResult r : topResult) {
					for (Entry<String, HashMap<String, Boolean>> resultEntry : r.getAuditTrailEntryResults()
							.entrySet()) {
						log.debug("Process instance: " + resultEntry.getKey());
						for (Entry<String, Boolean> piEntry : resultEntry.getValue().entrySet()) {
							log.debug(piEntry.getKey() + " passed: " + piEntry.getValue());
						}
					}
				}
			}
		}
	}

	/**
	 * Clear all present validation results for the workflow identified by
	 * workflowID.
	 *
	 * @param workflowID The ID of the workflow whose validation results should be
	 *                   cleared.
	 * @return true if results have been cleared, false otherwise (if no results are
	 *         present yet)
	 */
	public boolean clearResults(String workflowID) {
		synchronized (checkResults) {
			if (checkResults.containsKey(workflowID)) {
				checkResults.get(workflowID).clear();
				return true;
			}
			return false;
		}
	}

	/**
	 * Reset the validation environment (e.g. call the garbage collector to collect
	 * class instance, validation results, etc.).
	 */
	public void resetValidationEnvironment() {
		instance = null;
		System.gc();
	}

	/**
	 * Call the validation routine (parser, validator, etc.) and evaluate the
	 * received results.
	 *
	 * @param workflowID      The ID of the workflow to be validated.
	 * @param transitionEvent The object holding the information necessary for the
	 *                        process log (e.g. XML) conversion process.
	 * @param af              The formula to be validated against the wfInstance.
	 * @param detailedOutput  Decides if the evaluation output for the most recent
	 *                        validation result should also include detailed
	 *                        information (e.g. what audit trail entries (tasks)
	 *                        passed and which did not).
	 */
	public void validate(String workflowID, TaskStateTransitionEvent transitionEvent, AvailableFormulas af,
			boolean detailedOutput) {
		checkTraceAndStoreResult(workflowID, transitionEvent, af);
		evaluateResults(workflowID, af, detailedOutput);
	}

	/**
	 * Runnable used for periodic validation tasks.
	 */
	private class ValidationTask implements Runnable {

		private String workflowID;
		private AvailableFormulas af;
		private boolean detailedOutput;

		public ValidationTask(String workflowID, AvailableFormulas af, boolean detailedOutput) {
			this.workflowID = workflowID;
			this.af = af;
			this.detailedOutput = detailedOutput;
		}

		@Override
		public void run() {
			// retrieve most recent workflow instance
			TaskStateTransitionEvent transitionEvent = periodicTaskMap.get(workflowID).firstElement();

			// reset the stack
			periodicTaskMap.get(workflowID).clear();

			// call the validation routine
			checkTraceAndStoreResult(workflowID, transitionEvent, af);
			evaluateResults(workflowID, af, detailedOutput);
		}
	}
}
