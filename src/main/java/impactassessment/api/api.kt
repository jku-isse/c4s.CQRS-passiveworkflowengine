package impactassessment.api

import impactassessment.workflowmodel.WorkflowDefinition
import impactassessment.workflowmodel.definition.QACheckDocument
import org.axonframework.modelling.command.TargetAggregateIdentifier

// COMMANDS
data class CreateWorkflowCmd(@TargetAggregateIdentifier val id: String)
data class CreateWorkflowInstanceOfCmd(@TargetAggregateIdentifier val id: String, val wfd: WorkflowDefinition)
data class EnableTasksAndDecisionsCmd(@TargetAggregateIdentifier val id: String)
data class CompleteDataflowOfDecisionNodeInstanceCmd(@TargetAggregateIdentifier val id: String, val dniIndex: Int)
data class AddQACheckDocumentsArtifactOutputsCmd(@TargetAggregateIdentifier val id: String, val qacd: QACheckDocument)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class CreatedWorkflowEvt(override val id: String) : IdentifiableEvt
data class CreatedWorkflowInstanceOfEvt(override val id: String, val wfd: WorkflowDefinition) : IdentifiableEvt
data class EnabledTasksAndDecisionsEvt(override val id: String) : IdentifiableEvt
data class CompletedDataflowOfDecisionNodeInstanceEvt(override val id: String, val dniIndex: Int) : IdentifiableEvt
data class AddedQACheckDocumentsArtifactOutputsEvt(override val id: String, val qacd: QACheckDocument) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
