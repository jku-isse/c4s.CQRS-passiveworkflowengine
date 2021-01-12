package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.jira.subtypes.IJiraIssueType;
import com.atlassian.jira.rest.client.api.domain.IssueType;

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
