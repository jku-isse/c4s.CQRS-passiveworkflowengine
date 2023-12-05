package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import at.jku.isse.designspace.artifactconnector.core.IArtifactProvider;
import at.jku.isse.designspace.artifactconnector.core.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;

@Component
public class ProcessConfigProvider implements IArtifactProvider {

	private ProcessConfigBaseElementFactory configFactory;
	private Workspace ws;
	
	public ProcessConfigProvider(ProcessConfigBaseElementFactory configFactory, Workspace ws) {
		this.configFactory = configFactory;
		this.ws = ws;
	}
	
	@Override
	public ServiceResponse getServiceResponse(String id, String identifierType) {
		try {
			Element el = ws.findElement(Id.of(Long.parseLong(id)));
			if (el != null) {
				return new ServiceResponse(0, "ProcessConfigProvider", "Found", id);
			} else 
				return new ServiceResponse(3, "ProcessConfigProvider", "Not found", id);
		} catch(Exception e) {
			return new ServiceResponse(1, "ProcessConfigProvider", "Invalid id format", id);
		}
	}

	@Override
	public ServiceResponse getServiceResponse(String id, String identifierType, boolean doForceRefetch) {
		return getServiceResponse(id, identifierType);
	}

	@Override
	public ServiceResponse[] getServiceResponse(Set<String> ids, String identifierType) {
		return ids.stream().map(id -> getServiceResponse(id, identifierType)).collect(Collectors.toSet()).toArray(new ServiceResponse[0]);		
	}

	@Override
	public ServiceResponse[] getServiceResponse(Set<String> ids, String identifierType, boolean doForceRefetch) {
		return getServiceResponse(ids, identifierType);
	}

	@Override
	public InstanceType getArtifactInstanceType() {
		return configFactory.getBaseType();
	}

	@Override
	public Set<InstanceType> getArtifactInstanceTypes() {
		return Set.of(configFactory.getBaseType());
	}

	@Override
	public Map<InstanceType, List<String>> getSupportedIdentifier() {
		return Map.of(configFactory.getBaseType(), List.of("DSid"));				
	}
	
}
