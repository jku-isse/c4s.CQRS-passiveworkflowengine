package impactassessment.artifact.base;

import java.net.URI;

public interface IIssueType {

    Long getId();

    String getName();

    boolean isSubtask();

    URI getSelf();

    String getDescription();

}
