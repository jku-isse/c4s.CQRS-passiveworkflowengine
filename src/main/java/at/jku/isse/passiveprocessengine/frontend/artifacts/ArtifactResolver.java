package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

import at.jku.isse.designspace.artifactconnector.core.IArtifactProvider;
import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArtifactResolver {

	private Set<IArtifactProvider> connectors = new HashSet<>();

	private Workspace ws;
	private Map<InstanceType, List<String>> identifierTypes = new HashMap<>();
	
	public ArtifactResolver() {
		
	}
	
	public List<String> getIdentifierTypesForInstanceType(InstanceType type) {
		List<String> types = identifierTypes.getOrDefault(type, Collections.emptyList()); 
		if (types.isEmpty()) {
			if (type.superTypes().isEmpty())
				return types; // an empty list;
			else { // else check if we can resolve first super type
					return getIdentifierTypesForInstanceType(type.superTypes().iterator().next());
				}
		} else 
		return types; 
	}
	
	public Set<InstanceType> getAvailableInstanceTypes() {
		return identifierTypes.keySet();
	}
	
	public Set<Instance> get(Set<String> artIds, String idType) {
		Optional<IArtifactProvider> optConn = connectors.stream()
				.filter(conn1 -> conn1.getSupportedIdentifier()
										.values()
										.stream()
										.flatMap(ids -> ids.stream())
										.anyMatch(str ->  str.equalsIgnoreCase(idType)))
				.findAny();
		if (optConn.isPresent()) {
			ServiceResponse[] resps = optConn.get().getServiceResponse(artIds, idType);
			return Arrays.asList(resps).stream()
			.filter(resp -> resp.getKind() == ServiceResponse.SUCCESS)
			.map(resp -> {															
				Element el = ws.findElement(Id.of(Long.parseLong(resp.getInstanceId())));				
				return (Instance)el;				
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		} else {
			String msg = String.format("No service registered that provides artifacts of type %s", idType);
			log.error(msg);
			return Collections.emptySet();
		}
	}
	
	public Instance get(ArtifactIdentifier artId) throws ProcessException {
		Optional<IArtifactProvider> optConn = connectors.stream()
			.filter(conn1 -> conn1.getSupportedIdentifier()
									.values()
									.stream()
									.flatMap(ids -> ids.stream())
									.anyMatch(str ->  str.equalsIgnoreCase(artId.getIdType())))
			.findAny();
		if (optConn.isPresent()) {
			ServiceResponse resp = optConn.get().getServiceResponse(artId.getId(), artId.getIdType());
			if (resp.getKind() == ServiceResponse.SUCCESS) {
				//ws.update();
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

	@EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
		connectors.stream().forEach(connector -> identifierTypes.putAll(connector.getSupportedIdentifier()));
	}
	
	public void inject(Workspace ws) {
		this.ws = ws;
	}

}
