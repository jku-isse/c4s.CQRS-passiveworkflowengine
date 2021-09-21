package impactassessment.ltlcheck.util;

import java.util.HashMap;

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
	 * Properties associated with this formula. Not required for formulas
	 * investigating static process log properties (see {@link ValidationUtil}).
	 **/
	private HashMap<String, Object> formulaProperties;

	/**
	 * Empty constructor for convenience (as we have setters anyway).
	 */
	public LTLFormulaObject() {
	}

	/**
	 * Constructor for formulas without any additional properties.
	 */
	public LTLFormulaObject(String formulaName, String formulaDefinition, ValidationMode vm) {
		this(formulaName, formulaDefinition, vm, null);
	}

	/**
	 * Constructor for formulas with additional properties.
	 */
	public LTLFormulaObject(String formulaName, String formulaDefinition, ValidationMode vm,
			HashMap<String, Object> formulaProperties) {
		this.formulaName = formulaName;
		this.formulaDefinition = formulaDefinition;
		this.validationMode = vm;
		this.formulaProperties = formulaProperties;
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

	public HashMap<String, Object> getFormulaProperties() {
		return formulaProperties;
	}

	public void setFormulaProperties(HashMap<String, Object> formulaProperties) {
		this.formulaProperties = formulaProperties;
	}
}
