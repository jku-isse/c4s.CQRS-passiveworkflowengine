package at.jku.isse.passiveprocessengine.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.designspace.artifactconnector.core.repository.DesignspaceArtifactRepository;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.IArtifactProvider;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.ErrorResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.SuccessResponse;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;

public class DemoArtifactProvider extends DesignspaceArtifactRepository implements IArtifactProvider {


	private final PPEInstanceType typeJira;
	
	public DemoArtifactProvider(SchemaRegistry schemaReg, InstanceRepository ws, TestArtifacts artifactFactory ) {
		super(artifactFactory.getJiraInstanceType(), schemaReg.getTypeByName(CoreTypeFactory.BASE_TYPE_NAME) , ws);
		typeJira = artifactFactory.getJiraInstanceType();

	}
	
	@Override
	public Map<PPEInstanceType, List<String>> getSupportedIdentifiers() {
		return Map.of(getDefaultArtifactInstanceType(), List.of(typeJira.getName()));
	}
	
	@Override
	public PPEInstanceType getDefaultArtifactInstanceType() {
		return typeJira;
	}

	@Override
	public Set<PPEInstanceType> getProvidedArtifactInstanceTypes() {
		return Set.of(typeJira);				
	}

	@Override
	public Set<FetchResponse> fetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return artifactIdentifiers.stream()
			.map(id -> {
				Optional<PPEInstance> optInst = Optional.ofNullable(super.getInstanceByExternalDefaultId(id.getId()));
				if (optInst.isEmpty()) {
					return new ErrorResponse("No DemoIssue found for id: "+Objects.toString(id));
				} else {
					return new SuccessResponse(optInst.get());
				}
			})
			.collect(Collectors.toSet());
	}

	@Override
	public Set<FetchResponse> forceFetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return fetchArtifact(artifactIdentifiers);
	}
	
}
