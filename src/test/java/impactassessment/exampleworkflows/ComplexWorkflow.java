package impactassessment.exampleworkflows;


import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

/**
 *  workflow:
 *
 *      DND                  TD                 DND                     TD
 *
 *                    __ TASK_OPEN __
 *                  /                \                         ___ TASK_CLOSED
 *     kickoff_____/____ DD_OPEN _____\____ open2closed _____/
 *                 \                 /                       \___ REQ_WORKING
 *                  \___ REQ_OPEN __/
 *
 *
 */
public class ComplexWorkflow extends AbstractWorkflowDefinition {

    public static final String WORKFLOW_TYPE = "TEST_WORKFLOW_TYPE";

    // TaskDefinition IDs
    public static final String TD_TASK_OPEN = "TaskOpen";
    public static final String TD_DD_OPEN = "DDOpen";
    public static final String TD_REQ_OPEN = "ReqOpen";
    public static final String TD_TASK_CLOSED = "TaskClosed";
    public static final String TD_REQ_WORKING = "ReqWorking";
    // Branch IDs
    public static final String BRANCH_TASK_OPEN_IN = "taskOpenIn";
    public static final String BRANCH_DD_OPEN_IN = "ddOpenIn";
    public static final String BRANCH_REQ_OPEN_IN = "reqOpenIn";
    public static final String BRANCH_TASK_OPEN_OUT = "taskOpenOut";
    public static final String BRANCH_DD_OPEN_OUT = "ddOpenOut";
    public static final String BRANCH_REQ_OPEN_OUT = "reqOpenOut";
    public static final String BRANCH_TASK_CLOSED_IN = "taskClosedIn";
    public static final String BRANCH_REQ_WORKING_IN = "reqWorkingIn";
    // DecisionNodeDefinition IDs
    public static final String DND_KICKOFF = "workflowKickOff";
    public static final String DND_OPEN2CLOSED = "open2closed";

    public static final String ROLE_WPTICKET = "ROLE_WPTICKET";
    public static final String ROLE_QA_CHECK_DOC = "QA_PROCESS_CONSTRAINTS_CHECK";

    public ComplexWorkflow() {
        super(WORKFLOW_TYPE);
    }

    @Override
    public WorkflowInstance createInstance(String id) {
        TaskDefinition tdTaskOpen = getStateTaskOpenTaskDefinition();
        taskDefinitions.add(tdTaskOpen);
        TaskDefinition tdDDOpen = getStateDDOpenTaskDefinition();
        taskDefinitions.add(tdDDOpen);
        TaskDefinition tdReqOpen = getStateReqOpenTaskDefinition();
        taskDefinitions.add(tdReqOpen);
        TaskDefinition tdTaskClosed = getStateTaskClosedTaskDefinition();
        taskDefinitions.add(tdTaskClosed);
        TaskDefinition tdReqWorking = getStateReqWorkingTaskDefinition();
        taskDefinitions.add(tdReqWorking);

        dnds.add(getWfKickOff(tdTaskOpen, tdDDOpen, tdReqOpen));
        dnds.add(getOpen2Closed(tdTaskOpen, tdDDOpen, tdReqOpen, tdTaskClosed, tdReqWorking));
        return new WorkflowInstance(id, this, pub);
    }

    private TaskDefinition getStateTaskOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TD_TASK_OPEN, this);
        return td;
    }

    private TaskDefinition getStateDDOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TD_DD_OPEN, this);
        return td;
    }

    private TaskDefinition getStateReqOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TD_REQ_OPEN, this);
        return td;
    }

    private TaskDefinition getStateTaskClosedTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TD_TASK_CLOSED, this);
        // define expected Inputs:
        td.putExpectedInput(ROLE_QA_CHECK_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        return td;
    }

    private TaskDefinition getStateReqWorkingTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TD_REQ_WORKING, this);
        // define expected Inputs:
        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        return td;
    }

    private DecisionNodeDefinition getWfKickOff(TaskDefinition tdTaskOpen, TaskDefinition tdDDOpen, TaskDefinition tdReqOpen) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                DND_KICKOFF,
                this,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addOutBranchDefinition(new DefaultBranchDefinition(BRANCH_TASK_OPEN_IN, tdTaskOpen, false, true, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition(BRANCH_DD_OPEN_IN, tdDDOpen, false, true, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition(BRANCH_REQ_OPEN_IN, tdReqOpen, false, true, dnd));
        return dnd;
    }

    private DecisionNodeDefinition getOpen2Closed(TaskDefinition tdTaskOpen, TaskDefinition tdDDOpen, TaskDefinition tdReqOpen, TaskDefinition tdTaskClosed, TaskDefinition tdReqWorking) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                DND_OPEN2CLOSED,
                this,
                DecisionNodeDefinition.HAVING_EXTERNAL_RULE, // TODO: not sure about this
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        // incoming
        dnd.addInBranchDefinition(new DefaultBranchDefinition(BRANCH_TASK_OPEN_OUT, tdTaskOpen, false, true, dnd));
        dnd.addInBranchDefinition(new DefaultBranchDefinition(BRANCH_DD_OPEN_OUT, tdDDOpen, false, true, dnd));
        dnd.addInBranchDefinition(new DefaultBranchDefinition(BRANCH_REQ_OPEN_OUT, tdReqOpen, false, true, dnd));
        // outgoing
        dnd.addOutBranchDefinition(new DefaultBranchDefinition(BRANCH_TASK_CLOSED_IN, tdTaskClosed, false, true, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition(BRANCH_REQ_WORKING_IN, tdReqWorking, false, true, dnd));
        return dnd;
    }

}
