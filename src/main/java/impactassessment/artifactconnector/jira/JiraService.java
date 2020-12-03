package impactassessment.artifactconnector.jira;

import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.JiraInstance;
import impactassessment.artifactconnector.ArtifactIdentifier;
import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.IArtifactService;
import impactassessment.artifactconnector.jama.IJamaArtifact;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JiraService implements IArtifactService {

    private static final String TYPE = IJiraArtifact.class.getSimpleName();

    private JiraInstance jira;
    private JiraChangeSubscriber jiraChangeSubscriber;

    public JiraService(JiraInstance jira, JiraChangeSubscriber jiraChangeSubscriber) {
        this.jira = jira;
        this.jiraChangeSubscriber = jiraChangeSubscriber;
    }

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    @Override
    public IArtifact get(ArtifactIdentifier id, String workflowId) {
        return get(id.getId(), workflowId);
    }

    private IJiraArtifact get(String artifactKey, String workflowId) {
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
