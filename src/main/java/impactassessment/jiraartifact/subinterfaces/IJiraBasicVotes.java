package impactassessment.jiraartifact.subinterfaces;

import java.net.URI;

public interface IJiraBasicVotes {

    URI getSelf();

    int getVotes();

    boolean hasVoted();

}
