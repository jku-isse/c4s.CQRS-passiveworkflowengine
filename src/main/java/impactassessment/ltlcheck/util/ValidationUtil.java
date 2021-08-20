package impactassessment.ltlcheck.util;

import impactassessment.ltlcheck.RuntimeValidator;

/**
 * @author chris
 */
public class ValidationUtil {

	/**
	 * Use this enum for specifying which formula is to be evaluated by the
	 * {@link RuntimeValidator}.
	 *
	 * ALL - every formula in a "LTL-file" formula definition should be evaluated
	 * ANY - a random formula in a "LTL-file" formula definition should be evaluated
	 * SPECIAL - a special formula in a "LTL-file" formula definition (e.g. the
	 * formula with name "xxx") should be evaluated
	 **/
	public enum ValidationSelection {
		ALL, ANY, SPECIAL
	}
}
