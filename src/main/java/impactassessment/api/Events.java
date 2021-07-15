package impactassessment.api;

import artifactapi.ArtifactIdentifier;
import artifactapi.ResourceLink;
import lombok.Data;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.CorrelationTuple;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Events {

    public interface IdentifiableEvt {
        String getId();
    }

    @Data
    public static class CreatedWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final Collection<Entry<String,ArtifactIdentifier>> artifacts; // TODO we probably need this to be a tuple of: role, type, and artifact or role and artifactWrapper
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
        private final Collection<Entry<String,ArtifactIdentifier>> artifacts; // TODO we probably need this to be a tuple of: role, type, and artifact or role and artifactWrapper
    }
    @Data
    public static class CompletedDataflowEvt implements IdentifiableEvt {
        private final String id;
        private final String dniId;
        private final ResourceLink res;
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
        private final ArtifactIdentifier artifact;
        private final String role;
        private final String type; // TODO remove type, already inside ArtifactIdentifier
    }
    @Data
    public static class AddedOutputEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
        private final String type; // TODO remove type, already inside ArtifactIdentifier
    }
    @Data
    public static class AddedInputToWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final ArtifactIdentifier artifact;
        private final String role;
        private final String type; // TODO remove type, already inside ArtifactIdentifier
    }
    @Data
    public static class AddedOutputToWorkflowEvt implements IdentifiableEvt {
        private final String id;
        private final ArtifactIdentifier artifact;
        private final String role;
        private final String type; // TODO remove type, already inside ArtifactIdentifier
    }
    @Data
    public static class RemovedOutputEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class RemovedInputEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class UpdatedArtifactsEvt implements IdentifiableEvt {
        private final String id;
        private final List<ArtifactIdentifier> artifacts;
    }
    @Data
    public static class SetPreConditionsFulfillmentEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class SetPostConditionsFulfillmentEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class ActivatedTaskEvt implements IdentifiableEvt {
        private final String id;
        private final String wftId;
    }
    @Data
    public static class SetPropertiesEvt implements IdentifiableEvt {
        private final String id;
        private final String iwftId;
        private final Map<String, String> properties;
    }
    @Data
    public static class InstantiatedTaskEvt implements IdentifiableEvt {
        private final String id;
        private final String taskDefinitionId;
        private final List<ArtifactInput> optionalInputs;
        private final List<ArtifactOutput> optionalOutputs;
    }
    
    @Data
    public static class RefreshedTriggerEvent{
    	 private final String id;
    }
}

