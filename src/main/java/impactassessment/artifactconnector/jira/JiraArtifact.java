package impactassessment.artifactconnector.jira;

import artifactapi.*;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.*;

import com.atlassian.jira.rest.client.api.domain.*;
import impactassessment.artifactconnector.jira.subtypes.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
public class JiraArtifact implements IJiraArtifact {

    private ArtifactIdentifier artifactIdentifier;

    private Issue issue;
    private IJiraStatus status;
    private IJiraUser reporter;
    private IJiraUser assignee;
    private IJiraBasicPriority basicPriority;
    private IJiraIssueType issueType;
    private IJiraBasicProject basicProject;
    private IJiraBasicVotes basicVotes;
    private List<IJiraIssueLink> issueLinks = new ArrayList<>();
    private List<IJiraSubtask> subTasks = new ArrayList<>();
    private List<IJiraIssueField> issueFields = new ArrayList<>();
    private List<IJiraVersion> versions = new ArrayList<>();

   
    
    public JiraArtifact(Issue issue, IJiraService service) {
        this.artifactIdentifier = new ArtifactIdentifier(issue.getKey(), IJiraArtifact.class.getSimpleName());

        this.issue = issue;
        this.status = new JiraStatus(issue.getStatus());
        this.reporter = issue.getReporter() != null ? new JiraUser(issue.getReporter()) : null;
        this.assignee = issue.getAssignee() != null ? new JiraUser(issue.getAssignee()) : null;
        this.basicPriority = new JiraBasicPriority(issue.getPriority());
        this.issueType = new JiraIssueType(issue.getIssueType());
        this.basicProject = new JiraBasicProject(issue.getProject());
        this.basicVotes = new JiraBasicVotes(issue.getVotes());
        if (issue.getIssueLinks() != null) // null-check necessary because field is marked as @Nullable by atlassian
            for (IssueLink il : issue.getIssueLinks()) {
                issueLinks.add(new JiraIssueLink(il, service));
            }
        if (issue.getSubtasks() != null)
            for (Subtask t : issue.getSubtasks()) {
                subTasks.add(new JiraSubtask(t, service));
            }
        if (issue.getFields() != null)
            for (IssueField t : issue.getFields()) {
                issueFields.add(new JiraIssueField(t));
            }
        if (issue.getFixVersions() != null)
            for (Version v : issue.getFixVersions()) {
                versions.add(new JiraVersion(v));
            }
    }

    @Override
    public ArtifactIdentifier getArtifactIdentifier() {
        return artifactIdentifier;
    }

    @Override
    public ResourceLink convertToResourceLink() {
        String context = getSummary();
        String href = getBrowserLink().toString();
        String rel = "self";
        String as = getIssueType().getName();
        String linkType = "html";
        String title = getKey() +" "+getSummary();
        return new ResourceLink(context, href, rel, as, linkType, title);
    }

    @Override
    public void injectArtifactService(IArtifactService service) {
        if (service instanceof IJiraService) {
            for (IJiraIssueLink l : issueLinks) {
                l.injectArtifactService(service);
            }
            for (IJiraSubtask t : subTasks) {
                t.injectArtifactService(service);
            }
        } else {
            log.warn("Injection of {} into JiraArtifact not possible.", service.getClass().getSimpleName());
        }
    }

    @Override
    public IArtifact getParentArtifact() {
        // TODO auto-generated
        return null;
    }

    @Override
    public void setRemovedAtOriginFlag() {
        // TODO auto-generated
    }

    @Override
    public boolean isRemovedAtOrigin() {
        // TODO auto-generated
        return false;
    }

    public Issue getIssue() {
        return issue;
    }

    @Override
    public URI getSelf() {
        return issue.getSelf();
    }

    @Override
    public URI getBrowserLink() {
        try {
            //Example: https://passiveprocessengine.atlassian.net/jira/software/c/projects/DEMO/issues/DEMO-8
            //return new URI("https://"+getSelf().getHost()+"/jira/software/c/projects/"+getProject().getKey()+"/issues/"+getKey());
        	return new URI(getHumanReadableResourceLinkEndpoint());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String getHumanReadableResourceLinkEndpoint() {
		
		URI uri = getSelf();
		String port = uri.getPort() == -1 ? "" : ":"+uri.getPort()+"";
		String href = uri.getScheme()+"://"+uri.getHost()+port+"/browse/"+getKey();
		return href;
	}

    @Override
    public String getKey() {
        return issue.getKey();
    }

    @Override
    public String getId() {
        return issue.getId().toString();
    }

    @Override
    public IJiraStatus getStatus() {
        return status;
    }

    @Override
    public IJiraUser getReporter() {
        return reporter;
    }

    @Override
    public IJiraUser getAssignee() {
        return assignee;
    }

    @Override
    public String getSummary() {
        return issue.getSummary();
    }

    @Override
    public IJiraBasicPriority getPriority() {
        return basicPriority;
    }

    @Override
    public List<IJiraIssueLink> getIssueLinks() {
        return issueLinks;
    }

    @Override
    public List<IJiraSubtask> getSubtasks() {
        return subTasks;
    }

    @Override
    public List<IJiraIssueField> getFields() {
        return issueFields;
    }

    @Override
    public IJiraIssueField getField(String id) {
        for (IJiraIssueField issueField : issueFields) {
            if (issueField.getId().equals(id)) {
                return issueField;
            }
        }
        return null;
    }

    @Override
    public IJiraIssueField getFieldByName(String name) {
        for (IJiraIssueField issueField : issueFields) {
            if (issueField.getName().equals(name)) {
                return issueField;
            }
        }
        return null;
    }

    @Override
    public IJiraIssueType getIssueType() {
        return issueType;
    }

    @Override
    public IJiraBasicProject getProject() {
       return basicProject;
    }

    @Override
    public IJiraBasicVotes getVotes() {
        return basicVotes;
    }

    @Override
    public List<IJiraVersion> getFixVersions() {
        return versions;
    }

    @Override
    public Date getCreationDate() {
        return issue.getCreationDate() != null ? issue.getCreationDate().toDate() : null;
    }

    @Override
    public Date getUpdateDate() {
        return issue.getUpdateDate() != null ? issue.getUpdateDate().toDate() : null;
    }

    @Override
    public Date getDueDate() {
        return issue.getDueDate() != null ? issue.getDueDate().toDate() : null;
    }

    @Override
    public String getDescription() {
        return issue.getDescription();
    }

    @Override
    public String toString() {
        return "JiraArtifact [summary=" + getSummary() + ", description=" + getDescription() + ", self=" + getSelf() + ", key=" + getKey()
                + ", id=" + getId() + ", project=" + getProject() + ", issueType=" + getIssueType() + ", status=" + getStatus()
                + ", priority=" + getPriority() + ", reporter=" + getReporter() + ", assignee=" + getAssignee() + ", creationDate=" + getCreationDate()
                + ", updateDate=" + getUpdateDate() + ", dueDate=" + getDueDate() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JiraArtifact that = (JiraArtifact) o;
        return artifactIdentifier.equals(that.artifactIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactIdentifier);
    }
}
