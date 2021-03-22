package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.IArtifactService;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.IJiraIssueLink;
import artifactapi.jira.subtypes.IJiraIssueLinkType;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import impactassessment.artifactconnector.jira.IJiraService;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

@Slf4j
public class JiraIssueLink implements IJiraIssueLink {

    private IssueLink issueLink;
    private IJiraIssueLinkType jiraIssueLinkType;
    
    private transient IJiraService jiraService = null;

    public JiraIssueLink(IssueLink issueLink, IJiraService service) {
    	this.jiraService = service;
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
    public Optional<IJiraArtifact> getTargetIssue() {
        log.debug("Artifact fetching linked issue: {}", getTargetIssueKey());
        return  jiraService.getIssue(issueLink.getTargetIssueKey());
    }

    @Override
    public void injectArtifactService(IArtifactService iArtifactService) {
        if (jiraService != null) {
            if (iArtifactService instanceof IJiraService) // will be always the case
                jiraService = (IJiraService) iArtifactService;
        }
    }

    @Override
    public String toString() {
        return "JiraIssueLink{" +
                "issueLink=" + issueLink +
                ", jiraIssueLinkType=" + jiraIssueLinkType +
                ", jiraArtifactService=" + jiraService +
                '}';
    }
}
