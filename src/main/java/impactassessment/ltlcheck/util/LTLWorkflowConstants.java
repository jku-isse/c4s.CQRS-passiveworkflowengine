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
	public static final String PROCESS_LOG_TIMESTAMP_FORMAT = "yyyy.MM.dd HH:mm:ss";

	/** constants used as identifiers (for attributes, etc.) **/
	public static final String INPUT_IDENTIFIER = "INPUT";
	public static final String OUTPUT_IDENTIFIER = "OUTPUT";
	public static final String ACTUAL_TASK_STATE = "ACTUAL_STATE";
	public static final String EXPECTED_TASK_STATE = "EXPECTED_STATE";
	public static final String CREATION_TIMESTAMP_IDENTIFIER = "CREATION_TIMESTAMP";
	public static final String MODIFICATION_TIMESTAMP_IDENTIFIER = "MODIFICATION_TIMESTAMP";
}
