package impactassessment.artifact.base;

import org.joda.time.DateTime;

import java.net.URI;

public interface IArtifact {

    URI getSelf();

    String getKey();

    String getId();

    IStatus getStatus();

    IUser getReporter();

    IUser getAssignee();

    String getSummary();

    IBasicPriority getPriority();

    Iterable<IIssueLink> getIssueLinks();

    Iterable<ISubtask> getSubtasks();

    Iterable<IIssueField> getFields();

    IIssueField getField(String id);

    IIssueField getFieldByName(String name);

    IIssueType getIssueType();

    IBasicProject getProject();

    IBasicVotes getVotes();

    Iterable<IVersion> getFixVersions();

    DateTime getCreationDate();

    DateTime getUpdateDate();

    DateTime getDueDate();

    String getDescription();
}
