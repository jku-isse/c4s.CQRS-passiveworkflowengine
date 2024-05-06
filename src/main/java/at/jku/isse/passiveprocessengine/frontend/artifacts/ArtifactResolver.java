package at.jku.isse.passiveprocessengine.frontend.artifacts;

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
import org.springframework.context.event.EventListener;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.ErrorResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.SuccessResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.IArtifactProvider;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArtifactResolver {

	private Set<IArtifactProvider> connectors = new HashSet<>();

	private Map<PPEInstanceType, List<String>> identifierTypes = new HashMap<>();

	public ArtifactResolver() {

	}

	public List<String> getIdentifierTypesForInstanceType(PPEInstanceType type) {
		List<String> types = identifierTypes.getOrDefault(type, Collections.emptyList()); 
		if (types.isEmpty()) {
			if (type.getParentType()== null)
				return types; // an empty list;
			else { // else check if we can resolve first super type
				return getIdentifierTypesForInstanceType(type.getParentType());
			}
		} else 
			return types; 
	}

	public Set<PPEInstanceType> getAvailableInstanceTypes() {
		return identifierTypes.keySet();
	}

	public Set<PPEInstance> get(Set<ArtifactIdentifier> artIds, String idType) {
		Optional<IArtifactProvider> optConn = connectors.stream()
				.filter(conn1 -> conn1.getSupportedIdentifiers()
						.values()
						.stream()
						.flatMap(ids -> ids.stream())
						.anyMatch(str ->  str.equalsIgnoreCase(idType)))
				.findAny();
		if (optConn.isPresent()) {
			Set<FetchResponse> responses = optConn.get().forceFetchArtifact(artIds);

			return responses.stream()
					.map(resp -> {
						if (resp instanceof ErrorResponse) {
							String msg = String.format("Toolconnector failed to provide response for artifact %s ", ((ErrorResponse) resp).getErrormsg());
							log.error(msg);
							return null;
						} else if (resp instanceof SuccessResponse) {
							return ((SuccessResponse) resp).getInstance();
						} else {
							log.error("Unknown Response class: "+Objects.toString(resp));
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		} else {
			String msg = String.format("No service registered that provides artifacts of type %s", idType);
			log.error(msg);
			return Collections.emptySet();
		}
	}

	public PPEInstance get(ArtifactIdentifier artId) throws ProcessException {
		return get(artId, false);
	}


	public PPEInstance get(ArtifactIdentifier artId, boolean forceFetch) throws ProcessException {
		Optional<IArtifactProvider> optConn = connectors.stream()
				.filter(conn1 -> conn1.getSupportedIdentifiers()
						.values()
						.stream()
						.flatMap(ids -> ids.stream())
						.anyMatch(str ->  str.equalsIgnoreCase(artId.getIdType())))
				.findAny();
		if (optConn.isPresent()) {
			FetchResponse resp = optConn.get().forceFetchArtifact(Set.of(artId)).stream().findAny().orElse(null);
			if (resp != null) {
				if (resp instanceof SuccessResponse) {
					PPEInstance inst =  ((SuccessResponse) resp).getInstance();
					if (inst == null) {
						String msg = String.format("SuccessResponse but null Instance for artifact %s %s ", artId.getId(), artId.getType());
						log.error(msg);
						throw new ProcessException(msg);
					} else {
						return inst;
					}
				} else {
					throw new ProcessException( ((ErrorResponse) resp).getErrormsg() );
				}}
			else {
				String msg = String.format("Toolconnector failed to provide response for artifact %s %s ", artId.getId(), artId.getType());
				log.error(msg);
				throw new ProcessException(msg);
			}
		} else {
			String msg = String.format("No tool connector registered that provides artifacts of type %s", artId.getType());
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
		connectors.stream().forEach(connector -> identifierTypes.putAll(connector.getSupportedIdentifiers()));
	}


}
