package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.Release;

import java.util.Date;

public class JamaRelease {

    private Release release;
    private JamaProjectArtifact jamaProjectArtifact;

    public JamaRelease(Release release) {
        this.release = release;
        this.jamaProjectArtifact = new JamaProjectArtifact(release.getProject());
    }

    public String getName() {
        return release.getName();
    }

    public String getDescription() {
        return release.getDescription();
    }

    public JamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    public Date getReleaseDate() {
        return release.getReleaseDate();
    }

    public boolean isActive() {
        return release.isActive();
    }

    public boolean isArchived() {
        return release.isArchived();
    }

    public Integer getItemCount() {
        return release.getItemCount();
    }

    @Override
    public String toString() {
        return release.toString();
    }
}
