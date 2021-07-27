package impactassessment.artifactconnector.usage;

import java.util.Set;

import artifactapi.ArtifactIdentifier;

public interface PerProcessArtifactUsagePersistor {

	public void addUsage(String projectScopeId, ArtifactIdentifier ai);
	
	public Set<ArtifactIdentifier> getUsages(String projectScopeId);
	
	public void removeScope(String projectScopeId);
	
	public Set<String> getAllScopeIdentifier();
	
}
