package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.IJiraIssueLink;
import artifactapi.jira.subtypes.IJiraIssueLinkType;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import impactassessment.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

@Slf4j
public class JiraIssueLink implements IJiraIssueLink {

    private IssueLink issueLink;
    private IJiraIssueLinkType jiraIssueLinkType;

    private transient IArtifactRegistry artifactRegistry = null;

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
    public Optional<IJiraArtifact> getTargetIssue(String aggregateId, String corrId) {
        log.info("Artifact fetching linked issue: {}", getTargetIssueKey());
        if (artifactRegistry == null)
            artifactRegistry = SpringUtil.getBean(IArtifactRegistry.class);
        ArtifactIdentifier ai = new ArtifactIdentifier(issueLink.getTargetIssueKey(), IJiraArtifact.class.getSimpleName());
        Optional<IArtifact> a = artifactRegistry.get(ai, aggregateId);
        return a.map(artifact -> (IJiraArtifact)artifact);
    }

    @Override
    public String toString() {
        return "JiraIssueLink{" +
                "issueLink=" + issueLink +
                ", jiraIssueLinkType=" + jiraIssueLinkType +
                ", jiraArtifactService=" + artifactRegistry +
                '}';
    }
}
