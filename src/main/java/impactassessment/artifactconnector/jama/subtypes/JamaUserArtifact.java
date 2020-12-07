package impactassessment.artifactconnector.jama.subtypes;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;
import impactassessment.artifactconnector.jama.subinterfaces.IJamaUserArtifact;

public class JamaUserArtifact implements IJamaUserArtifact {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String title;
    private String location;
    private String licenseType;
    private boolean active;

    public JamaUserArtifact(JamaUser jamaUser) {
        if (jamaUser != null) {
            this.username = jamaUser.getUsername();
            this.firstName = jamaUser.getFirstName();
            this.lastName = jamaUser.getLastName();
            this.email = jamaUser.getEmail();
            this.phone = jamaUser.getPhone();
            this.title = jamaUser.getTitle();
            this.location = jamaUser.getLocation();
            this.licenseType = jamaUser.getLicenseType();
            this.active = jamaUser.isActive();
        }
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getLicenseType() {
        return licenseType;
    }

    @Override
    public boolean isActive() {
        return active;
    }

}
