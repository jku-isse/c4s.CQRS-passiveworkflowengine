package impactassessment.artifactconnector.jira.subinterfaces;

import java.net.URI;

public interface IJiraIssueType {

    Long getId();

    String getName();

    boolean isSubtask();

    URI getSelf();

    String getDescription();

}
