package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.Release;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaRelease;

import java.util.Date;

public class JamaRelease implements IJamaRelease {

    private String name;
    private String description;
    private IJamaProjectArtifact jamaProjectArtifact;
    private Date releaseDate;
    private boolean active;
    private boolean achieved;
    private int itemCount;

    public JamaRelease(Release release) {
        if (release != null) {
            this.name = release.getName();
            this.description = release.getDescription();
            this.jamaProjectArtifact = new JamaProjectArtifact(release.getProject());
            this.releaseDate = release.getReleaseDate();
            this.active = release.isActive();
            this.achieved = release.isArchived();
            this.itemCount = release.getItemCount();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IJamaProjectArtifact getProject() {
        return jamaProjectArtifact;
    }

    @Override
    public Date getReleaseDate() {
        return releaseDate;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isArchived() {
        return achieved;
    }

    @Override
    public Integer getItemCount() {
        return itemCount;
    }

}
