package impactassessment.artifactconnector;

import passiveprocessengine.instance.ArtifactWrapper;

public interface IArtifactRegistry {

    ArtifactWrapper get(ArtifactIdentifier id, String workflowId);

    void register(IArtifactService service);
}
