package impactassessment.exampleworkflows;

import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

public class UncleanWorkflow extends AbstractWorkflowDefinition {

    public static final String WORKFLOW_TYPE = "UNCLEAN_TEST_WORKFLOW_TYPE";

    public static final String TASK_STATE_OPEN = "Open";
    public static final String TASK_STATE_CLOSED = "Closed";
    public static final String ROLE_WPTICKET = "ROLE_WPTICKET";

    public UncleanWorkflow() {
        super(WORKFLOW_TYPE);
    }

    @Override
    public WorkflowInstance createInstance(String id) {
        initWorkflowSpecification();
        return new WorkflowInstance(id, this, pub);
    }

    private void initWorkflowSpecification() {
        TaskDefinition tdOpen = getStateOpenTaskDefinition();
        taskDefinitions.add(tdOpen);
        TaskDefinition tdClosed = getStateClosedTaskDefinition();
        taskDefinitions.add(tdClosed);

        dnds.add(getWfKickOff(tdOpen));
        dnds.add(getOpen2Closed(tdOpen, tdClosed));
        dnds.add(getUnconnected());
    }

    private TaskDefinition getStateOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_OPEN, this);
        return td;
    }

    private TaskDefinition getStateClosedTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_CLOSED, this);
        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        return td;
    }

    private DecisionNodeDefinition getWfKickOff(TaskDefinition tdOpen) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "workflowKickOff",
                this,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("openIn", tdOpen, false, true, dnd));
        return dnd;
    }

    private DecisionNodeDefinition getOpen2Closed(TaskDefinition tdOpen, TaskDefinition tdClosed) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "open2closed",
                this,
                DecisionNodeDefinition.HAVING_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addInBranchDefinition(new DefaultBranchDefinition("openOut", tdOpen, false, true, dnd));
//        dnd.addOutBranchDefinition(new DefaultBranchDefinition("closedIn", tdClosed, false, true, dnd));
        dnd.addMapping(TASK_STATE_OPEN, "INPUT", TASK_STATE_CLOSED, "INPUT");
        return dnd;
    }

    private DecisionNodeDefinition getUnconnected() {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "unconnected",
                this,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        return dnd;
    }
}
