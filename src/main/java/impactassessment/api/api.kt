package impactassessment.api

import impactassessment.mock.artifact.Artifact
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.kie.api.runtime.rule.FactHandle

// COMMANDS
data class AddArtifactCmd(@TargetAggregateIdentifier val id: String, val artifact: Artifact)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val artifact: Artifact)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val branchId: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)
data class AppendQACheckDocumentCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val state: String)
data class AddQAConstraintCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val constrPrefix: String, val ruleName: String, val description: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class AddedArtifactEvt(override val id: String, val artifact: Artifact) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dniId: String, val artifact: Artifact) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dniId: String, val wftId: String) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dniId: String, val branchId: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt
data class AppendedQACheckDocumentEvt(override val id: String, val wftId: String, val state: String) : IdentifiableEvt
data class AddedQAConstraintEvt(override val id: String, val wftId: String, val constrPrefix: String, val ruleName: String, val description: String) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
