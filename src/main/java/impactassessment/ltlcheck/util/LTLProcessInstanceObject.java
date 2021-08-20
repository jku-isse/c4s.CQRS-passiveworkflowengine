package impactassessment.ltlcheck.util;

import java.util.HashMap;
import java.util.List;

/**
 * @author chris
 */
public class LTLProcessInstanceObject {

	// process instance ID
	private String workflowID;

	// additional attributes
	private HashMap<String, String> attributes;

	// audit trail entries
	private List<LTLTaskObject> auditTrailEntries;

	public LTLProcessInstanceObject(String workflowID) {
		this.workflowID = workflowID;
	}

	public String getWorkflowID() {
		return workflowID;
	}

	public void setWorkflowID(String workflowID) {
		this.workflowID = workflowID;
	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}

	public List<LTLTaskObject> getAuditTrailEntries() {
		return auditTrailEntries;
	}

	public void setAuditTrailEntries(List<LTLTaskObject> auditTrailEntries) {
		this.auditTrailEntries = auditTrailEntries;
	}
}
