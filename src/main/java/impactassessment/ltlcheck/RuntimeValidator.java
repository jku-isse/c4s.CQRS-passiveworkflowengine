package impactassessment.ltlcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.analysis.ltlchecker.CheckResult;
import org.processmining.analysis.ltlchecker.ParamData;
import org.processmining.analysis.ltlchecker.SetsSet;
import org.processmining.analysis.ltlchecker.Substitutes;
import org.processmining.analysis.ltlchecker.TreeBuilder;
import org.processmining.analysis.ltlchecker.formulatree.FormulaNode;
import org.processmining.analysis.ltlchecker.formulatree.RootNode;
import org.processmining.analysis.ltlchecker.parser.LTLParser;
import org.processmining.analysis.ltlchecker.parser.SimpleNode;
import org.processmining.framework.log.AuditTrailEntries;
import org.processmining.framework.log.AuditTrailEntry;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.log.ProcessInstance;

import impactassessment.ltlcheck.framework.CustomSetsSet;
import impactassessment.ltlcheck.util.ValidationUtil;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationSelection;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chris
 */
@Slf4j
public class RuntimeValidator {

	/** list with formulas **/
	private ArrayList<String> formulaList;

	/** check option: validate till first successful check **/
	private boolean firstSuccess;

	/** check option: validate till first failed check **/
	private boolean firstFailure;

	/** parser instance with parsed data **/
	private LTLParser parser;

	/** set of defined sets in LTL file **/
	private SetsSet setOfSets;

	/** the process log the formulas should be validated against **/
	private LogReader logReader;

	/** list containing the succeeded checks **/
	private ArrayList<CheckResult> goodResults;

	/** list containing the failed checks **/
	private ArrayList<CheckResult> badResults;

	/** the formula to be checked against the process log **/
	private Pair<ValidationUtil.ValidationSelection, String> validationSelection;

	public RuntimeValidator(LogReader logReader, LTLParser parser, Pair<Boolean, Boolean> processConstraints,
			Pair<ValidationSelection, String> validationSelection) {
		this.parser = parser;
		this.logReader = logReader;

		this.setOfSets = null;
		this.validationSelection = (validationSelection == null) ? Pair.of(ValidationSelection.ANY, "")
				: validationSelection;

		this.firstSuccess = (processConstraints == null) ? false : processConstraints.getKey();
		this.firstFailure = (processConstraints == null) ? false : processConstraints.getValue();

		formulaList = new ArrayList<String>();

		Iterator<?> iter = this.parser.getVisibleFormulaNames().iterator();
		while (iter.hasNext()) {
			formulaList.add((String) iter.next());
		}
	}

	/**
	 * @return boolean determining if the whole process log should be checked or if
	 *         the process should be stopped once the first success or failure is
	 *         found
	 */
	@SuppressWarnings("unused")
	private boolean getWholeProcess() {
		return ((!this.firstSuccess) && (!this.firstFailure));
	}

	public ArrayList<ValidationResult> validate() {
		if (formulaList.isEmpty()) {
			log.debug("No formulas have been specified!");
			return null;
		}

		ArrayList<ValidationResult> results = new ArrayList<>();
		if (validationSelection.getKey().equals(ValidationSelection.ALL)) {
			for (String s : formulaList) {
				results.add(validateInternal(parser.getFormula(s)));
			}
		} else if (validationSelection.getKey().equals(ValidationSelection.ANY)) {
			results.add(validateInternal(parser.getFormula(formulaList.get(new Random().nextInt(formulaList.size())))));
		} else {
			String selection = validationSelection.getValue();
			if (selection.equals("") || !formulaList.contains(selection)) {
				log.debug("The specified formula has not been defined!");
				return null;
			}
			results.add(validateInternal(parser.getFormula(validationSelection.getValue())));
		}
		return results;
	}

	private ValidationResult validateInternal(SimpleNode node) {
		String formulaName = node.getName();
		int piCntTotal = 0;
		int piCntCurr = 0;

		if (setOfSets == null) {
			log.debug("Creating attribute sets...");
			setOfSets = new CustomSetsSet(parser, logReader.getLogSummary());
			setOfSets.fill(logReader, null);
			piCntTotal = logReader.getLogSummary().getNumberOfProcessInstances();
		}

		log.debug("Building formula tree...");
		TreeBuilder tb = new TreeBuilder(parser, formulaName, setOfSets);

		// Create the set of substitutes if there are any present.
		// That is, if the current context is a template, it can already have
		// substitutes. Therefore, these substitutes should be used. Otherwise, an empty
		// set of substitutes suffices.
		RootNode root = new RootNode();
		Substitutes subs = getSubstitutes(node);
		subs.setBinder(root);

		root.setFormula((FormulaNode) tb.build(node, subs, root));

		// conduct the validation
		goodResults = new ArrayList<>();
		badResults = new ArrayList<>();

		int currPiNumber = 0;

		boolean run = true;
		boolean fulfill = false;

		log.debug("Validating formula " + node.getName() + "...");

		logReader.reset();

		// collect evaluation results of every audit trail entry of every process
		// instance
		HashMap<String, HashMap<String, Boolean>> ateResults = new HashMap<>();

		while (logReader.hasNext() && run) {
			ProcessInstance pi = logReader.next();
			AuditTrailEntries ates = pi.getAuditTrailEntries();

			// create result entry for current process instance in ateResults map
			ateResults.put(pi.getName(), new HashMap<String, Boolean>());

			log.debug("Process Instance " + pi.getName() + " [" + (++piCntCurr) + "/" + piCntTotal + "]");

			// The process instance must be walked through in reverse. As the ProM framework
			// does not support this, the audit trail entries of the current process
			// instance are first read into a list.

			ates.reset();
			LinkedList<AuditTrailEntry> atesList = new LinkedList<>();

			while (ates.hasNext()) {
				AuditTrailEntry ate = ates.next();
				atesList.add(ate);
			}

			fulfill = false;

			for (int j = atesList.size(); j >= 0; j--) {
				// start with n + 1
				fulfill = root.value(pi, atesList, j);

				// record evaluation result of current audit trail entry
				if (j < atesList.size()) {
					ateResults.get(pi.getName()).put(atesList.get(j).getElement(), fulfill);
				}
			}

			// The computed boolean-value for the variable fulfill of the last audit trail
			// entry of the process entry is the value of the whole process instance.
			// Because of the use of dynamic programming the associated values for the other
			// audit trail entries have already been computed and used.
			if (!fulfill && firstFailure) {
				// stop at first failure
				run = false;
			} else if (fulfill && firstSuccess) {
				run = false;
			}

			if (fulfill) {
				goodResults.add(new CheckResult(currPiNumber, pi));
			} else {
				badResults.add(new CheckResult(currPiNumber, pi));
			}

			// The process instance counter can only be incremented after checking the
			// current process instance as the appropriate numbering starts with 0.
			currPiNumber++;
		}
		log.debug("Validation completed:\n" + "\tPassed process instances: " + goodResults.size() + "\n"
				+ "\tFailed process instances: " + badResults.size());

		return new ValidationResult(formulaName, logReader, goodResults, badResults, ateResults);
	}

	private Substitutes getSubstitutes(SimpleNode node) {
		return new ParamData(parser.getParameters(node.getName())).getSubstitutes(parser);
	}
}
