package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

import java.util.Date;
import java.util.List;

public class JamaProjectArtifact {
    private JamaProject jamaProject;

    public JamaProjectArtifact(JamaProject jamaProject) {
        this.jamaProject = jamaProject;
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

    public JamaUser getCreatedBy() {
        return jamaProject.getCreatedBy();
    }

    public Date getCreatedDate() {
        return jamaProject.getCreatedDate();
    }

    public Date getModifiedDate() {
        return jamaProject.getModifiedDate();
    }

    public JamaUser getModifiedBy() {
        return jamaProject.getModifiedBy();
    }

    public List<JamaItem> getItems() throws RestClientException {
        return jamaProject.getItems();
    }

    @Override
    public String toString() {
        return jamaProject.toString();
    }
}
