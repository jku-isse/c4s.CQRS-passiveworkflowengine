package impactassessment.api;

import impactassessment.jiraartifact.IJiraArtifact;
import lombok.Data;
import passiveprocessengine.definition.Artifact;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.CorrelationTuple;
import passiveprocessengine.instance.ResourceLink;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Events {

    public interface IdentifiableEvt {
        String getId();
    }

    @Data
    public static class CreatedWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final List<IJiraArtifact> artifacts;
        private final String definitionName;
        private final WorkflowDefinition wfd;
    }
    @Data
    public static class CreatedSubWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final String parentWfiId;
        private final String parentWftId;
        private final String definitionName;
        private final WorkflowDefinition wfd;
    }
    @Data
    public static class CompletedDataflowEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final ResourceLink res;
    }
    @Data
    public static class ActivatedInBranchEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final String wftId;
    }
    @Data
    public static class ActivatedOutBranchEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final String branchId;
    }
    @Data
    public static class ActivatedInOutBranchEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final String wftId;
        private final String branchId;
    }
    @Data
    public static class ActivatedInOutBranchesEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final String wftId;
        private final Set<String> branchIds;
    }
    @Data
    public static class DeletedEvt implements IdentifiableEvt {
        private final String id;
    }
    @Data
    public static class AddedConstraintsEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final Map<String, String> rules;
    }
    @Data
    public static class AddedEvaluationResultToConstraintEvt implements IdentifiableEvt {
        private final String id;
        private final String qacId;
        private final Map<ResourceLink, Boolean> res;
        private final CorrelationTuple corr;
        private final Instant time;
    }
    @Data
    public static class CheckedConstraintEvt implements IdentifiableEvt {
        private final String id;
        private final String corrId;
    }
    @Data
    public static class CheckedAllConstraintsEvt implements IdentifiableEvt {
        private final String id;
    }
    @Data
    public static class AddedInputEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final Artifact artifact;
        private final String role;
        private final ArtifactType type;
    }
    @Data
    public static class AddedOutputEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final Artifact artifact;
        private final String role;
        private final ArtifactType type;
    }
    @Data
    public static class AddedInputToWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final ArtifactInput input;
    }
    @Data
    public static class AddedOutputToWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final ArtifactOutput output;
    }
    @Data
    public static class UpdatedArtifactsEvt implements IdentifiableEvt {
        private final String id;
        private final List<IJiraArtifact> artifacts;
    }
}

