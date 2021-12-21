package impactassessment.api;

import artifactapi.ArtifactIdentifier;
import artifactapi.ResourceLink;
import lombok.Data;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.CorrelationTuple;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class Events {

    public interface IdentifiableEvt {
        String getId();
        
    }
    
    public static abstract class TimedEvt implements IdentifiableEvt {
    	OffsetDateTime timestamp;
    	public void setTimestamp(OffsetDateTime timestamp) {
    		this.timestamp = timestamp;
    	}
		public OffsetDateTime getTimestamp() {
			return timestamp;
		}
		
		String parentCauseRef;
		public String getParentCauseRef() {
			return parentCauseRef;
		}
		public void setParentCauseRef(String parentCauseRef) {
			this.parentCauseRef = parentCauseRef;
		}
		
    }

    @Data
    public static class CompositeEvt extends TimedEvt {
    	private final String id;
    	private final List<TimedEvt> eventList;
    }
    
    @Data
    public static class CreatedWorkflowEvt extends TimedEvt {
        private final String id;
        private final Map<ArtifactIdentifier, String> artifacts;
        private final String definitionName;
        private final WorkflowDefinition wfd;
    }
    @Data
    public static class CreatedSubWorkflowEvt extends TimedEvt {
        private final String id;
        private final String parentWfiId;
        private final String parentWftId;
        private final String definitionName;
        private final WorkflowDefinition wfd;
        private final Map<ArtifactIdentifier, String> artifacts;
    }
    @Data
    public static class CompletedDataflowEvt extends TimedEvt {
        private final String id;
        private final String dniId;
        private final ResourceLink res;
    }
    
    @Data
    public static class DeletedEvt extends TimedEvt {
        private final String id;
    }
    @Data
    public static class AddedConstraintsEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final Map<String, String> rules;
    }
    @Data
    public static class AddedEvaluationResultToConstraintEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final String qacId;
        private final Map<ResourceLink, Boolean> res;
        private final CorrelationTuple corr;
        private final Instant time;
    }
    @Data
    public static class UpdatedEvaluationTimeEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final String qacId;
        private final CorrelationTuple corr;
        private final Instant time;
    }
    @Data
    public static class CheckedConstraintEvt extends TimedEvt {
        private final String id;
        private final String constrId;
    }
    @Data
    public static class CheckedAllConstraintsEvt extends TimedEvt {
        private final String id;
    }
    @Data
    public static class AddedInputEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class AddedOutputEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class AddedInputToWorkflowEvt extends TimedEvt {
        private final String id;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class AddedOutputToWorkflowEvt extends TimedEvt {
        private final String id;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class RemovedOutputEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class RemovedInputEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifact;
        private final String role;
    }
    @Data
    public static class UpdatedArtifactsEvt extends TimedEvt {
        private final String id;
        private final List<ArtifactIdentifier> artifacts;
    }
    @Data
    public static class SetPreConditionsFulfillmentEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class SetPostConditionsFulfillmentEvt extends TimedEvt {
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class ActivatedTaskEvt extends TimedEvt {
        private final String id;
        private final String wftId;
    }
    @Data
    public static class ChangedCanceledStateOfTaskEvt extends TimedEvt{
    	@TargetAggregateIdentifier
        private final String id;
        private final String wftId; // or wfi Id if whole process is to be canceled or uncanceled
        private final boolean isCanceled;
    }
    
    @Data
    public static class SetPropertiesEvt extends TimedEvt {
        private final String id;
        private final String iwftId;
        private final Map<String, String> properties;
    }
    @Data
    public static class InstantiatedTaskEvt extends TimedEvt {
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

