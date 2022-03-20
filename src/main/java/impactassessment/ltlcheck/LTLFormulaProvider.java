package impactassessment.ltlcheck;

import java.util.HashMap;
import java.util.List;

import impactassessment.ltlcheck.util.LTLFormulaConstants;
import impactassessment.ltlcheck.util.LTLFormulaObject;
import impactassessment.ltlcheck.util.ValidationUtil.ValidationMode;

/**
 * Formulas (e.g. valid "LTL-files") will be constructed here and subsequently
 * passed on to the {@link RuntimeParser} which will check them for validity
 * before invoking the {@link RuntimeValidator}.
 *
 * @author chris
 */
public class LTLFormulaProvider {

	/**
	 * Enum holding the names of every defined formula.
	 */
	public enum AvailableFormulas {
		OUTPUT_MISSING, DETECT_WF_INCONSISTENCY
	}

	/**
	 * Map containing defined formulas.
	 **/
	private static HashMap<String, LTLFormulaObject> formulas = new HashMap<>();

	/**
	 * Statically build all available formulas.
	 */
	static {
		buildOutputMissingFormula();
		buildDetectWorkflowInconsistencyFormula();
	}

	/**
	 * Build formula <em>OUTPUT_MISSING</em>.
	 */
	private static void buildOutputMissingFormula() {
		String formulaName = AvailableFormulas.OUTPUT_MISSING.toString();

		String formulaDefinition = buildOutputMissingFormulaDefinition(formulaName);

		ValidationMode vm = ValidationMode.STATIC;

		LTLFormulaObject formulaObj = new LTLFormulaObject(formulaName, formulaDefinition, vm);

		formulas.put(formulaName, formulaObj);
	}

	/**
	 * @param formulaName The name of this formula.
	 * @return a string containing a formula definition checking for missing output
	 *         artifacts of individual tasks at a single point in time
	 */
	private static String buildOutputMissingFormulaDefinition(String formulaName) {
		StringBuilder sb = new StringBuilder();
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("string ate.OUTPUT;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("rename ate.OUTPUT as output;\n");
		sb.append("# formulas\n");
		sb.append("formula " + formulaName + "() := {\n");
		sb.append("}\n");
		sb.append(LTLFormulaConstants.EVENTUALLY + "( output == \"OUTPUT_MISSING\");");
		// sb.append("(output == \"OUTPUT_MISSING\" " + LTLFormulaConstants.UNTIL + "
		// output == \"OUTPUT_SUFFICIENT\");");

		return sb.toString();
	}

	/**
	 * Build formula <em>DETECT_WF_INCONSISTENCY</em>.
	 */
	private static void buildDetectWorkflowInconsistencyFormula() {
		String formulaName = AvailableFormulas.DETECT_WF_INCONSISTENCY.toString();

		String formulaDefinition = buildDetectWorkflowInconsistencyDefinition(formulaName);

		ValidationMode vm = ValidationMode.TRACE;

		// default value for property KEY_TRACE_VIOLATED
		HashMap<String, Boolean> traceProperties = new HashMap<>();
		traceProperties.put(LTLFormulaConstants.KEY_TRACE_VIOLATED, Boolean.FALSE);

		HashMap<String, List<String>> formulaProperties = new HashMap<>();
		formulaProperties.put(LTLFormulaConstants.KEY_TRACE_START, List.of("Specifying"));
		formulaProperties.put(LTLFormulaConstants.KEY_TRACE_END, List.of("Modeling"));

		LTLFormulaObject formulaObj = new LTLFormulaObject(formulaName, formulaDefinition, vm, traceProperties,
				formulaProperties);

		formulas.put(formulaName, formulaObj);
	}

	/**
	 * @param formulaName The name of this formula.
	 * @return a string containing a formula definition checking for workflow
	 *         inconsistencies such as a invalid state of a task following another
	 *         one
	 */
	private static String buildDetectWorkflowInconsistencyDefinition(String formulaName) {
		StringBuilder sb = new StringBuilder();
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("string ate.ACTUAL_STATE;\n");
		sb.append("string ate.PREVIOUS_STATE;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.ACTUAL_STATE as state;\n");
		sb.append("rename ate.PREVIOUS_STATE as pstate;\n");
		sb.append("# formulas\n");
		sb.append("formula " + formulaName + "() := {\n");
		sb.append("}\n");
		sb.append(LTLFormulaConstants.EVENTUALLY
				+ " ( ( task == \"Specifying\" /\\ ( pstate == \"COMPLETED\" /\\ ( state == \"ENABLED\" /\\ "
				+ LTLFormulaConstants.EVENTUALLY
				+ " ( ( task == \"Modeling\" /\\ (pstate == \"COMPLETED\" /\\ state == \"ENABLED\" ) ) ) ) ) ) );");
//				+ LTLFormulaConstants.EVENTUALLY
//				+ " ( ( task == \"Modeling\" /\\ pstate == \"COMPLETED\" ) ) ) ) ) );");

		return sb.toString();
	}

	/**
	 * @param key The formula name mapped to a {@link LTLFormulaObject} in map
	 *            <code>formulas</code>.
	 * @return the formula definition of the {@link LTLFormulaObject} identified
	 *         by @param key
	 */
	public static LTLFormulaObject getFormulaDefinition(String key) {
		return (key != null && !key.isEmpty()) ? formulas.get(key) : null;
	}
}
