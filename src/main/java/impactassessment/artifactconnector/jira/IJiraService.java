package impactassessment.artifactconnector.jira;

import java.util.Optional;

import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;

public interface IJiraService extends IArtifactService {

	
	public Optional<IJiraArtifact> getIssue(String id, String workflow);
	
	public Optional<IJiraArtifact> getIssue(String key);
}
