package impactassessment.model.definition;

import impactassessment.model.workflowmodel.ResourceLink;

public class ArtifactTypes {

	public static final String ARTIFACT_TYPE_JIRA_TICKET = "ARTIFACT_TYPE_JIRA_TICKET";
	public static final String ARTIFACT_TYPE_RESOURCE_LINK = "ARTIFACT_TYPE_RESOURCE_LINK";
	
	public static ResourceLink getLink(String href, String title) {
		return new ResourceLink(ARTIFACT_TYPE_RESOURCE_LINK, href, "self", "", "application/json", title);
	}
}
