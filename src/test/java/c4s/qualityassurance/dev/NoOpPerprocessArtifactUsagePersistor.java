package c4s.qualityassurance.dev;

import java.util.Collections;
import java.util.Set;

import artifactapi.ArtifactIdentifier;
import impactassessment.artifactconnector.usage.PerProcessArtifactUsagePersistor;

public class NoOpPerprocessArtifactUsagePersistor implements PerProcessArtifactUsagePersistor {

	@Override
	public void addUsage(String projectScopeId, ArtifactIdentifier ai) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<ArtifactIdentifier> getUsages(String projectScopeId) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public void removeScope(String projectScopeId) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<String> getAllScopeIdentifier() {
		return Collections.emptySet();
	}

}
