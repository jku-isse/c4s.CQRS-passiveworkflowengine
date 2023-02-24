package impactassessment.ltlcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import impactassessment.ltlcheck.LTLFormulaProvider.AvailableFormulas;
import impactassessment.ltlcheck.util.LTLFormulaConstants;
import impactassessment.ltlcheck.util.LTLFormulaObject;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationMode;
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
	 * This map associates a unique workflow ID with a map of formula names each
	 * paired with a stack of {@link TaskStateTransitionEvent} objects. This mapping
	 * ensures that multiple different periodic validation tasks (e.g. multiple
	 * different formula validation tasks) can be scheduled for a single workflow.
	 * If a periodic task for a certain workflow is to be executed, the most recent
	 * {@link TaskStateTransitionEvent} (e.g. the top element of the stack) mapped
	 * to a certain formula name must be used for the evaluation.
	 **/
	private ConcurrentHashMap<String, HashMap<String, Stack<TaskStateTransitionEvent>>> periodicTaskMap;

	/**
	 * max. number of threads to keep in the thread pool for parallel task
	 * executions
	 **/
	private static final int MAX_THREAD_COUNT = 10;

	/**
	 * This collection will contain one entry per workflow the
	 * {@link LTLValidationManager} is called for. An entry is identified by a
	 * string-value (i.e. a unique workflow-ID, etc.) which is paired with a
	 * HashMap. This HashMap contains entries which map a formula-name to a Stack of
	 * type ArrayList<ValidationResult> that in turn comprises lists with validation
	 * results (one list per validation invocation) associated with one single
	 * formula. This structure allows to store multiple validation results per
	 * formula for an arbitrary amounts of formulas validated for a workflow. As we
	 * use a stack, backwards comparison is done from top to bottom.
	 **/
	private ConcurrentHashMap<String, HashMap<String, Stack<ArrayList<ValidationResult>>>> checkResults;

	/**
	 * Default constructor.
	 */
	private LTLValidationManager() {
		checkResults = new ConcurrentHashMap<>();
		periodicTaskMap = new ConcurrentHashMap<>();
		validationScheduler = Executors.newScheduledThreadPool(MAX_THREAD_COUNT);
	}

	/**
	 * @return instance of {@link LTLValidationManager}
	 */
	public static synchronized LTLValidationManager getInstance() {
		if (instance == null) {
			instance = new LTLValidationManager();
		}
		return instance;
	}

	/**
	 * Register a validation task for periodic execution.
	 *
	 * @param workflowID     The unique ID of the workflow to be validated. Multiple
	 *                       tasks can be registered with the same ID, granted that
	 *                       they target another formula to be validated against the
	 *                       workflow (i.e. another parameter value <em>af</em> for
	 *                       every new task for workflow <em>workflowID</em>).
	 * @param af             The formula to be validated against the workflow.
	 * @param detailedOutput Decides if the evaluation output for the most recent
	 *                       validation result should also include detailed
	 *                       information (e.g. what audit trail entries (tasks)
	 *                       passed and which did not).
	 * @param initialDelay   The delay before the task is executed for the first
	 *                       time.
	 * @param delay          The delay between the periodic executions of the task.
	 * @param timeUnit       The time unit for the specified delays.
	 */
	public void registerValidationTask(String workflowID, AvailableFormulas af, boolean detailedOutput,
			long initialDelay, long delay, TimeUnit timeUnit) {

		// register the validation task
		validationScheduler.scheduleAtFixedRate(new ValidationTask(workflowID, af, detailedOutput), initialDelay, delay,
				timeUnit);
	}

	/**
	 * Update the desired task stack with the most recent state of the workflow to
	 * be checked periodically.
	 *
	 * @param workflowID      The workflow identifier mapped to a certain formula.
	 * @param formulaName     The formulaName (enum-value, see
	 *                        {@link LTLFormulaProvider}) mapped to the correct
	 *                        transition event stack.
	 * @param transitionEvent Transition event (containing the workflow instance) to
	 *                        be pushed to the stack mapped to the formulaName.
	 */
	private void updateTaskStack(String workflowID, AvailableFormulas formulaName,
			TaskStateTransitionEvent transitionEvent) {
		if (!periodicTaskMap.containsKey(workflowID)) {
			HashMap<String, Stack<TaskStateTransitionEvent>> tempMap = new HashMap<>();
			Stack<TaskStateTransitionEvent> tempStack = new Stack<>();
			tempStack.push(transitionEvent);
			tempMap.put(formulaName.toString(), tempStack);
			periodicTaskMap.put(workflowID, tempMap);
		} else {
			if (!periodicTaskMap.get(workflowID).containsKey(formulaName.toString())) {
				Stack<TaskStateTransitionEvent> tempStack = new Stack<>();
				tempStack.push(transitionEvent);
				periodicTaskMap.get(workflowID).put(formulaName.toString(), tempStack);
			} else {
				periodicTaskMap.get(workflowID).get(formulaName.toString()).push(transitionEvent);
			}
		}
	}

	/**
	 * Update the respective task stacks with the most recent state of the workflow
	 * to be checked periodically.
	 *
	 * @param workflowID      The workflow identifier mapped to a certain formula.
	 * @param formulas        The formula names (enum-values, see
	 *                        {@link LTLFormulaProvider}) mapped to the correct
	 *                        transition event stack.
	 * @param transitionEvent Transition event (containing the workflow instance) to
	 *                        be pushed to the stack mapped to the formulaName.
	 */
	public void updateTaskStack(String workflowID, List<AvailableFormulas> formulas,
			TaskStateTransitionEvent transitionEvent) {
		if (formulas != null) {
			for (AvailableFormulas af : formulas) {
				updateTaskStack(workflowID, af, transitionEvent);
			}
		}
	}

	/**
	 * Check if the process log (processLogObj) of workflow with workflowID
	 * satisfies the LTL formula(s) denoted by ltlFormulaDefinitionKey.
	 *
	 * @param workflowID              The unique ID of the workflow the validation
	 *                                procedure is to be invoked for. This
	 *                                identifier can be reused for validating
	 *                                additional formulas for the same workflow.
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
			resultStack.push(RuntimeParser.checkLTLTrace(workflowID, ltlFormulaDefinitionKey, transitionEvent));
			workflowMap.put(ltlFormulaDefinitionKey.toString(), resultStack);
			checkResults.put(workflowID, workflowMap);
		} else {
			if (!checkResults.get(workflowID).containsKey(ltlFormulaDefinitionKey.toString())) {
				Stack<ArrayList<ValidationResult>> resultStack = new Stack<ArrayList<ValidationResult>>();
				resultStack.push(RuntimeParser.checkLTLTrace(workflowID, ltlFormulaDefinitionKey, transitionEvent));
				checkResults.get(workflowID).put(ltlFormulaDefinitionKey.toString(), resultStack);
			} else {
				checkResults.get(workflowID).get(ltlFormulaDefinitionKey.toString())
						.push(RuntimeParser.checkLTLTrace(workflowID, ltlFormulaDefinitionKey, transitionEvent));
			}
		}
	}

	/**
	 * Evaluate if the most recent execution of the validation algorithm for a
	 * certain <em>static</em> formula resulted in the same set of failed and
	 * successful process instances or if anything changed for the better or worse.
	 *
	 * @param workflowID           The unique ID of the workflow mapped to the
	 *                             formula name which in turn is mapped to the
	 *                             desired formula validation result.
	 * @param formulaDefinitionKey The identifier of the formula associated with the
	 *                             validation results to be compared.
	 * @param detailedOutput       Decides if the evaluation output for the most
	 *                             recent validation result should also include
	 *                             detailed information (e.g. what audit trail
	 *                             entries (tasks) passed and which did not).
	 */
	private void evaluateResults(String workflowID, AvailableFormulas formulaDefinitionKey, boolean detailedOutput) {
		ValidationMode evalMode = LTLFormulaProvider.getFormulaDefinition(formulaDefinitionKey.toString())
				.getValidationMode();

		log.debug("WORKFLOW NAME: {}", workflowID);
		if (evalMode.equals(ValidationMode.STATIC)) {
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

			ArrayList<ValidationResult> topResult = tempStack.peek();
			ArrayList<ValidationResult> olderResult = tempStack.elementAt(tempStack.size() - 2);

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
		} else {
			evaluateTraceResults(workflowID, formulaDefinitionKey);
		}
	}

	/**
	 * Evaluate the validation results of a formula of type TRACE.
	 *
	 * @param workflowID           The unique ID of the workflow mapped to the
	 *                             formula name which in turn is mapped to the
	 *                             desired formula validation result.
	 * @param formulaDefinitionKey The identifier of the formula associated with the
	 *                             validation results to be compared.
	 */
	private void evaluateTraceResults(String workflowID, AvailableFormulas formulaDefinitionKey) {
		LTLFormulaObject formula = LTLFormulaProvider.getFormulaDefinition(formulaDefinitionKey.toString());

		if (formula.getTraceProperties() != null && !formula.getTraceProperties().isEmpty()) {

			Stack<ArrayList<ValidationResult>> tempStack = checkResults.get(workflowID)
					.get(formulaDefinitionKey.toString());

			if (tempStack == null) {
				return;
			}

			ArrayList<ValidationResult> topResults = tempStack.peek();
			TreeMap<String, HashMap<String, Boolean>> ateResults = topResults.get(0).getAuditTrailEntryResults();

			boolean currTraceViolated = false;
			Entry<String, HashMap<String, Boolean>> newestResults = ateResults.lastEntry();
			for (Entry<String, Boolean> subEntry : newestResults.getValue().entrySet()) {
				if (subEntry.getValue()) {
					currTraceViolated = true;
					break;
				}
			}

			boolean isTraceViolated = formula.getTraceProperties().get(LTLFormulaConstants.KEY_TRACE_VIOLATED);

			List<String> startTasks = formula.getFormulaProperties().get(LTLFormulaConstants.KEY_TRACE_START);
			List<String> endTasks = formula.getFormulaProperties().get(LTLFormulaConstants.KEY_TRACE_END);

			if (currTraceViolated && !isTraceViolated) {
				log.debug("Constraint violation found between the states of task(s) {} and {} in current trace.",
						startTasks.toString(), endTasks.toString());
			} else if (!currTraceViolated && isTraceViolated) {
				log.debug("Constraint violation between task(s) {} and task(s) {} has been repaired.",
						startTasks.toString(), endTasks.toString());
			} else {
				log.debug("No new constraint violations identified in current trace.");
			}

			// save current violation state of the trace
			formula.getTraceProperties().replace(LTLFormulaConstants.KEY_TRACE_VIOLATED, currTraceViolated);
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
	 * Reset the validation environment (i.e., call the garbage collector to collect
	 * the current {@link LTLValidationManager} class instance, all recorded
	 * validation results, etc.).
	 */
	public void resetValidationEnvironment() {
		instance = null;
		System.gc();
	}

	/**
	 * Call the validation routine (parser, validator, etc.) and evaluate the
	 * received results.
	 *
	 * @param workflowID      The unique ID of the workflow the validation procedure
	 *                        is to be invoked for. This identifier can be reused
	 *                        for validating additional formulas for the same
	 *                        workflow.
	 * @param transitionEvent The object holding the information necessary for the
	 *                        process log (e.g. XML) conversion process.
	 * @param af              The formula to be validated against the converted
	 *                        process log.
	 * @param detailedOutput  Decides if the evaluation output for the most recent
	 *                        validation result should also include detailed
	 *                        information (e.g. what audit trail entries (tasks)
	 *                        passed and which did not).
	 */
	private void validate(String workflowID, TaskStateTransitionEvent transitionEvent, AvailableFormulas af,
			boolean detailedOutput) {
		long startTime = System.currentTimeMillis();

		// conduct validation
		checkTraceAndStoreResult(workflowID, transitionEvent, af);
		evaluateResults(workflowID, af, detailedOutput);

		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);

		// report time taken
		log.info("Validation of formula {} took {} milliseconds.", af.toString(), duration);
	}

	/**
	 * Call the validation routine (parser, validator, etc.) and evaluate the
	 * received results.
	 *
	 * @param workflowID      The unique ID of the workflow the validation procedure
	 *                        is to be invoked for. This identifier can be reused
	 *                        for validating additional formulas for the same
	 *                        workflow.
	 * @param transitionEvent The object holding the information necessary for the
	 *                        process log (e.g. XML) conversion process.
	 * @param formulas        List of all formulas to be validated against the
	 *                        converted process log.
	 * @param detailedOutput  Decides if the evaluation output for the most recent
	 *                        validation result should also include detailed
	 *                        information (e.g. what audit trail entries (tasks)
	 *                        passed and which did not).
	 */
	public void validate(String workflowID, TaskStateTransitionEvent transitionEvent, List<AvailableFormulas> formulas,
			boolean detailedOutput) {
		if (formulas != null) {
			for (AvailableFormulas af : formulas) {
				validate(workflowID, transitionEvent, af, detailedOutput);
			}
		}
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
			try {
				// obtain the stack holding the desired transition events
				Stack<TaskStateTransitionEvent> tempStack = periodicTaskMap.get(workflowID).get(af.toString());

				// if the stack is empty at this point in time, there is nothing to evaluate yet
				if (!tempStack.isEmpty()) {

					// retrieve most recent transition event
					TaskStateTransitionEvent transitionEvent = tempStack.peek();

					// nothing to evaluate if the top-element is null
					if (transitionEvent != null) {
						// reset the stack
						periodicTaskMap.get(workflowID).get(af.toString()).clear();

						// record the time required for finishing scheduled validation task
						long startTime = System.currentTimeMillis();

						// call the validation routine
						checkTraceAndStoreResult(workflowID, transitionEvent, af);
						evaluateResults(workflowID, af, detailedOutput);

						long endTime = System.currentTimeMillis();
						long duration = (endTime - startTime);

						// report time taken
						log.info("Scheduled validation of formula {} took {} milliseconds.", af.toString(), duration);
					}
				}
			} catch (Exception ex) {
				log.error("Could not execute periodic validation task for workflow '{}' and formula(s) '{}'.",
						workflowID, af.toString(), ex);
			}
		}
	}
}
