package impactassessment.artifactconnector.jira;

import artifactapi.ArtifactIdentifier;
import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.*;
import com.atlassian.jira.rest.client.api.domain.*;
import impactassessment.artifactconnector.jira.subtypes.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
        this.reporter = new JiraUser(issue.getReporter());
        this.assignee = new JiraUser(issue.getAssignee());
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
            return new URI("https://"+getSelf().getHost()+"/jira/software/c/projects/"+getProject().getKey()+"/issues/"+getKey());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
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
    public Iterable<IJiraIssueLink> getIssueLinks() {
        return issueLinks;
    }

    @Override
    public Iterable<IJiraSubtask> getSubtasks() {
        return subTasks;
    }

    @Override
    public Iterable<IJiraIssueField> getFields() {
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
    public Iterable<IJiraVersion> getFixVersions() {
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
}
