package impactassessment.ltlcheck.convert;

/**
 * This class provides constants and methods used for both creating and
 * expanding a valid process log.
 *
 * @author chris
 */
public class WorkflowXmlUtility {

	/** path to temp-directory **/
	public static final String tempBaseDir = System.getProperty("java.io.tmpdir");

	/** file name constants **/
	public static final String WF_PROCESS_LOG_PREFIX = "wf_log_";
	public static final String WF_PROCESS_LOG_SUFFIX = ".xml";

	/** XML file-structure constants **/
	public static final String XML_META_DATA_TAG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	public static final String XML_WORKFLOW_LOG_TAG_OPEN = "<WorkflowLog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
			+ " xsi:noNamespaceSchemaLocation=\"WorkflowLog.xsd\">";
	public static final String XML_WORKFLOW_LOG_TAG_CLOSE = "</WorkflowLog>";

	public static final String XML_DATA_TAG_OPEN = "<Data>";
	public static final String XML_DATA_TAG_CLOSE = "</Data>";

	public static final String XML_PROCESS_TAG_CLOSE = "</Process>";

	public static final String XML_PROCESS_INSTANCE_TAG_CLOSE = "</ProcessInstance>";

	public static final String XML_AUDIT_TRAIL_ENTRY_TAG_OPEN = "<AuditTrailEntry>";
	public static final String XML_AUDIT_TRAIL_ENTRY_TAG_CLOSE = "</AuditTrailEntry>";

	public static final String XML_WORKFLOW_MODEL_ELEM_TAG_OPEN = "<WorkflowModelElement>";
	public static final String XML_WORKFLOW_MODEL_ELEM_TAG_CLOSE = "</WorkflowModelElement>";

	public static final String XML_EVENT_TYPE_TAG_OPEN = "<EventType>";
	public static final String XML_EVENT_TYPE_TAG_CLOSE = "</EventType>";

	public static final String XML_ORIGINATOR_TAG_OPEN = "<Originator>";
	public static final String XML_ORIGINATOR_TAG_CLOSE = "</Originator>";

	/** XML tag names for processing **/
	public static final String XML_TAG_NAME_PROCESS = "Process";
	public static final String XML_TAG_NAME_PI = "ProcessInstance";
	public static final String XML_TAG_NAME_ATE = "AuditTrailEntry";
	public static final String XML_TAG_NAME_DATA = "Data";
	public static final String XML_TAG_NAME_ATTRIBUTE = "Attribute";
	public static final String XML_TAG_NAME_WF_MODEL_ELEM = "WorkflowModelElement";
	public static final String XML_TAG_NAME_EVENT_TYPE = "EventType";
	public static final String XML_TAG_NAME_ORIGINATOR = "Originator";

	/** XML attributes for processing **/
	public static final String XML_ATTRIBUTE_ID = "id";
	public static final String XML_ATTRIBUTE_NAME = "name";

	/**
	 * special XML processing constants (these indices are context sensitive and
	 * therefore not generally applicable)
	 **/
	public static final int PROCESS_LOG_PROCESS_DOM_INDEX = 0;
	public static final int PROCESS_LOG_PI_DOM_INDEX = 0;
	public static final int PROCESS_LOG_PI_DATA_DOM_INDEX = 1;

	/**
	 * Build an 'attribute' tag made up of a single named XML attribute ('name') and
	 * a respective associated value.
	 *
	 * @param name  The name of the attribute.
	 * @param value The value to be encapsulated by the 'attribute' tag.
	 * @return a valid 'attribute' tag as string
	 */
	public static String buildAttributeTag(String name, String value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Attribute name=");
		sb.append("\"" + name + "\">");
		sb.append(value);
		sb.append("</Attribute>");
		return sb.toString();
	}

	/**
	 * Build a 'source' tag made up of a single named XML attribute ('program') and
	 * a respective associated value.
	 *
	 * @param program The value of the attribute 'program'.
	 * @param value   The value to be encapsulated by the 'source' tag.
	 * @return a valid 'source' tag as string
	 */
	public static String buildSourceTag(String program, String value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Source program=");
		sb.append("\"" + program + "\">");
		sb.append(value);
		sb.append("</Source>");
		return sb.toString();
	}

	/**
	 * Build an opening 'process' tag with its respective identifier as attribute.
	 *
	 * @param id The identifier associated with the process tag.
	 * @return a valid opening 'process tag'
	 */
	public static String buildProcessTagOpen(String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Process id=");
		sb.append("\"" + id + "\">");
		return sb.toString();
	}

	/**
	 * Build an opening 'process instance' tag with its respective identifier as
	 * attribute.
	 *
	 * @param id The identifier associated with the process instance.
	 * @return a valid opening 'process instance' tag
	 */
	public static String buildProcessInstanceTagOpen(String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ProcessInstance id=");
		sb.append("\"" + id + "\">");
		return sb.toString();
	}

	/**
	 * Build the default child-elements of an 'audit trail entry' tag.
	 *
	 * @param workflowModelElementValue The name of a task encapsulated by the
	 *                                  current 'audit trail entry' tag.
	 * @param eventTypeValue            The event type of the current 'audit trail
	 *                                  entry' tag.
	 * @param originatorValue           The originator of the current 'audit trail
	 *                                  entry' tag.
	 * @return the valid default child-elements of an 'audit trail entry' tag
	 */
	public static String buildDefaultAuditTrailEntryTags(String workflowModelElementValue, String eventTypeValue,
			String originatorValue) {
		StringBuilder sb = new StringBuilder();

		sb.append(XML_WORKFLOW_MODEL_ELEM_TAG_OPEN);
		sb.append(workflowModelElementValue);
		sb.append(XML_WORKFLOW_MODEL_ELEM_TAG_CLOSE);

		sb.append(XML_EVENT_TYPE_TAG_OPEN);
		sb.append(eventTypeValue);
		sb.append(XML_EVENT_TYPE_TAG_CLOSE);

		sb.append(XML_ORIGINATOR_TAG_OPEN);
		sb.append(originatorValue);
		sb.append(XML_ORIGINATOR_TAG_CLOSE);

		return sb.toString();
	}

	/**
	 * XSLT transformation definition used to prevent the XML transformer from
	 * adding unnecessary white spaces and blank lines when writing to the file
	 * system.
	 *
	 * @return XSLT transformation definition as string
	 */
	public static String getFormattingXSLT() {
		StringBuilder sb = new StringBuilder();
		sb.append("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n");
		sb.append("<xsl:output indent=\"yes\"/>\n");
		sb.append("<xsl:strip-space elements=\"*\"/>\n");
		sb.append("<xsl:template match=\"@*|node()\">\n");
		sb.append("<xsl:copy>\n");
		sb.append("<xsl:apply-templates select=\"@*|node()\"/>\n");
		sb.append("</xsl:copy>\n");
		sb.append("</xsl:template>\n");
		sb.append("</xsl:stylesheet>");
		return sb.toString();
	}
}
