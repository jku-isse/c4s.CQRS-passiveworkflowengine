package at.jku.isse.passiveprocessengine.frontend.artifacts;

import at.jku.isse.designspace.endpoints.grpc.service.IResponder;

public interface IArtifactProvider extends IResponder{

	public boolean isProviding(String artifactType);
	
}
