package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueLink;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueLink;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueLinkType;

import java.net.URI;

public class JiraIssueLink implements IJiraIssueLink {

    private IssueLink issueLink;
    private IJiraIssueLinkType jiraIssueLinkType;

    public JiraIssueLink(IssueLink issueLink) {
        this.issueLink = issueLink;
        this.jiraIssueLinkType = new JiraIssueLinkType(issueLink.getIssueLinkType());
    }

    @Override
    public String getTargetIssueKey() {
        return issueLink.getTargetIssueKey();
    }

    @Override
    public URI getTargetIssueUri() {
        return issueLink.getTargetIssueUri();
    }

    @Override
    public IJiraIssueLinkType getIssueLinkType() {
        return jiraIssueLinkType;
    }
}
