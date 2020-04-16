package impactassessment.api

import impactassessment.mock.artifact.Artifact
import impactassessment.model.workflowmodel.AbstractWorkflowInstanceObject
import impactassessment.model.workflowmodel.DecisionNodeInstance
import impactassessment.model.workflowmodel.WorkflowTask
import org.axonframework.modelling.command.TargetAggregateIdentifier

// COMMANDS
data class AddArtifactCmd(@TargetAggregateIdentifier val id: String, val artifact: Artifact)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dni: DecisionNodeInstance)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dni: DecisionNodeInstance, val wft: WorkflowTask)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dni: DecisionNodeInstance, val branchId: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class AddedArtifactEvt(override val id: String, val artifact: Artifact) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dni: DecisionNodeInstance) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dni: DecisionNodeInstance, val wft: WorkflowTask) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dni: DecisionNodeInstance, val branchId: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
