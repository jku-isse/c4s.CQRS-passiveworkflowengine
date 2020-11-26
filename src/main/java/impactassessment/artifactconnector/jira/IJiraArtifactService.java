package impactassessment.artifactconnector.jira;

public interface IJiraArtifactService {

    IJiraArtifact get(String artifactKey, String workflowId);

}
