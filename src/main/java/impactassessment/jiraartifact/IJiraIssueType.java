package impactassessment.jiraartifact;

import java.net.URI;

public interface IJiraIssueType {

    Long getId();

    String getName();

    boolean isSubtask();

    URI getSelf();

    String getDescription();

}
