package impactassessment.artifactconnector.jira.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueType;
import impactassessment.artifactconnector.jira.subinterfaces.IJiraIssueType;

import java.net.URI;

public class JiraIssueType implements IJiraIssueType {

    private IssueType issueType;

    public JiraIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    @Override
    public Long getId() {
        return issueType.getId();
    }

    @Override
    public String getName() {
        return issueType.getName();
    }

    @Override
    public boolean isSubtask() {
        return issueType.isSubtask();
    }

    @Override
    public URI getSelf() {
        return issueType.getSelf();
    }

    @Override
    public String getDescription() {
        return issueType.getDescription();
    }
}
