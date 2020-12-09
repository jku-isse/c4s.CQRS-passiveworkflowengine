package impactassessment.artifactconnector.jira.subinterfaces;

import java.net.URI;

public interface IJiraBasicVotes {

    URI getSelf();

    int getVotes();

    boolean hasVoted();

}
