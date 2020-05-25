package impactassessment.api

import impactassessment.analytics.CorrelationTuple
import impactassessment.mock.artifact.Artifact
import impactassessment.model.workflowmodel.ResourceLink
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.kie.api.runtime.rule.FactHandle

// COMMANDS
data class AddArtifactCmd(@TargetAggregateIdentifier val id: String, val artifact: Artifact)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val artifact: Artifact)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val branchId: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)
data class AddQAConstraintCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val state: String, val constrPrefix: String, val ruleName: String, val description: String)
data class AddResourceToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple)
data class AddResourcesToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val fulfilled: Boolean, val res: List<ResourceLink>, val corr: CorrelationTuple)
data class CheckConstraintCmd(@TargetAggregateIdentifier val id: String, val corrId: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class AddedArtifactEvt(override val id: String, val artifact: Artifact) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dniId: String, val artifact: Artifact) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dniId: String, val wftId: String) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dniId: String, val branchId: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt
data class AddedQAConstraintEvt(override val id: String, val wftId: String, val state: String, val constrPrefix: String, val ruleName: String, val description: String) : IdentifiableEvt
data class AddedResourceToConstraintEvt(override val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple) : IdentifiableEvt
data class AddedResourcesToConstraintEvt(override val id: String, val qacId: String, val fulfilled: Boolean, val res: List<ResourceLink>, val corr: CorrelationTuple) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
