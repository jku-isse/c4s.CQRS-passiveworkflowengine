package impactassessment.artifact.base;

import javax.annotation.Nullable;
import java.net.URI;

public interface IBasicProject {
    URI getSelf();

    String getKey();

    @Nullable
    String getName();

    @Nullable
    Long getId();
}
