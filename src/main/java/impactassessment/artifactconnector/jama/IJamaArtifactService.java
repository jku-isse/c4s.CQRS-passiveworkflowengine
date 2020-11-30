package impactassessment.artifactconnector.jama;

public interface IJamaArtifactService {

    IJamaArtifact get(String artifactKey, String workflowId);
}
