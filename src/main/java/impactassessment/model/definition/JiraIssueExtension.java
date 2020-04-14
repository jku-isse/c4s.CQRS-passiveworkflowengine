package impactassessment.model.definition;

import impactassessment.model.workflowmodel.AbstractArtifact;
import impactassessment.model.workflowmodel.Artifact;
import impactassessment.model.workflowmodel.ArtifactType;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity
public class JiraIssueExtension extends AbstractArtifact implements Artifact {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Property
	String key;
	@Property
	boolean isSSSReviewFinished = false;
	@Property
	boolean isSSDDReviewFinished = false;
	
	@Property
	String sssReviewLink = "";
	@Property
	String ssddReviewLink = "";
	@Property
	String feddReviewLink = "";
	@Property
	String sss2srsMappingReviewLink = "";
	
	@Deprecated
	public JiraIssueExtension() {
		super();
	}
	
	public JiraIssueExtension(String key) {
		super();
		this.key = key;
		this.id = key;
	}
	
	
	
	@Override
	public String toString() {
		return "SimpleJiraTicket [key=" + key + ", isSSSReviewFinished=" + isSSSReviewFinished
				+ ", isSSDDReviewFinished=" + isSSDDReviewFinished + ", sssReviewLink=" + sssReviewLink
				+ ", ssddReviewLink=" + ssddReviewLink + ", feddReviewLink=" + feddReviewLink
				+ ", sss2srsMappingReviewLink=" + sss2srsMappingReviewLink + "]";
	}



	@Override
	public String getId() {
		return key;
	}	
	
	@Override
	public ArtifactType getType() {
		return new ArtifactType(WPManagementWorkflow.ARTIFACT_TYPE_JIRA_TICKET);
	}

	@Override
	public Artifact getParentArtifact() {		
		return null;
	}

	public boolean isSSSReviewFinished() {
		return isSSSReviewFinished;
	}

	public void setSSSReviewFinished(boolean isSSSReviewFinished) {
		this.isSSSReviewFinished = isSSSReviewFinished;
	}

	public boolean isSSDDReviewFinished() {
		return isSSDDReviewFinished;
	}

	public void setSSDDReviewFinished(boolean isSSDDReviewFinished) {
		this.isSSDDReviewFinished = isSSDDReviewFinished;
	}

	public String getSssReviewLink() {
		return sssReviewLink;
	}

	public void setSssReviewLink(String sssReviewLink) {
		this.sssReviewLink = sssReviewLink;
	}

	public String getSsddReviewLink() {
		return ssddReviewLink;
	}

	public void setSsddReviewLink(String ssddReviewLink) {
		this.ssddReviewLink = ssddReviewLink;
	}

	public String getFeddReviewLink() {
		return feddReviewLink;
	}

	public void setFeddReviewLink(String feddReviewLink) {
		this.feddReviewLink = feddReviewLink;
	}

	public String getSss2srsMappingReviewLink() {
		return sss2srsMappingReviewLink;
	}

	public void setSss2srsMappingReviewLink(String sss2srsMappingReviewLink) {
		this.sss2srsMappingReviewLink = sss2srsMappingReviewLink;		
	}

	public String getKey() {
		return key;
	}

	
	
}
