package impactassessment.artifact.base;

import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.net.URI;

public interface IVersion {

    URI getSelf();

    @Nullable
    Long getId();

    String getDescription();

    String getName();

    boolean isArchived();

    boolean isReleased();

    @Nullable
    DateTime getReleaseDate();

}
