package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueField;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueField;

public class JiraIssueField implements IJiraIssueField {

    private IssueField issueField;

    public JiraIssueField(IssueField issueField) {
        this.issueField = issueField;
    }

    @Override
    public String getId() {
        return issueField.getId();
    }

    @Override
    public String getName() {
        return issueField.getName();
    }

    @Override
    public String getType() {
        return issueField.getType();
    }

    @Override
    public Object getValue() {
        return issueField.getValue();
    }
}
