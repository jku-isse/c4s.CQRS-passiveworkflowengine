package impactassessment.passiveprocessengine.workflowmodel;

import java.util.Map;

public class MappingReport {

    // FROM
    private String fromWFT;
    private ArtifactType outputType;
    private String outputRole;
    // TO
    private String toWFT;
    private Map<String, ArtifactType> expectedInput;
    // REPORT
    private boolean roleMatched;
    private boolean typeMatched;

    public MappingReport(String fromWFT, ArtifactType outputType, String outputRole, String toWFT, Map<String,ArtifactType> expectedInput) {
        this.fromWFT = fromWFT;
        this.outputType = outputType;
        this.outputRole = outputRole;
        this.toWFT = toWFT;
        this.expectedInput = expectedInput;
        this.roleMatched = expectedInput.keySet().contains(outputRole);
        this.typeMatched = expectedInput.values().stream()
                .anyMatch(artT -> artT.getArtifactType().equals(outputType.getArtifactType()));
    }

    public String getFrom() {
        return fromWFT;
    }

    public String getTo() {
        return toWFT;
    }

    public boolean didRoleMatch() {
        return roleMatched;
    }

    public boolean didTypeMatch() {
        return typeMatched;
    }

    public boolean wasMappingSuccessful() {
        return roleMatched && typeMatched;
    }

}
