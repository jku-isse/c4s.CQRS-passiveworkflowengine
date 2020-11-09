package impactassessment.jiraartifact;

public interface IJiraArtifactService {

    IJiraArtifact get(String artifactKey, String workflowId);

}
