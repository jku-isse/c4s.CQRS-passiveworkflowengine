package impactassessment.artifactconnector.jira;

import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.IssueCache;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class MockedJiraCache implements IssueCache {

    @Override
    public Optional<IssueAgent> getFromCache(String s) {
        return Optional.empty();
    }

    @Override
    public void insertOrUpdate(IssueAgent issueAgent) {

    }

    @Override
    public void updateIfExists(IssueAgent issueAgent) {

    }

    @Override
    public Optional<ZonedDateTime> getLastRefreshedOn() {
        return Optional.empty();
    }

    @Override
    public void setLastRefreshedOn(ZonedDateTime zonedDateTime) {

    }

	@Override
	public void removeFromCache(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getCachedItemIds() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

}
