package impactassessment.artifactconnector;

public interface IArtifactRegistry {

    IArtifact get(ArtifactIdentifier id, String workflowId);

    void register(IArtifactService service);
}
