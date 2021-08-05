package impactassessment.artifactconnector.demo;

import java.util.HashMap;
import java.util.Optional;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;

public class DemoService implements IArtifactService{

	//public static final String
	
	protected HashMap<String, DemoArtifact> artifacts = new HashMap<String, DemoArtifact>();
	
	@Override
	public boolean provides(String type) {
		return (type.equalsIgnoreCase(DemoIssue.type.toString()) || 
				type.equalsIgnoreCase(DemoRequirement.type.toString()) ||
				type.equalsIgnoreCase(DemoTestCase.type.toString()) );
	}

	@Override
	public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
		return Optional.ofNullable(artifacts.get(id.getId())); //workflow is ignored for demo
	}
	
	public DemoArtifact get(String id) {
		return artifacts.get(id);
	}

	@Override
	public void injectArtifactService(IArtifact artifact, String workflowId) {
		artifact.injectArtifactService(this);
	}

	@Override
	public void deleteDataScope(String scopeId) {
		// remove all entries
		artifacts.clear();
	}
	
	public void addArtifact(DemoArtifact art) {
		artifacts.put(art.id, art);
	}
	

}
