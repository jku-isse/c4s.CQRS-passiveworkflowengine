package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.ResourceLink;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.IJiraIssueType;
import artifactapi.jira.subtypes.IJiraStatus;
import artifactapi.jira.subtypes.IJiraSubtask;
import impactassessment.artifactconnector.jira.IJiraService;
import com.atlassian.jira.rest.client.api.domain.Subtask;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class JiraSubtask implements IJiraSubtask {

	private ArtifactIdentifier artifactIdentifier;
	
    private Subtask task;
    private IJiraStatus status;
    private IJiraIssueType issueType;
    private transient IJiraService jiraService = null;

    public JiraSubtask(Subtask task, IJiraService service) {
    	this.artifactIdentifier = new ArtifactIdentifier(task.getIssueKey(), IJiraArtifact.class.getSimpleName());
    	this.jiraService = service;
        this.task = task;
        this.status = new JiraStatus(task.getStatus());
        this.issueType = new JiraIssueType(task.getIssueType());
    }
    
    @Override
    public ArtifactIdentifier getArtifactIdentifier() {
        return artifactIdentifier;
    }

    @Override
    public ResourceLink convertToResourceLink() {
        String context = getSummary();
        URI uri = task.getIssueUri();
		String port = uri.getPort() == -1 ? "" : ":"+uri.getPort()+"";
		String href = uri.getScheme()+"://"+uri.getHost()+port+"/browse/"+getIssueKey();
        String rel = "self";
        String as = getIssueType().getName();
        String linkType = "html";
        String title = getIssueKey() +" "+getSummary();
        return new ResourceLink(context, href, rel, as, linkType, title);
    }

    @Override
    public String getIssueKey() {
        return task.getIssueKey();
    }

    @Override
    public URI getIssueUri() {
        return task.getIssueUri();
    }

    @Override
    public String getSummary() {
        return task.getSummary();
    }

    @Override
    public IJiraIssueType getIssueType() {
        return issueType;
    }

    @Override
    public IJiraStatus getStatus() {
        return status;
    }

    @Override
    public void injectArtifactService(IArtifactService iArtifactService) {
        if (iArtifactService instanceof IJiraService) // will be always the case
            jiraService = (IJiraService) iArtifactService;
    }

    @Override
    public Optional<IJiraArtifact> getSubtask() {
    	return  jiraService.getIssue(task.getIssueKey());
    }

	@Override
	public IArtifact getParentArtifact() {
		return null;
	}

	@Override
	public void setRemovedAtOriginFlag() {
	}

	@Override
	public boolean isRemovedAtOrigin() {
		return false;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JiraSubtask that = (JiraSubtask) o;
        return artifactIdentifier.equals(that.artifactIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactIdentifier);
    }
	
}
