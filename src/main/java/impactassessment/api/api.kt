package impactassessment.api

import impactassessment.jiraartifact.IJiraArtifact
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper
import org.axonframework.modelling.command.TargetAggregateIdentifier
import passiveprocessengine.definition.Artifact
import passiveprocessengine.definition.ArtifactType
import passiveprocessengine.definition.WorkflowDefinition
import passiveprocessengine.instance.ArtifactInput
import passiveprocessengine.instance.ArtifactOutput
import passiveprocessengine.instance.CorrelationTuple
import passiveprocessengine.instance.ResourceLink

import java.time.Instant

// COMMANDS
data class CreateMockWorkflowCmd(@TargetAggregateIdentifier val id: String, val status: String, val issuetype: String, val priority: String, val summary: String)
data class CreateDefaultWorkflowCmd(@TargetAggregateIdentifier val id: String, val input: Map<String, String>)
data class CreateWorkflowCmd(@TargetAggregateIdentifier val id: String, val input: Map<String, String>, val definitionName: String)
data class CreateSubWorkflowCmd(@TargetAggregateIdentifier val id: String, val parentWfiId: String, val parentWftId: String, val definitionName: String)
data class CompleteDataflowCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val res: ResourceLink)
data class ActivateInBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String)
data class ActivateOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val branchId: String)
data class ActivateInOutBranchCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String, val branchId: String)
data class ActivateInOutBranchesCmd(@TargetAggregateIdentifier val id: String, val dniId: String, val wftId: String, val branchIds: Set<String>)
data class DeleteCmd(@TargetAggregateIdentifier val id: String)
data class AddConstraintsCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val rules: Map<String, String>)
data class AddEvaluationResultToConstraintCmd(@TargetAggregateIdentifier val id: String, val qacId: String, val res: Map<ResourceLink, Boolean>, val corr: CorrelationTuple, val time: Instant)
data class CheckConstraintCmd(@TargetAggregateIdentifier val id: String, val corrId: String)
data class CheckAllConstraintsCmd(@TargetAggregateIdentifier val id: String)
data class AddInputCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val artifact: Artifact, val role: String, val type: ArtifactType)
data class AddOutputCmd(@TargetAggregateIdentifier val id: String, val wftId: String, val artifact: Artifact, val role: String, val type: ArtifactType)
data class AddInputToWorkflowCmd(@TargetAggregateIdentifier val id: String, val input: ArtifactInput)
data class AddOutputToWorkflowCmd(@TargetAggregateIdentifier val id: String, val output: ArtifactOutput)
data class UpdateArtifactsCmd(@TargetAggregateIdentifier val id: String, val artifacts: List<IJiraArtifact>)

// EVENTS
interface IdentifiableEvt{val id: String}

data class CreatedDefaultWorkflowEvt(override val id: String, val artifacts: List<IJiraArtifact>) : IdentifiableEvt
data class CreatedWorkflowEvt(override val id: String, val artifacts: List<IJiraArtifact>, val definitionName: String, val wfd: WorkflowDefinition) : IdentifiableEvt
data class CreatedSubWorkflowEvt(override val id: String, val parentWfiId: String, val parentWftId: String, val definitionName: String, val wfd: WorkflowDefinition) : IdentifiableEvt
data class CompletedDataflowEvt(override val id: String, val dniId: String, val res: ResourceLink) : IdentifiableEvt
data class ActivatedInBranchEvt(override val id: String, val dniId: String, val wftId: String) : IdentifiableEvt
data class ActivatedOutBranchEvt(override val id: String, val dniId: String, val branchId: String) : IdentifiableEvt
data class ActivatedInOutBranchEvt(override val id: String, val dniId: String, val wftId: String, val branchId: String) : IdentifiableEvt
data class ActivatedInOutBranchesEvt(override val id: String, val dniId: String, val wftId: String, val branchIds: Set<String>) : IdentifiableEvt
data class DeletedEvt(override val id:String) : IdentifiableEvt
data class AddedConstraintsEvt(override val id: String, val wftId: String, val rules: Map<String, String>) : IdentifiableEvt
data class AddedEvaluationResultToConstraintEvt(override val id: String, val qacId: String, val res: Map<ResourceLink, Boolean>, val corr: CorrelationTuple, val time: Instant) : IdentifiableEvt
data class CheckedConstraintEvt(override val id: String, val corrId: String) : IdentifiableEvt
data class CheckedAllConstraintsEvt(override val id: String) : IdentifiableEvt
data class AddedInputEvt(override val id: String, val wftId: String, val artifact: Artifact, val role: String, val type: ArtifactType) : IdentifiableEvt
data class AddedOutputEvt(override val id: String, val wftId: String, val artifact: Artifact, val role: String, val type: ArtifactType) : IdentifiableEvt
data class AddedInputToWorkflowEvt(override val id: String, val input: ArtifactInput) : IdentifiableEvt
data class AddedOutputToWorkflowEvt(override val id: String, val output: ArtifactOutput) : IdentifiableEvt
data class UpdatedArtifactsEvt(override val id: String, val artifacts: List<IJiraArtifact>) : IdentifiableEvt

// QUERIES
data class GetStateQuery(val depth: Int)
data class PrintKBQuery(val id: String)

// QUERY-RESPONSES
data class GetStateResponse(val state: List<WorkflowInstanceWrapper>)
data class PrintKBResponse(val kbString: String)

// EXCEPTIONS
class SomeException(message: String) : Exception(message)
