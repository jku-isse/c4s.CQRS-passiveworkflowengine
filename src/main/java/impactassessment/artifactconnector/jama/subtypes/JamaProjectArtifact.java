package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaProjectArtifact;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;

import java.util.Date;

public class JamaProjectArtifact implements IJamaProjectArtifact {
    private JamaProject jamaProject;
    private IJamaUserArtifact userCreated;
    private IJamaUserArtifact userModified;

    public JamaProjectArtifact(JamaProject jamaProject) {
        this.jamaProject = jamaProject;
        this.userCreated = new JamaUserArtifact(jamaProject.getCreatedBy());
        this.userModified = new JamaUserArtifact(jamaProject.getModifiedBy());
    }

    @Override
    public String getProjectKey() {
        return jamaProject.getProjectKey();
    }

    @Override
    public String getName() {
        return jamaProject.getName();
    }

    @Override
    public String getDescription() {
        return jamaProject.getDescription();
    }

    @Override
    public boolean isFolder() {
        return jamaProject.isFolder();
    }

    @Override
    public IJamaUserArtifact getCreatedBy() {
        return userCreated;
    }

    @Override
    public Date getCreatedDate() {
        return jamaProject.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return jamaProject.getModifiedDate();
    }

    @Override
    public IJamaUserArtifact getModifiedBy() {
        return userModified;
    }

    @Override
    public String toString() {
        return jamaProject.toString();
    }
}
