package impactassessment.ltlcheck.util;

import impactassessment.ltlcheck.WorkflowDataExtractor;

/**
 * Constants intended for use in class {@link WorkflowDataExtractor}.
 *
 * @author chris
 */
public class LTLWorkflowConstants {

	/** constants used as values (for attributes, etc.) **/
	public static final String DEFAULT_EVENT_TYPE = "complete";

	/** constants used as identifiers (for attributes, etc.) **/
	public static final String INPUT_IDENTIFIER = "INPUT";
	public static final String OUTPUT_IDENTIFIER = "OUTPUT";
	public static final String PI_CREATION_TIMESTAMP_IDENTIFIER = "CREATION_TIMESTAMP";
	public static final String PI_CREATION_TIMESTAMP_FORMAT = "yyyy.MM.dd HH:mm:ss";
}
