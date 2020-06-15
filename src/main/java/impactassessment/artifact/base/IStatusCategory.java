package impactassessment.artifact.base;

import java.net.URI;

public interface IStatusCategory {

    Long getId();

    URI getSelf();

    String getKey();

    String getName();
}
