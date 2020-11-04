package impactassessment.jiraartifact.subtypes;

import com.atlassian.jira.rest.client.api.domain.BasicVotes;
import impactassessment.jiraartifact.subinterfaces.IJiraBasicVotes;

import java.net.URI;

public class JiraBasicVotes implements IJiraBasicVotes {

    private BasicVotes basicVotes;

    public JiraBasicVotes(BasicVotes basicVotes) {
        this.basicVotes = basicVotes;
    }

    @Override
    public URI getSelf() {
        return basicVotes.getSelf();
    }

    @Override
    public int getVotes() {
        return basicVotes.getVotes();
    }

    @Override
    public boolean hasVoted() {
        return basicVotes.hasVoted();
    }
}
