package impactassessment.ltlcheck.util;

import java.util.HashMap;
import java.util.List;

import impactassessment.ltlcheck.util.ValidationUtil.ValidationMode;

/**
 * An instance of this class represents a single LTL-formula.
 *
 * @author chris
 */
public class LTLFormulaObject {

	/** The LTL-formula's name. **/
	private String formulaName;

	/**
	 * The formula definition to be parsed and subsequently tested against a process
	 * log.
	 **/
	private String formulaDefinition;

	/**
	 * The validation mode associated with this formula (either trace-oriented or
	 * static).
	 **/
	private ValidationMode validationMode;

	/**
	 * Trace properties associated with this formula. Not required for formulas
	 * investigating static process log properties (see {@link ValidationUtil}).
	 **/
	private HashMap<String, Boolean> traceProperties;

	/**
	 * General properties associated with this formula (e.g. start and end task(s)
	 * which are under inspection by a formula). Not required for formulas
	 * investigating static process log properties (see {@link ValidationUtil}).
	 */
	private HashMap<String, List<String>> formulaProperties;

	/**
	 * Empty constructor for convenience (as we have setters anyway).
	 */
	public LTLFormulaObject() {
	}

	/**
	 * Constructor for formulas without any additional properties.
	 */
	public LTLFormulaObject(String formulaName, String formulaDefinition, ValidationMode vm) {
		this(formulaName, formulaDefinition, vm, null, null);
	}

	/**
	 * Constructor for formulas with additional properties.
	 */
	public LTLFormulaObject(String formulaName, String formulaDefinition, ValidationMode vm,
			HashMap<String, Boolean> traceProperties, HashMap<String, List<String>> formulaProperties) {
		this.formulaName = formulaName;
		this.formulaDefinition = formulaDefinition;
		this.validationMode = vm;
		this.formulaProperties = formulaProperties;
		this.traceProperties = traceProperties;
	}

	public String getFormulaName() {
		return formulaName;
	}

	public void setFormulaName(String formulaName) {
		this.formulaName = formulaName;
	}

	public String getFormulaDefinition() {
		return formulaDefinition;
	}

	public void setFormulaDefinition(String formulaDefinition) {
		this.formulaDefinition = formulaDefinition;
	}

	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	public HashMap<String, List<String>> getFormulaProperties() {
		return formulaProperties;
	}

	public void setFormulaProperties(HashMap<String, List<String>> formulaProperties) {
		this.formulaProperties = formulaProperties;
	}

	public HashMap<String, Boolean> getTraceProperties() {
		return traceProperties;
	}

	public void setTraceProperties(HashMap<String, Boolean> traceProperties) {
		this.traceProperties = traceProperties;
	}
}
