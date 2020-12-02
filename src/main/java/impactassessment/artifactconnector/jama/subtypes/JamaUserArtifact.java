package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;

public class JamaUserArtifact implements IJamaUserArtifact {

    private JamaUser jamaUser;

    public JamaUserArtifact(JamaUser jamaUser) {
        this.jamaUser = jamaUser;
    }

    @Override
    public String getUsername() {
        return jamaUser.getUsername();
    }

    @Override
    public String getFirstName() {
        return jamaUser.getFirstName();
    }

    @Override
    public String getLastName() {
        return jamaUser.getLastName();
    }

    @Override
    public String getEmail() {
        return jamaUser.getEmail();
    }

    @Override
    public String getPhone() {
        return jamaUser.getPhone();
    }

    @Override
    public String getTitle() {
        return jamaUser.getTitle();
    }

    @Override
    public String getLocation() {
        return jamaUser.getLocation();
    }

    @Override
    public String getLicenseType() {
        return jamaUser.getLicenseType();
    }

    @Override
    public boolean isActive() {
        return jamaUser.isActive();
    }

    @Override
    public String toString() {
        return jamaUser.toString();
    }
}
