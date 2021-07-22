package impactassessment.ltlcheck;

import java.util.ArrayList;

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

	/** name of the evaluated formula **/
	private String formulaName;

	public ValidationResult(String formulaName, LogReader log, ArrayList<CheckResult> goodResults,
			ArrayList<CheckResult> badResults) {
		this.formulaName = formulaName;
		this.logReader = log;
		this.goodResults = goodResults;
		this.badResults = badResults;
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
