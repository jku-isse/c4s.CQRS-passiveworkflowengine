package impactassessment.artifactconnector.demo;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.ResourceLink;

public abstract class DemoArtifact implements IArtifact {

	protected String id;
	protected ArtifactIdentifier ai;
	
	@Override
	public ResourceLink convertToResourceLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void injectArtifactService(IArtifactService service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IArtifact getParentArtifact() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRemovedAtOriginFlag() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRemovedAtOrigin() {
		// TODO Auto-generated method stub
		return false;
	}

}
