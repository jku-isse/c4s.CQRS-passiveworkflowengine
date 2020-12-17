package impactassessment.artifactconnector.jama.subtypes;

import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaRelease;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.Release;

import java.util.Date;

public class JamaRelease implements IJamaRelease {

    private String name;
    private String description;
    private IJamaProjectArtifact jamaProjectArtifact;
    private Date releaseDate;
    private Boolean active;
    private Boolean achieved;
    private Integer itemCount;

    public JamaRelease(Release release) {
        this.name = release.getName();
        this.description = release.getDescription();
        this.jamaProjectArtifact = release.getProject() != null ? new JamaProjectArtifact(release.getProject()) : null;
        this.releaseDate = release.getReleaseDate();
        this.active = release.isActive();
        this.achieved = release.isArchived();
        this.itemCount = release.getItemCount();
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
