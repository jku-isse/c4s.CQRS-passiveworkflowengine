package impactassessment.artifactconnector;

import java.util.Optional;

public interface IArtifactRegistry {

    Optional<IArtifact> get(ArtifactIdentifier id, String workflowId);

    void register(IArtifactService service);
}
