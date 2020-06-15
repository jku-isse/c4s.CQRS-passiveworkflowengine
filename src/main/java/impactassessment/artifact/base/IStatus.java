package impactassessment.artifact.base;

import java.net.URI;

public interface IStatus {

    URI getSelf();

    String getName();

    Long getId();

    String getDescription();

    IStatusCategory getStatusCategory();
}
