package impactassessment.command;


import lombok.AllArgsConstructor;
import lombok.Data;
import passiveprocessengine.definition.ArtifactType;

@Data
public class ArtifactUsage {
    private String artifactKey;
    private Usage usage;
    private String wftId;
    private String role;
    private ArtifactType artifactType;
    private String corrId;

    private ArtifactUsage(String artifactKey, Usage usage, String wftId, String role, ArtifactType artifactType, String corrId) {
        this.artifactKey = artifactKey;
        this.usage = usage;
        this.wftId = wftId;
        this.role = role;
        this.artifactType = artifactType;
        this.corrId = corrId;
    }

    /**
     * used for INPUT, OUTPUT
     */
    public ArtifactUsage(String artifactKey, Usage usage, String wftId, String role, ArtifactType artifactType) {
        this(artifactKey, usage, wftId, role, artifactType, null);
    }

    /**
     * used for WF_INPUT, WF_OUTPUT
     */
    public ArtifactUsage(String artifactKey, Usage usage, String role, ArtifactType artifactType) {
        this(artifactKey, usage, null, role, artifactType);
    }

    /**
     * used for RESOURCE
     */
    public ArtifactUsage(String artifactKey, Usage usage, String corrId) {
        this(artifactKey, usage, null, null, null, corrId);
    }

    public enum Usage {
        INPUT, OUTPUT, WF_INPUT, WF_OUTPUT, RESOURCE
    }
}
