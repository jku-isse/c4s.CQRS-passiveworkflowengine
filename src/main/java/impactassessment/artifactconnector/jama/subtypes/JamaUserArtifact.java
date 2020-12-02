package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

public class JamaUserArtifact {

    private JamaUser jamaUser;

    public JamaUserArtifact(JamaUser jamaUser) {
        this.jamaUser = jamaUser;
    }

    public String getUsername() {
        return jamaUser.getUsername();
    }

    public String getFirstName() {
        return jamaUser.getFirstName();
    }

    public String getLastName() {
        return jamaUser.getLastName();
    }

    public String getEmail() {
        return jamaUser.getEmail();
    }

    public String getPhone() {
        return jamaUser.getPhone();
    }

    public String getTitle() {
        return jamaUser.getTitle();
    }

    public String getLocation() {
        return jamaUser.getLocation();
    }

    public String getLicenseType() {
        return jamaUser.getLicenseType();
    }

    public boolean isActive() {
        return jamaUser.isActive();
    }

    @Override
    public String toString() {
        return jamaUser.toString();
    }
}
