package impactassessment.artifactconnector.jira;

import java.util.Optional;

import artifactapi.jira.IJiraArtifact;

public interface IJiraService {

	
	public Optional<IJiraArtifact> getIssue(String id, String workflow);
	
	public Optional<IJiraArtifact> getIssue(String key);
}
