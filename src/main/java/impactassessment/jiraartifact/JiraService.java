package impactassessment.jiraartifact;

import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.JiraInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JiraService implements IJiraArtifactService {

    private JiraInstance jira;
    private JiraChangeSubscriber jiraChangeSubscriber;


    public JiraService(JiraInstance jira, JiraChangeSubscriber jiraChangeSubscriber) {
        this.jira = jira;
        this.jiraChangeSubscriber = jiraChangeSubscriber;
    }

    @Override
    public IJiraArtifact get(String artifactKey, String workflowId) {
        log.debug("JiraService loads "+artifactKey);
        IssueAgent issueAgent = jira.fetchAndMonitor(artifactKey);
        if (issueAgent == null) {
            log.debug("Not able to fetch Jira Issue");
            return null;
        } else  {
            log.debug("Successfully fetched Jira Issue");
            jiraChangeSubscriber.addUsage(workflowId, artifactKey);
            return new JiraArtifact(issueAgent.getIssue());
        }
    }

}
