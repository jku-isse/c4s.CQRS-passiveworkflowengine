package at.jku.isse.passiveprocessengine.frontend.artifacts;

import at.jku.isse.designspace.artifactconnector.core.endpoints.grpc.service.IResponder;

public interface IArtifactProvider extends IResponder{

	public boolean isProviding(String artifactType);
	
}
