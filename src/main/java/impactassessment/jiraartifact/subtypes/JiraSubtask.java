package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.Subtask;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueType;
import impactassessment.jiraartifact.subinterfaces.IJiraStatus;
import impactassessment.jiraartifact.subinterfaces.IJiraSubtask;

import java.net.URI;

public class JiraSubtask implements IJiraSubtask {

    private Subtask task;
    private IJiraStatus status;
    private IJiraIssueType issueType;

    public JiraSubtask(Subtask task) {
        this.task = task;
        this.status = new JiraStatus(task.getStatus());
        this.issueType = new JiraIssueType(task.getIssueType());
    }

    @Override
    public String getIssueKey() {
        return task.getIssueKey();
    }

    @Override
    public URI getIssueUri() {
        return task.getIssueUri();
    }

    @Override
    public String getSummary() {
        return task.getSummary();
    }

    @Override
    public IJiraIssueType getIssueType() {
        return issueType;
    }

    @Override
    public IJiraStatus getStatus() {
        return status;
    }
}
