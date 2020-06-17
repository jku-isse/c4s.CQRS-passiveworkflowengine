package impactassessment.artifact.base;

import java.net.URI;

public interface IBasicVotes {

    URI getSelf();

    int getVotes();

    boolean hasVoted();

}
