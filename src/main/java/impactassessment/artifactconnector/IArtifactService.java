package impactassessment.artifactconnector;

public interface IArtifactService {

    boolean provides(String type);

    IArtifact get(ArtifactIdentifier id, String workflowId);
}
