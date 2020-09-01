package impactassessment.passiveprocessengine.verification;

import java.util.ArrayList;
import java.util.List;

public class Report {

    List<Warning> warnings;
    List<Patch> patches;

    protected Report() {
        warnings = new ArrayList<>();
        patches = new ArrayList<>();
    }

    protected void addWarnings(Warning... warnings) {
        if (warnings != null && warnings.length > 0) this.warnings.addAll(List.of(warnings));
    }

    public List<Warning> getWarnings() {
        return warnings;
    }

    protected void addPatches(Patch... patches) {
        if (patches != null && patches.length > 0) this.patches.addAll(List.of(patches));
    }

    public List<Patch> getPatches() {
        return patches;
    }

    public static class Warning {

        private String affectedArtifact;
        private String description;
        private WarningType warningType;

        protected Warning(WarningType warningType, String description, String affectedArtifact) {
            this.description = description;
            this.affectedArtifact = affectedArtifact;
            this.warningType = warningType;
        }

        public String getAffectedArtifact() {
            return affectedArtifact;
        }

        public String getDescription() {
            return description;
        }

        public WarningType getWarningType() {
            return warningType;
        }

    }

    enum WarningType {PLACEHOLDER, MAPPING, STRUCTURE}

    public static class Patch {

        private String affectedArtifact;
        private String description;

        protected Patch(String description, String affectedArtifact) {
            this.description = description;
            this.affectedArtifact = affectedArtifact;
        }

        public String getAffectedArtifact() {
            return affectedArtifact;
        }

        public String getDescription() {
            return description;
        }

    }
}
