package impactassessment.api

import impactassessment.analytics.CorrelationTuple
import impactassessment.mock.artifact.Artifact
import impactassessment.model.WorkflowInstanceWrapper
import impactassessment.model.workflowmodel.ResourceLink
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.kie.api.runtime.rule.FactHandle
import java.time.Instant

// COMMANDS
data class AddArtifactCmd(@TargetAggregateIdentifier val id: String, val artifact: Artifact)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val artifact: Artifact)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val branchId: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)
data class AddQAConstraintCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val state: String, val constrPrefix: String, val ruleName: String, val description: String)
data class AddResourceToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple, val time: Instant)
data class AddResourcesToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val fulfilled: Boolean, val res: List<ResourceLink>, val corr: CorrelationTuple, val time: Instant)
data class CheckConstraintCmd(@TargetAggregateIdentifier val id: String, val corrId: String)
data class PrintKBCmd(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class AddedArtifactEvt(override val id: String, val artifact: Artifact) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dniId: String, val artifact: Artifact) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dniId: String, val wftId: String) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dniId: String, val branchId: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt
data class AddedQAConstraintEvt(override val id: String, val wftId: String, val state: String, val constrPrefix: String, val ruleName: String, val description: String) : IdentifiableEvt
data class AddedResourceToConstraintEvt(override val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple, val time: Instant) : IdentifiableEvt
data class AddedResourcesToConstraintEvt(override val id: String, val qacId: String, val fulfilled: Boolean, val res: List<ResourceLink>, val corr: CorrelationTuple, val time: Instant) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)
data class GetStateQuery(val depth: Int)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)
data class GetStateResponse(val state: List<WorkflowInstanceWrapper>)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
