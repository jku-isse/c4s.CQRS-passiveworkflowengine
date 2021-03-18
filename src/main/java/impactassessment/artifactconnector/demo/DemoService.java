package impactassessment.artifactconnector.demo;

import java.util.Optional;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;

public class DemoService implements IArtifactService{

	//public static final String
	
	@Override
	public boolean provides(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void injectArtifactService(IArtifact artifact, String workflowId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteDataScope(String scopeId) {
		// TODO Auto-generated method stub
		
	}

}
