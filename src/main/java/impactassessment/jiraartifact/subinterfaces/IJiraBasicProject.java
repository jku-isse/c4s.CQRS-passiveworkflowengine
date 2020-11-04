package impactassessment.jiraartifact.subinterfaces;

import javax.annotation.Nullable;
import java.net.URI;

public interface IJiraBasicProject {

    URI getSelf();

    String getKey();

    @Nullable
    String getName();

    @Nullable
    Long getId();
}
