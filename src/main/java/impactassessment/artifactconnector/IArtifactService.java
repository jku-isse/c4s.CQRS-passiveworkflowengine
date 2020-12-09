package impactassessment.artifactconnector;

import java.util.Optional;

public interface IArtifactService {

    boolean provides(String type);

    Optional<IArtifact> get(ArtifactIdentifier id, String workflowId);
}
