package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

import java.util.Date;
import java.util.List;

public class JamaProjectArtifact {
    private JamaProject jamaProject;
    private JamaUserArtifact userCreated;
    private JamaUserArtifact userModified;

    public JamaProjectArtifact(JamaProject jamaProject) {
        this.jamaProject = jamaProject;
        this.userCreated = new JamaUserArtifact(jamaProject.getCreatedBy());
        this.userModified = new JamaUserArtifact(jamaProject.getModifiedBy());
    }

    public String getProjectKey() {
        return jamaProject.getProjectKey();
    }

    public String getName() {
        return jamaProject.getName();
    }

    public String getDescription() {
        return jamaProject.getDescription();
    }

    public boolean isFolder() {
        return jamaProject.isFolder();
    }

    public JamaUserArtifact getCreatedBy() {
        return userCreated;
    }

    public Date getCreatedDate() {
        return jamaProject.getCreatedDate();
    }

    public Date getModifiedDate() {
        return jamaProject.getModifiedDate();
    }

    public JamaUserArtifact getModifiedBy() {
        return userModified;
    }



    @Override
    public String toString() {
        return jamaProject.toString();
    }
}
