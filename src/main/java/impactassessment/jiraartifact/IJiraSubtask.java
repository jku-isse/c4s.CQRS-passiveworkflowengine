package impactassessment.jiraartifact;

import java.net.URI;

public interface IJiraSubtask {

    String getIssueKey();

    URI getIssueUri();

    String getSummary();

    IJiraIssueType getIssueType();

    IJiraStatus getStatus();

}
