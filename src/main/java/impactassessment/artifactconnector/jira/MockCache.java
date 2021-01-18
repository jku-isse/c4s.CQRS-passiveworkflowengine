package impactassessment.artifactconnector.jira;

import c4s.jiralightconnector.IssueAgent;
import c4s.jiralightconnector.IssueCache;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

@Component
public class MockCache implements IssueCache {

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
    public void removeFromCache(String s) {

    }

    @Override
    public Set<String> getCachedItemIds() {
        return null;
    }
}
