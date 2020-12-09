package impactassessment.artifactconnector.jira;

import impactassessment.artifactconnector.IArtifact;
import impactassessment.artifactconnector.jira.subinterfaces.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Date;

public interface IJiraArtifact extends IArtifact {

    URI getSelf();

    URI getBrowserLink();

    String getKey();

    String getId();

    IJiraStatus getStatus();

    IJiraUser getReporter();

    IJiraUser getAssignee();

    String getSummary();

    IJiraBasicPriority getPriority();

    Iterable<IJiraIssueLink> getIssueLinks();

    Iterable<IJiraSubtask> getSubtasks();

    Iterable<IJiraIssueField> getFields();

    IJiraIssueField getField(String id);

    IJiraIssueField getFieldByName(String name);

    IJiraIssueType getIssueType();

    IJiraBasicProject getProject();

    IJiraBasicVotes getVotes();

    Iterable<IJiraVersion> getFixVersions();

    Date getCreationDate();

    Date getUpdateDate();

    Date getDueDate();

    String getDescription();
}
