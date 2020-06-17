package impactassessment.artifact.base;

import com.atlassian.jira.rest.client.api.domain.IssueLinkType;

import java.net.URI;

public interface IIssueLink {

    String getTargetIssueKey();

    URI getTargetIssueUri();

    IssueLinkType getIssueLinkType();

}
