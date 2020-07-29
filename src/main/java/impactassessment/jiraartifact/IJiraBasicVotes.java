package impactassessment.jiraartifact;

import java.net.URI;

public interface IJiraBasicVotes {

    URI getSelf();

    int getVotes();

    boolean hasVoted();

}
