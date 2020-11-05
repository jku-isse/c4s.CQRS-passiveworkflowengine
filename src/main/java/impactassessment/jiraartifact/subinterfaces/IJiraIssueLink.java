package impactassessment.jiraartifact.subinterfaces;

import com.atlassian.jira.rest.client.api.domain.IssueLinkType;
import impactassessment.jiraartifact.IJiraArtifact;

import java.net.URI;

public interface IJiraIssueLink {

    String getTargetIssueKey();

    URI getTargetIssueUri();

    IJiraIssueLinkType getIssueLinkType();

    /**
     * New method to directly fetch the target issue
     * @return the target issue
     */
    IJiraArtifact getTargetIssue(String aggregateId, String corrId);
}
