package impactassessment.ltlcheck.util;

import impactassessment.ltlcheck.LTLFormulaProvider;

/**
 * This class holds constants needed for formulating valid LTL formulas
 * alongside constants needed in the {@link LTLFormulaProvider} as well as in
 * instances of class {@link LTLFormulaObject}.
 *
 * @author chris
 */
public class LTLFormulaConstants {

	/** constants required for formulating valid LTL formulas **/

	/** constants for quantifiers **/
	public static final String FORALL = "forall";
	public static final String EXISTS = "exists";

	/** constants for LTL operators **/
	public static final String ALWAYS = "[]"; // e.g. A(...)
	public static final String EVENTUALLY = "<>"; // e.g. F(...)
	public static final String NEXT_TIME = "_O"; // e.g. X(...)
	public static final String UNTIL = "_U"; // e.g. (... U ...)

	/** constants used in instances of {@link LTLFormulaObject} **/
	public static final String KEY_TRACE_START = "TRACE_START_POINT";
	public static final String KEY_TRACE_END = "TRACE_END_POINT";
	public static final String KEY_TRACE_VIOLATED = "TRACE_VIOLATED";
}
