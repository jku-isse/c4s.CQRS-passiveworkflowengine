package impactassessment.artifactconnector.jama.subtypes;

import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;

import java.util.Date;

public class JamaProjectArtifact implements IJamaProjectArtifact {

    private String projectKey;
    private String name;
    private String description;
    private Boolean folder;
    private IJamaUserArtifact userCreated;
    private Date createdDate;
    private Date modifiedDate;
    private IJamaUserArtifact userModified;

    public JamaProjectArtifact(JamaProject jamaProject) {
        if (jamaProject != null) {
            this.projectKey = jamaProject.getProjectKey();
            this.name = jamaProject.getName();
            this.description = jamaProject.getDescription();
            this.folder = jamaProject.isFolder();
            this.userCreated = new JamaUserArtifact(jamaProject.getCreatedBy());
            this.createdDate = jamaProject.getCreatedDate();
            this.modifiedDate = jamaProject.getModifiedDate();
            this.userModified = new JamaUserArtifact(jamaProject.getModifiedBy());
        }
    }

    @Override
    public String getProjectKey() {
        return projectKey;
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
    public boolean isFolder() {
        return folder;
    }

    @Override
    public IJamaUserArtifact getCreatedBy() {
        return userCreated;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public Date getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public IJamaUserArtifact getModifiedBy() {
        return userModified;
    }

}
