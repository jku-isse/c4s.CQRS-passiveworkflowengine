package impactassessment.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CreateCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class CreatedEvt(val id: String, val amount: Int)
data class IncreaseCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IncreasedEvt(val id: String, val amount: Int)
data class DecreaseCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class DecreasedEvt(val id: String, val amount: Int)

data class FindQuery(val id: String)
data class FindResponse(val id: String, val amount: Int)

class AmountNegativeException(message: String) : Exception(message)

// Workflow example
data class CreateWorkflowCmd(@TargetAggregateIdentifier val id: String)
data class CreatedWorkflowEvt(val id: String)
data class EnableCmd(@TargetAggregateIdentifier val id: String, val dniNumber: Int)
data class EnabledEvt(val id: String, val dniNumber: Int)
data class CompleteCmd(@TargetAggregateIdentifier val id: String)
data class CompletedEvt(val id: String)

data class PrintQuery(val id: String)
