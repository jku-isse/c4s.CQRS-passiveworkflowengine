package impactassessment.artifactconnector.jira.subtypes;

import com.atlassian.jira.rest.client.api.domain.IssueLink;
import impactassessment.SpringUtil;
import impactassessment.artifactconnector.jira.IJiraArtifact;
import impactassessment.artifactconnector.jira.IJiraArtifactService;
import impactassessment.artifactconnector.jira.subinterfaces.IJiraIssueLink;
import impactassessment.artifactconnector.jira.subinterfaces.IJiraIssueLinkType;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class JiraIssueLink implements IJiraIssueLink {

    private IssueLink issueLink;
    private IJiraIssueLinkType jiraIssueLinkType;

    private transient IJiraArtifactService jiraArtifactService = null;

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
