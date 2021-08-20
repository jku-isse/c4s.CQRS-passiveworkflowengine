package impactassessment.ltlcheck;

import java.util.HashMap;

/**
 * Formulas (e.g. valid "LTL-files") will be constructed here and subsequently
 * passed on to the {@link RuntimeParser} which will check them for validity
 * before invoking the {@link RuntimeValidator}.
 *
 * @author chris
 */
public class LTLFormulaProvider {

	/**
	 * Entries in this enum must have the same name (including same case-usage,
	 * etc.) as a defined formula or start with the prefix MULT if the definition
	 * mapped to the enum value contains multiple formulas.
	 */
	public enum AvailableFormulas {
		SIMPLE_TEST, MULT_TEST, DOES_JOHN_DRIVE, COMPLEX_FORMULA, OUTPUT_MISSING
	}

	/**
	 * Map storing entries of type <key=AvailableFormulas.xxx, value=STRING (formula
	 * definition files)> for static (predefined) formulas.
	 **/
	private static HashMap<AvailableFormulas, String> staticFormulas = new HashMap<>();

	static {
		staticFormulas.put(AvailableFormulas.SIMPLE_TEST, buildTestLTLDefinition());
		staticFormulas.put(AvailableFormulas.MULT_TEST, buildTestForMultipleFormulas());
		staticFormulas.put(AvailableFormulas.DOES_JOHN_DRIVE, buildDoesJohnDriveFormula());
		staticFormulas.put(AvailableFormulas.COMPLEX_FORMULA, buildComplexFormula());
		staticFormulas.put(AvailableFormulas.OUTPUT_MISSING, buildOutputMissingFormula());
	}

	/** constants for quantifiers **/
	private static final String FORALL = "forall";
	private static final String EXISTS = "exists";

	/** constants for LTL operators **/
	private static final String ALWAYS = "[]"; // e.g. A(...)
	private static final String EVENTUALLY = "<>"; // e.g. F(...)
	private static final String NEXT_TIME = "_O"; // e.g. X(...)
	private static final String UNTIL = "_U"; // e.g. (... U ...)

	/**
	 * @return a string containing a single valid formula definition for testing
	 *         purposes
	 */
	private static String buildTestLTLDefinition() {
		StringBuilder sb = new StringBuilder();
		sb.append("# attributes\n");
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("# formulas\n");
		sb.append("formula SIMPLE_TEST() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "(task == \"asking\");");

		return sb.toString();
	}

	/**
	 * @return a string containing multiple valid formula definitions for testing
	 *         purposes
	 */
	private static String buildTestForMultipleFormulas() {
		StringBuilder sb = new StringBuilder();
		sb.append("# attributes\n");
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("# formulas\n");
		sb.append("formula test_one() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "(task == \"asking\");\n");
		sb.append("formula test_two() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "(person == \"Mary\");");

		return sb.toString();
	}

	/**
	 * @return a string containing a single valid formula definition
	 *         (DOES_JOHN_DRIVE) for testing purposes
	 */
	private static String buildDoesJohnDriveFormula() {
		StringBuilder sb = new StringBuilder();
		sb.append("# attributes\n");
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("# formulas\n");
		sb.append("formula DOES_JOHN_DRIVE() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "( (task == \"driving\" /\\ person == \"John\") );");

		return sb.toString();
	}

	/**
	 * @return a string containing a single complex formula definition for testing
	 *         purposes
	 */
	private static String buildComplexFormula() {
		StringBuilder sb = new StringBuilder();
		sb.append("# attributes\n");
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("# formulas\n");
		sb.append("formula COMPLEX_FORMULA() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "(" + ALWAYS + "( task == \"flying\"));");

		return sb.toString();
	}

	/**
	 * @return a string containing a formula definition checking for missing outputs
	 *         for individual tasks at a single point in time
	 */
	private static String buildOutputMissingFormula() {
		StringBuilder sb = new StringBuilder();
		sb.append("set ate.WorkflowModelElement;\n");
		sb.append("set ate.Originator;\n");
		sb.append("string ate.OUTPUT;\n");
		sb.append("# renamings\n");
		sb.append("rename ate.WorkflowModelElement as task;\n");
		sb.append("rename ate.Originator as person;\n");
		sb.append("rename ate.OUTPUT as output;\n");
		sb.append("# formulas\n");
		sb.append("formula OUTPUT_MISSING() := {\n");
		sb.append("}\n");
		sb.append(EVENTUALLY + "( output == \"OUTPUT_MISSING\");");

		return sb.toString();
	}

	/**
	 * @param key identifying a formula definition in map <code>formulas</code>
	 * @return the formula definition mapped to key
	 */
	public static String getFormulaDefinition(AvailableFormulas key) {
		return (key == null) ? null : staticFormulas.get(key);
	}
}
