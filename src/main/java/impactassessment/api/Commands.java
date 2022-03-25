package impactassessment.api;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.CorrelationTuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Commands {

	public interface IdentifiableCmd {
        String getId();
        
    }
	
	public static abstract class TrackableCmd implements IdentifiableCmd{
		
		String parentCauseRef;
		public String getParentCauseRef() {
			return parentCauseRef;
		}
		public TrackableCmd setParentCauseRef(String parentCauseRef) {
			this.parentCauseRef = parentCauseRef;
			return this;
		}
		
	}
	
	@Data
	public static class CompositeCmd extends TrackableCmd {
		@TargetAggregateIdentifier
        private final String id;
		private final List<TrackableCmd> commandList;
	}
	
    @Data
    public static class CreateWorkflowCmd extends TrackableCmd{
        @TargetAggregateIdentifier
        private final String id;
        private final Map<ArtifactIdentifier, String> input;
        private final String definitionName;
    }
    @Data
    public static class CreateSubWorkflowCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String parentWfiId;
        private final String parentWftId;
        private final String definitionName;
        private final Map<ArtifactIdentifier, String> input;
    }

    @Data
    public static class CompleteDataflowCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final ResourceLink res;
    }

    @Data
    public static class DeleteCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
    }
    @Data
    public static class AddConstraintsCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final Map<String, String> rules;
    }
    @Data
    public static class AddEvaluationResultToConstraintCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final String qacId;
        private final Map<ResourceLink, Boolean> res;
        private final CorrelationTuple corr;
        private final Instant time;
    }
    @Data
    public static class CheckConstraintCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String constrId;
    }
    @Data
    public static class CheckAllConstraintsCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
    }
    @Data
    public static class AddInputCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddOutputCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddInputToWorkflowCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddOutputToWorkflowCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class RemoveOutputCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifactId;
        private final String role;
    }
    @Data
    public static class RemoveInputCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final ArtifactIdentifier artifactId;
        private final String role;
    }
    @Data
    public static class UpdateArtifactsCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final List<IArtifact> artifacts;
    }
    @Data
    public static class SetPreConditionsFulfillmentCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class SetPostConditionsFulfillmentCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class ActivateTaskCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
    }
    
    @Data
    public static class ChangeCanceledStateOfTaskCmd extends TrackableCmd {
    	@TargetAggregateIdentifier
        private final String id;
        private final String wftId; // or wfi Id if whole process is to be canceled or uncanceled
        private final boolean isCanceled;
    }
    
    @Data
    public static class SetPropertiesCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String iwftId;
        private final Map<String, String> properties;
    }
    @Data
    public static class InstantiateTaskCmd extends TrackableCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String taskDefinitionId;
        private final List<ArtifactInput> optionalInputs;
        private final List<ArtifactOutput> optionalOutputs;
    }
    
    @Data
    public static class RefreshFrontendDataCmd extends TrackableCmd {
    	 @TargetAggregateIdentifier
         private final String id;
    }
}

