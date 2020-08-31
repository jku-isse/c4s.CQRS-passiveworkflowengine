package impactassessment.passiveprocessengine.verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Warning {

    private List<String> affectedArtifacts;
    private String description;

    protected Warning(String description, String... affectedArtifacts) {
        this.description = description;
        this.affectedArtifacts = new ArrayList<>();
        this.affectedArtifacts.addAll(Arrays.asList(affectedArtifacts));
    }

    public List<String> getAffectedArtifacts() {
        return affectedArtifacts;
    }

    public String getDescription() {
        return description;
    }
}
