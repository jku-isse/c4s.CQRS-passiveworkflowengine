package impactassessment.exampleworkflows;

import com.google.inject.Inject;
import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.UUID;

public class DronologyWorkflow extends AbstractWorkflowDefinition {

    public static final String WORKFLOW_TYPE = "DRONOLOGY_WORKFLOW_TYPE";

    public static final String TASK_STATE_OPEN = "Open";
    public static final String TASK_STATE_IN_PROGRESS = "In Progress";
    public static final String TASK_STATE_RESOLVED = "Resolved";

    public static final String ROLE_WPTICKET = "ROLE_WPTICKET";
    public static final String ROLE_QA_CHECK_DOC = "QA_PROCESS_CONSTRAINTS_CHECK";
    public static final String INPUT_ROLE_DESIGN_DEFINITION = "INPUT_ROLE_DESIGN_DEFINITION";
    public static final String INPUT_ROLE_REQUIREMENT = "INPUT_ROLE_REQUIREMENT";
    public static final String OUTPUT_ROLE_TEST = "OUTPUT_ROLE_TEST";

    public DronologyWorkflow(){
        super(WORKFLOW_TYPE);
        initWorkflowSpecification();
    }

    @Inject
    public void initWorkflowSpecification() {
        putExpectedInput("JIRA", new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET));

        TaskDefinition tdOpen = getStateOpenTaskDefinition();
        taskDefinitions.add(tdOpen);
        TaskDefinition tdInProgress = getStateInProgressTaskDefinition();
        taskDefinitions.add(tdInProgress);
        TaskDefinition tdResolved = getStateResolvedTaskDefinition();
        taskDefinitions.add(tdResolved);

        dnds.add(getWfKickOff(tdOpen));
        dnds.add(getOpen2InProgressOrResolved(tdOpen, tdInProgress, tdResolved));
        dnds.add(getInProgress2Resolved(tdInProgress, tdResolved));
    }

    private TaskDefinition getStateOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_OPEN, this);
        td.getExpectedInput().put(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET));
        td.getExpectedInput().put(INPUT_ROLE_DESIGN_DEFINITION, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        td.getExpectedInput().put(INPUT_ROLE_REQUIREMENT, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        return td;
    }
    private TaskDefinition getStateInProgressTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_IN_PROGRESS, this);
        td.getExpectedInput().put(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET));
        td.getExpectedInput().put(INPUT_ROLE_DESIGN_DEFINITION, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        td.getExpectedInput().put(INPUT_ROLE_REQUIREMENT, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        return td;
    }
    private TaskDefinition getStateResolvedTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_RESOLVED, this);
        td.getExpectedInput().put(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET));
        td.getExpectedInput().put(INPUT_ROLE_DESIGN_DEFINITION, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        td.getExpectedInput().put(INPUT_ROLE_REQUIREMENT, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        td.getExpectedOutput().put(OUTPUT_ROLE_TEST, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        return td;
    }

    private DecisionNodeDefinition getWfKickOff(TaskDefinition tdOpen) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition("workflowKickOff", this, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("OpenIn", tdOpen, false, false, dnd));
        return dnd;
    }
    private DecisionNodeDefinition getOpen2InProgressOrResolved(TaskDefinition tdOpen, TaskDefinition tdInProgress, TaskDefinition tdResolved) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition("open2inProgressOrResolved", this, DecisionNodeDefinition.HAVING_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addInBranchDefinition(new DefaultBranchDefinition("openOut", tdOpen, false, false, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("inProgressIn", tdInProgress, true, false, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("resolvedIn", tdResolved, true, false, dnd));
        dnd.setOutBranchingType(DecisionNodeDefinition.OutFlowType.ASYNC);
        return dnd;
    }
    private DecisionNodeDefinition getInProgress2Resolved(TaskDefinition tdInProgress, TaskDefinition tdResolved) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition("inProgress2resolved", this, DecisionNodeDefinition.HAVING_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addInBranchDefinition(new DefaultBranchDefinition("inProgressOut", tdInProgress, false, false, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("resolvedIn", tdResolved, false, false, dnd));
        return dnd;
    }

    @Override
    public WorkflowInstance createInstance(String withOptionalId) {
        String wfid = withOptionalId != null ? withOptionalId : this.id+"#"+UUID.randomUUID().toString();
        WorkflowInstance wfi = new WorkflowInstance(wfid, this, pub);
        return wfi;
    }

}
