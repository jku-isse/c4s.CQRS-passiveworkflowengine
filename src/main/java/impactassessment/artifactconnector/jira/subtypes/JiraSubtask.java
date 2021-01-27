package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.IJiraIssueType;
import artifactapi.jira.subtypes.IJiraStatus;
import artifactapi.jira.subtypes.IJiraSubtask;
import impactassessment.artifactconnector.jira.IJiraService;

import com.atlassian.jira.rest.client.api.domain.Subtask;

import java.net.URI;
import java.util.Optional;

public class JiraSubtask implements IJiraSubtask {

    private Subtask task;
    private IJiraStatus status;
    private IJiraIssueType issueType;
    private transient IJiraService jiraService = null;

    public JiraSubtask(Subtask task, IJiraService service) {
    	this.jiraService = service;
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

    @Override
    public void injectArtifactService(IArtifactService iArtifactService) {
        if (iArtifactService instanceof IJiraService) // will be always the case
            jiraService = (IJiraService)iArtifactService;
    }

    //@Override
    public Optional<IJiraArtifact> getSubtask() {
    	return  jiraService.getIssue(task.getIssueKey());
    }
}
