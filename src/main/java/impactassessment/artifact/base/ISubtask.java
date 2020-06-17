package impactassessment.artifact.base;

import java.net.URI;

public interface ISubtask {

    String getIssueKey();

    URI getIssueUri();

    String getSummary();

    IIssueType getIssueType();

    IStatus getStatus();

}
