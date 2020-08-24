package impactassessment.api

import impactassessment.passiveprocessengine.definition.CorrelationTuple
import impactassessment.jiraartifact.IJiraArtifact
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper
import impactassessment.passiveprocessengine.workflowmodel.AbstractWorkflowDefinition
import impactassessment.passiveprocessengine.workflowmodel.ResourceLink
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

// COMMANDS
data class AddMockArtifactCmd(@TargetAggregateIdentifier val id: String, val status: String, val issuetype: String, val priority: String, val summary: String)
data class ImportOrUpdateArtifactCmd(@TargetAggregateIdentifier val id: String, val source: Sources)
data class ImportOrUpdateArtifactWithWorkflowDefinitionCmd(@TargetAggregateIdentifier val id: String, val source: Sources, val wfd: AbstractWorkflowDefinition)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val res: ResourceLink)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val branchId: String)
data class ActivateInOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String, val branchId: String)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)
data class AddQAConstraintCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val status: String, val ruleName: String, val description: String)
data class AddConstraintsCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val rules: Map<String, String>)
data class AddResourceToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple, val time: Instant)
data class AddResourcesToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val res: Map<ResourceLink, Boolean>, val corr: CorrelationTuple, val time: Instant)
data class CheckConstraintCmd(@TargetAggregateIdentifier val id: String, val corrId: String)
data class CheckAllConstraintsCmd(@TargetAggregateIdentifier val id: String)
data class PrintKBCmd(@TargetAggregateIdentifier val id: String)

// EVENTS
interface IdentifiableEvt{val id: String}

data class ImportedOrUpdatedArtifactEvt(override val id: String, val artifact: IJiraArtifact) : IdentifiableEvt
data class ImportedOrUpdatedArtifactWithWorkflowDefinitionEvt(override val id: String, val artifact: IJiraArtifact, val wfd: AbstractWorkflowDefinition) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dniId: String, val res: ResourceLink) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dniId: String, val wftId: String) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dniId: String, val branchId: String) : IdentifiableEvt
data class ActivatedInOutBranchEvt(override val id: String, val dniId: String, val wftId: String, val branchId: String) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt
data class AddedQAConstraintEvt(override val id: String, val wftId: String, val status: String, val ruleName: String, val description: String) : IdentifiableEvt
data class AddedConstraintsEvt(override val id: String, val wftId: String, val rules: Map<String, String>) : IdentifiableEvt
data class AddedResourceToConstraintEvt(override val id: String, val qacId: String, val fulfilled: Boolean, val res: ResourceLink, val corr: CorrelationTuple, val time: Instant) : IdentifiableEvt
data class AddedResourcesToConstraintEvt(override val id: String, val qacId: String, val res: Map<ResourceLink, Boolean>, val corr: CorrelationTuple, val time: Instant) : IdentifiableEvt

// QUERIES
data class FindQuery(val id: String)
data class GetStateQuery(val depth: Int)

// QUERY-RESPONSES
data class FindResponse(val id: String, val amount: Int)
data class GetStateResponse(val state: List<WorkflowInstanceWrapper>)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
