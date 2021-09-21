package impactassessment.ltlcheck;

import java.util.HashMap;

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
		OUTPUT_MISSING
	}

	/**
	 * Map containing defined formulas.
	 **/
	private static HashMap<AvailableFormulas, LTLFormulaObject> formulas = new HashMap<>();

	/**
	 * Statically build all available formulas.
	 */
	static {
		buildOutputMissingFormula();
	}

	/**
	 * Build formula <em>OUTPUT_MISSING</em>.
	 */
	private static void buildOutputMissingFormula() {
		String formulaName = AvailableFormulas.OUTPUT_MISSING.toString();

		String formulaDefinition = buildOutputMissingFormulaDefinition(formulaName);

		ValidationMode vm = ValidationMode.STATIC;

		LTLFormulaObject formulaObj = new LTLFormulaObject(formulaName, formulaDefinition, vm);

		formulas.put(AvailableFormulas.OUTPUT_MISSING, formulaObj);
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

		return sb.toString();
	}

	/**
	 * @param key The formula name mapped to a {@link LTLFormulaObject} in map
	 *            <code>formulas</code>.
	 * @return the formula definition of the {@link LTLFormulaObject} identified
	 *         by @param key
	 */
	public static LTLFormulaObject getFormulaDefinition(AvailableFormulas key) {
		return (key == null) ? null : formulas.get(key);
	}
}
