package impactassessment.artifactconnector.jama.subinterfaces;

public interface IJamaUserArtifact {
    String getUsername();

    String getFirstName();

    String getLastName();

    String getEmail();

    String getPhone();

    String getTitle();

    String getLocation();

    String getLicenseType();

    boolean isActive();

    @Override
    String toString();
}
