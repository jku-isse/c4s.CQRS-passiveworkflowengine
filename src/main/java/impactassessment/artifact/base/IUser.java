package impactassessment.artifact.base;

import java.net.URI;

public interface IUser {

    URI getSelf();

    String getName();

    String getDisplayName();

    String getAccoutId();

    String getEmailAddress();

    boolean isActive();

}
