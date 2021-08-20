package impactassessment.ltlcheck.convert;

/**
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

	/** methods **/
	public static String buildAttributeTag(String name, String value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Attribute name=");
		sb.append("\"" + name + "\">");
		sb.append(value);
		sb.append("</Attribute>");
		return sb.toString();
	}

	public static String buildSourceTag(String name, String value) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Source program=");
		sb.append("\"" + name + "\">");
		sb.append(value);
		sb.append("</Source>");
		return sb.toString();
	}

	public static String buildProcessTagOpen(String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Process id=");
		sb.append("\"" + id + "\">");
		return sb.toString();
	}

	public static String buildProcessInstanceTagOpen(String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ProcessInstance id=");
		sb.append("\"" + id + "\">");
		return sb.toString();
	}

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
		sb.append(eventTypeValue);
		sb.append(XML_ORIGINATOR_TAG_CLOSE);

		return sb.toString();
	}
}
