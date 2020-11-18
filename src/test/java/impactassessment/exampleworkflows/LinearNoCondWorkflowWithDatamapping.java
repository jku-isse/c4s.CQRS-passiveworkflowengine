package impactassessment.exampleworkflows;

import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

/**
 * workflow:
 *
 *       DND        TD          DND             TD
 *
 *    kickoff ---- OPEN ---- open2closed ---- CLOSED
 *
 */
public class LinearNoCondWorkflowWithDatamapping extends AbstractWorkflowDefinition {

    public static final String WORKFLOW_TYPE = "LINEARWITHDATA_WORKFLOW_TYPE";

    public static final String TASK_STATE_OPEN = "Open";
    public static final String TASK_STATE_CLOSED = "Closed";
    public static final String ROLE_WPTICKET = "ROLE_WPTICKET";


    public LinearNoCondWorkflowWithDatamapping() {
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
        dnds.add(getClosed2End(tdClosed));
        
        this.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        this.putExpectedOutput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
    }

    private TaskDefinition getStateOpenTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_OPEN, this);
        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        td.putExpectedOutput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        return td;
    }

    private TaskDefinition getStateClosedTaskDefinition() {
        TaskDefinition td = new TaskDefinition(TASK_STATE_CLOSED, this);
        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        td.putExpectedOutput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
        return td;
    }

    private DecisionNodeDefinition getWfKickOff(TaskDefinition tdOpen) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "workflowKickOff",
                this,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("openIn", tdOpen, false, false, dnd));
        dnd.addMapping(WORKFLOW_TYPE, ROLE_WPTICKET, TASK_STATE_OPEN, ROLE_WPTICKET);
        return dnd;
    }

    private DecisionNodeDefinition getOpen2Closed(TaskDefinition tdOpen, TaskDefinition tdClosed) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "open2closed",
                this,
                DecisionNodeDefinition.HAVING_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addInBranchDefinition(new DefaultBranchDefinition("openOut", tdOpen, true, false, dnd));
        dnd.addOutBranchDefinition(new DefaultBranchDefinition("closedIn", tdClosed, false, false, dnd));
        dnd.addMapping(TASK_STATE_OPEN, ROLE_WPTICKET, TASK_STATE_CLOSED, ROLE_WPTICKET);
        return dnd;
    }
    
    private DecisionNodeDefinition getClosed2End(TaskDefinition tdClosed) {
        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
                "closed2end",
                this,
                DecisionNodeDefinition.HAVING_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE,
                DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dnd.addInBranchDefinition(new DefaultBranchDefinition("closedOut", tdClosed, false, false, dnd));
        dnd.addMapping(TASK_STATE_CLOSED, ROLE_WPTICKET, WORKFLOW_TYPE, ROLE_WPTICKET);
        return dnd;
    }
}
