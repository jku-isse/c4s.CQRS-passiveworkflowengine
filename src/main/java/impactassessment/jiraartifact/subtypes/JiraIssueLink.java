package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueLink;
import impactassessment.SpringUtil;
import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueLink;
import impactassessment.jiraartifact.subinterfaces.IJiraIssueLinkType;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class JiraIssueLink implements IJiraIssueLink {

    private IssueLink issueLink;
    private IJiraIssueLinkType jiraIssueLinkType;

    private volatile IJiraArtifactService jiraArtifactService = null;

    public JiraIssueLink(IssueLink issueLink) {
        this.issueLink = issueLink;
        this.jiraIssueLinkType = new JiraIssueLinkType(issueLink.getIssueLinkType());
    }

    @Override
    public String getTargetIssueKey() {
        return issueLink.getTargetIssueKey();
    }

    @Override
    public URI getTargetIssueUri() {
        return issueLink.getTargetIssueUri();
    }

    @Override
    public IJiraIssueLinkType getIssueLinkType() {
        return jiraIssueLinkType;
    }

    /**
     * New method to directly fetch the target issue
     * @return the target issue
     */
    @Override
    public IJiraArtifact getTargetIssue(String aggregateId, String corrId) {
        log.info("Artifact fetching linked issue: {}", getTargetIssueKey());
        if (jiraArtifactService == null)
            jiraArtifactService = SpringUtil.getBean(IJiraArtifactService.class);
        return jiraArtifactService.get(issueLink.getTargetIssueKey(), aggregateId);
    }

    @Override
    public String toString() {
        return "JiraIssueLink{" +
                "issueLink=" + issueLink +
                ", jiraIssueLinkType=" + jiraIssueLinkType +
                ", jiraArtifactService=" + jiraArtifactService +
                '}';
    }
}
