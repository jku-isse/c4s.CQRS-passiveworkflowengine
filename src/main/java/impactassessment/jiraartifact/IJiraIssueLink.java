package impactassessment.jiraartifact;

import com.atlassian.jira.rest.client.api.domain.IssueLinkType;

import java.net.URI;

public interface IJiraIssueLink {

    String getTargetIssueKey();

    URI getTargetIssueUri();

    IssueLinkType getIssueLinkType();

}
