package impactassessment.jiraartifact;

import impactassessment.jiraartifact.subinterfaces.*;
import org.joda.time.DateTime;

import java.net.URI;

public interface IJiraArtifact {

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

    DateTime getCreationDate();

    DateTime getUpdateDate();

    DateTime getDueDate();

    String getDescription();
}
