package impactassessment.jiraartifact.subinterfaces;

import java.net.URI;

public interface IJiraSubtask {

    String getIssueKey();

    URI getIssueUri();

    String getSummary();

    IJiraIssueType getIssueType();

    IJiraStatus getStatus();

}
