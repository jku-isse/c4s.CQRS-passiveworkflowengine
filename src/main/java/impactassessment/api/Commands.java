package impactassessment.api;

import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import passiveprocessengine.definition.TaskLifecycle;
import passiveprocessengine.instance.CorrelationTuple;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Commands {
    @Data
    public static class CreateMockWorkflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String status;
        private final String issuetype;
        private final String priority;
        private final String summary;
    }
    @Data
    public static class CreateWorkflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final Map<String, String> input;
        private final String definitionName;
    }
    @Data
    public static class CreateSubWorkflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String parentWfiId;
        private final String parentWftId;
        private final String definitionName;
        private final Collection<Entry<String,IArtifact>> artifacts;
    }

    @Data
    public static class CompleteDataflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final ResourceLink res;
    }
    @Data
    public static class ActivateInBranchCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final String wftId;
    }
    @Data
    public static class ActivateOutBranchCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final String branchId;
    }
    @Data
    public static class ActivateInOutBranchCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final String wftId;
        private final String branchId;
    }
    @Data
    public static class ActivateInOutBranchesCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String dniId;
        private final String wftId;
        private final Set<String> branchIds;
    }
    @Data
    public static class DeleteCmd {
        @TargetAggregateIdentifier
        private final String id;
    }
    @Data
    public static class AddConstraintsCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final Map<String, String> rules;
    }
    @Data
    public static class AddEvaluationResultToConstraintCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String qacId;
        private final Map<ResourceLink, Boolean> res;
        private final CorrelationTuple corr;
        private final Instant time;
    }
    @Data
    public static class CheckConstraintCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String corrId;
    }
    @Data
    public static class CheckAllConstraintsCmd {
        @TargetAggregateIdentifier
        private final String id;
    }
    @Data
    public static class AddInputCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddOutputCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddInputToWorkflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class AddOutputToWorkflowCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String artifactId;
        private final String role;
        private final String type;
    }
    @Data
    public static class UpdateArtifactsCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final List<IArtifact> artifacts;
    }
    @Data
    public static class SetPreConditionsFulfillmentCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class SetPostConditionsFulfillmentCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
        private final boolean isFulfilled;
    }
    @Data
    public static class ActivateTaskCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String wftId;
    }
    @Data
    public static class SetPropertiesCmd {
        @TargetAggregateIdentifier
        private final String id;
        private final String iwftId;
        private final Map<String, String> properties;
    }
}

