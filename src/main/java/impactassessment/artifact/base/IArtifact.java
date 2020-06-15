package impactassessment.artifact.base;

import impactassessment.model.workflowmodel.ResourceLink;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.List;

public interface IArtifact {

    ResourceLink toResourceLink();

    URI getSelf();

    String getKey();

    Long getId();

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
