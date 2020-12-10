package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;
import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.JiraInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
        String artifactKey = id.getId();
        log.debug("JiraService loads "+artifactKey);
        IssueAgent issueAgent = jira.fetchAndMonitor(artifactKey);
        if (issueAgent == null) {
            log.debug("Not able to fetch Jira Issue");
            return Optional.empty();
        } else  {
            log.debug("Successfully fetched Jira Issue");
            jiraChangeSubscriber.addUsage(workflowId, artifactKey);
            IArtifact artifact = new JiraArtifact(issueAgent.getIssue());
            return Optional.of(artifact);
        }
    }

}
