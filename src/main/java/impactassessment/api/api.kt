package impactassessment.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

// COMMANDS
data class CreateWorkflowCmd(@TargetAggregateIdentifier val id: String)
data class EnableCmd(@TargetAggregateIdentifier val id: String, val dniNumber: Int)
data class CompleteCmd(@TargetAggregateIdentifier val id: String)
data class DeleteCommand(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class CreatedWorkflowEvt(override val id: String) : IdentifiableEvt
data class EnabledEvt(override val id: String, val dniNumber: Int) : IdentifiableEvt
data class CompletedEvt(override val id: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
