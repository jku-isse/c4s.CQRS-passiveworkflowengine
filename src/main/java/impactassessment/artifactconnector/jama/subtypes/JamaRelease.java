package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.Release;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;

import java.util.Date;

public class JamaRelease implements IJamaRelease {

    private Release release;
    private IJamaProjectArtifact jamaProjectArtifact;

    public JamaRelease(Release release) {
        this.release = release;
        this.jamaProjectArtifact = new JamaProjectArtifact(release.getProject());
    }

    @Override
    public String getName() {
        return release.getName();
    }

    @Override
    public String getDescription() {
        return release.getDescription();
    }

    @Override
    public IJamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    @Override
    public Date getReleaseDate() {
        return release.getReleaseDate();
    }

    @Override
    public boolean isActive() {
        return release.isActive();
    }

    @Override
    public boolean isArchived() {
        return release.isArchived();
    }

    @Override
    public Integer getItemCount() {
        return release.getItemCount();
    }

    @Override
    public String toString() {
        return release.toString();
    }
}
