package impactassessment.api

import impactassessment.model.workflowmodel.WorkflowDefinition
import impactassessment.model.definition.QACheckDocument
import org.axonframework.modelling.command.TargetAggregateIdentifier

// COMMANDS
data class CreateWorkflowCmd(@TargetAggregateIdentifier val id: String)
data class CreateWorkflowInstanceOfCmd(@TargetAggregateIdentifier val id: String, val wfd: WorkflowDefinition)
data class EnableTasksAndDecisionsCmd(@TargetAggregateIdentifier val id: String)
data class CompleteDataflowOfDecisionNodeInstanceCmd(@TargetAggregateIdentifier val id: String, val dniIndex: Int)
data class AddQAConstraintsAsArtifactOutputsCmd(@TargetAggregateIdentifier val id: String, val qac: QACheckDocument.QAConstraint)
data class CreateConstraintTriggerCmd(@TargetAggregateIdentifier val id: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class CreatedWorkflowEvt(override val id: String) : IdentifiableEvt
data class CreatedWorkflowInstanceOfEvt(override val id: String, val wfd: WorkflowDefinition) : IdentifiableEvt
data class EnabledTasksAndDecisionsEvt(override val id: String) : IdentifiableEvt
data class CompletedDataflowOfDecisionNodeInstanceEvt(override val id: String, val dniIndex: Int) : IdentifiableEvt
data class AddedQAConstraintsAsArtifactOutputsEvt(override val id: String, val qac: QACheckDocument.QAConstraint) : IdentifiableEvt
data class CreatedConstraintTriggerEvt(override val id: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
