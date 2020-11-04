package impactassessment.jiraartifact.subinterfaces;

import com.atlassian.jira.rest.client.api.domain.IssueLinkType;

import java.net.URI;

public interface IJiraIssueLink {

    String getTargetIssueKey();

    URI getTargetIssueUri();

    IJiraIssueLinkType getIssueLinkType();

}
