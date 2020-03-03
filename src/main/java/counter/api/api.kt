package counter.api

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

