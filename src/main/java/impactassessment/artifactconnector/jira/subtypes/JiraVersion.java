package impactassessment.artifactconnector.jira.subtypes;

import artifactapi.jira.subtypes.IJiraVersion;
import com.atlassian.jira.rest.client.api.domain.Version;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Date;

public class JiraVersion implements IJiraVersion {

        private Version version;

        public JiraVersion(Version version) {
            this.version = version;
        }

        @Override
        public URI getSelf() {
            return version.getSelf();
        }

        @Nullable
        @Override
        public Long getId() {
            return version.getId();
        }

        @Override
        public String getDescription() {
            return version.getDescription();
        }

        @Override
        public String getName() {
            return version.getName();
        }

        @Override
        public boolean isArchived() {
            return version.isArchived();
        }

        @Override
        public boolean isReleased() {
            return version.isReleased();
        }

        @Nullable
        @Override
        public Date getReleaseDate() {
            return version.getReleaseDate() != null ? version.getReleaseDate().toDate() : null;
        }
}
