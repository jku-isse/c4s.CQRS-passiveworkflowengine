package impactassessment.ltlcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.processmining.analysis.ltlchecker.CheckResult;
import org.processmining.framework.log.LogReader;

/**
 * Class which is used to aggregate the validation results of a single formula
 * validation conducted in {@link RuntimeValidator}.
 *
 * @author chris
 */
public class ValidationResult {

	/** the log checked by the analysis algorithm **/
	private LogReader logReader;

	/**
	 * the list containing the process instances which satisfied the evaluated
	 * formula
	 **/
	private ArrayList<CheckResult> goodResults;

	/**
	 * the list containing the process instances which did not satisfy the evaluated
	 * formula
	 **/
	private ArrayList<CheckResult> badResults;

	/**
	 * TreeMap containing key/value-pairs of type <processInstance, HashMap<audit
	 * trail entry, passed (boolean)>>. This collection maps every process instance
	 * to another map containing the evaluation results for the individual audit
	 * trail entries of the aforementioned process instance.
	 */
	private TreeMap<String, HashMap<String, Boolean>> ateResults;

	/** name of the evaluated formula **/
	private String formulaName;

	public ValidationResult(String formulaName, LogReader log, ArrayList<CheckResult> goodResults,
			ArrayList<CheckResult> badResults, TreeMap<String, HashMap<String, Boolean>> ateResults) {
		this.formulaName = formulaName;
		this.logReader = log;
		this.goodResults = goodResults;
		this.badResults = badResults;
		this.ateResults = ateResults;
	}

	public String getFormulaName() {
		return formulaName;
	}

	public ArrayList<CheckResult> getGoodResults() {
		return goodResults;
	}

	public ArrayList<CheckResult> getBadResults() {
		return badResults;
	}

	public TreeMap<String, HashMap<String, Boolean>> getAuditTrailEntryResults() {
		return ateResults;
	}

	public LogReader getLogReader() {
		return logReader;
	}

	public int getNrOfGoodResults() {
		return getGoodResults().size();
	}

	public int getNrOfBadResults() {
		return getBadResults().size();
	}
}
