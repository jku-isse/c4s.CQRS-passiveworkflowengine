package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.IArtifactService;
import at.jku.isse.designspace.artifactconnector.core.IService;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.endpoints.grpc.service.IResponder;
import at.jku.isse.designspace.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArtifactResolver {

	private Set<IArtifactProvider> connectors = new HashSet<>();

	private Workspace ws;
	
	public ArtifactResolver() {
		
	}
	
	public Instance get(ArtifactIdentifier artId) throws ProcessException {
		Optional<IArtifactProvider> optConn = connectors.stream()
			.filter(conn1 -> conn1.isProviding(artId.getType()))
			.findAny();
		if (optConn.isPresent()) {
			ServiceResponse resp = optConn.get().getServiceResponse(artId.getId(), artId.getType());
			if (resp.getKind() == ServiceResponse.SUCCESS) {
				ws.update();
				Element el = ws.findElement(Id.of(Long.parseLong(resp.getInstanceId())));
				if (el == null) {
					String msg = String.format("Able to resolve artifact %s %s but unable to find element by id %s in process engine workspace", artId.getId(), artId.getType(), resp.getInstanceId());
					log.error(msg);
					throw new ProcessException(msg);
				} else if (el instanceof Instance) {
					return (Instance)el;
				} else {
					String msg = String.format("Able to resolve artifact %s %s but not of 'instance' type", artId.getId(), artId.getType());
					log.error(msg);
					throw new ProcessException(msg);
				}
			} else {
				throw new ProcessException(resp.getMsg());
			}
		} else {
			String msg = String.format("No service registered that provides artifacts of type %s", artId.getType());
			log.error(msg);
	        throw new ProcessException(msg);
		}
	}

	public void register(IArtifactProvider connector) {
		assert(connector != null);
		connectors.add(connector);
	}

	
	public void inject(Workspace ws) {
		this.ws = ws;
	}

}
