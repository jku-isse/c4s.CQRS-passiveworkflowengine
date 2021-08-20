package impactassessment.ltlcheck.util;

import java.util.HashMap;

/**
 * @author chris
 */
public class LTLTaskObject {

	// taskID
	private String workflowModelElement;

	// EventType
	private String eventType;

	// Originator
	private String originator;

	// additional attributes
	private HashMap<String, String> attributes;

	public LTLTaskObject(String wfModelElement, String eventType, String originator, HashMap<String, String> attr) {
		this.workflowModelElement = wfModelElement;
		this.eventType = eventType;
		this.originator = originator;
		this.attributes = attr;
	}

	public LTLTaskObject(String wfModelElement, String eventType, String originator) {
		this(wfModelElement, eventType, originator, null);
	}

	public String getWorkflowModelElement() {
		return workflowModelElement;
	}

	public void setWorkflowModelElement(String workflowModelElement) {
		this.workflowModelElement = workflowModelElement;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}
}
