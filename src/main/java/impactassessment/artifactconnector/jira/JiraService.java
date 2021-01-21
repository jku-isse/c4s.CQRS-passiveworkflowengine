package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jira.IJiraArtifact;
import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.JiraInstance;
import impactassessment.artifactconnector.jama.IJamaService;
import impactassessment.artifactconnector.jama.JamaDataScope;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class JiraService implements IJiraService, IArtifactService {

    private static final String TYPE = IJiraArtifact.class.getSimpleName();

    private JiraInstance jira;
    private JiraChangeSubscriber jiraChangeSubscriber;
    private HashMap<String, JiraDataScope> perProcessCaches = new HashMap<String, JiraDataScope>();

    
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
    	if (workflowId != null) {
    		//TODO: some method to purge scope entries when workflow is removed
    		IJiraService scope = perProcessCaches.computeIfAbsent(workflowId, k -> new JiraDataScope(k, this));
    		Optional<IJiraArtifact> opt = scope.getIssue(id.getId()); // no need to pass the workflow, as scope has that id;
    		return  opt.map(jArt -> (IArtifact)jArt);
    	}
    	else { // 
    		Optional<IJiraArtifact> opt = getIssue(id.getId()); // local passthrough to backend cache without change tracking
    		return  opt.map(jArt -> (IArtifact)jArt);
    	}
    }

	@Override
	public Optional<IJiraArtifact> getIssue(String key, String workflowId) {
		log.debug("JiraService loads "+key);
        IssueAgent issueAgent = jira.fetchAndMonitor(key);
        if (issueAgent == null) {
            log.debug("Not able to fetch Jira Issue");
            return Optional.empty();
        } else  {
            log.debug("Successfully fetched Jira Issue");
            IJiraService scope = perProcessCaches.get(workflowId);
            if (scope == null) scope = this;
            IJiraArtifact artifact = new JiraArtifact(issueAgent.getIssue(), scope);
            if (artifact != null) {
            	jiraChangeSubscriber.addUsage(perProcessCaches.get(workflowId), new ArtifactIdentifier(key, IJiraArtifact.class.getSimpleName()));
            	return Optional.of(artifact);
            } else
            	return Optional.empty();
        }
	}

	@Override // simple passthrough to backend, no caching, no updating
	public Optional<IJiraArtifact> getIssue(String key) {
		log.debug("JiraService loads "+key);
        IssueAgent issueAgent = jira.fetchAndMonitor(key);
        if (issueAgent == null) {
            log.debug("Not able to fetch Jira Issue");
            return Optional.empty();
        } else  {
            log.debug("Successfully fetched Jira Issue");
            IJiraArtifact artifact = new JiraArtifact(issueAgent.getIssue(), this);
            return Optional.of(artifact);
        }
	}

}
