//package impactassessment.exampleworkflows;
//
//import passiveprocessengine.definition.*;
//import passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
//import passiveprocessengine.definition.DecisionNodeDefinition.OutFlowType;
//import passiveprocessengine.instance.WorkflowInstance;
//
///**
// * workflow:
// *
// *       DND        TD          DND             TD
// *
// *    kickoff ---- OPEN ---- open2closed ---- CLOSED
// *
// */
//public class ParallelNoCondWorkflowWithDatamapping extends AbstractWorkflowDefinition {
//
//    public static final String WORKFLOW_TYPE = "LINEARWITHDATA_WORKFLOW_TYPE";
//
//    public static final String TASK_STATE_OPEN = "Open";
//    public static final String TASK_STATE_DOC = "Doc";
//    public static final String TASK_STATE_CLOSED = "Closed";
//    public static final String TASK_STATE_REPORTED = "Reported";
//    public static final String ROLE_WPTICKET = "ROLE_WPTICKET";
//    public static final String ROLE_DOC = "ROLE_DOCUMENTATION";
//
//    public ParallelNoCondWorkflowWithDatamapping() {
//        super(WORKFLOW_TYPE);
//    }
//
//    @Override
//    public WorkflowInstance createInstance(String id) {
//        initWorkflowSpecification();
//        return new WorkflowInstance(id, this, pub);
//    }
//
//    private void initWorkflowSpecification() {
//        TaskDefinition tdOpen = getStateOpenTaskDefinition();
//        taskDefinitions.add(tdOpen);
//        TaskDefinition tdDoc = getDocTD();
//        taskDefinitions.add(tdDoc);
//        TaskDefinition tdRep = getReportTD();
//        taskDefinitions.add(tdRep);
//        TaskDefinition tdClosed = getStateClosedTaskDefinition();
//        taskDefinitions.add(tdClosed);
//
//        dnds.add(getWfKickOff(tdOpen, tdDoc));
//        dnds.add(getOpen2Closed(tdOpen, tdDoc, tdClosed, tdRep));
//        dnds.add(getClosed2End(tdClosed, tdRep));
//
//        this.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//        this.putExpectedOutput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//    }
//
///*
// * 			    -- wpticket> [ Open ] ------				  -wpticket- [ Closed ] --
// * in ticket -- < 							  > OR / SYNC < 						   > AND --- doc2out
// * 				-- wpticket> [ doc ] ---doc--				  -doc - [ Report ]  -doc-
// */
//
//
//    private TaskDefinition getStateOpenTaskDefinition() {
//        TaskDefinition td = new TaskDefinition(TASK_STATE_OPEN, this);
//        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
//        // no output --> requires external completion event
//        return td;
//    }
//
//    private TaskDefinition getDocTD() {
//        TaskDefinition td = new TaskDefinition(TASK_STATE_DOC, this);
//        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//        td.putExpectedOutput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//        return td;
//    }
//
//    private TaskDefinition getStateClosedTaskDefinition() {
//        TaskDefinition td = new TaskDefinition(TASK_STATE_CLOSED, this);
//        td.putExpectedInput(ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//        // no output --> requires external completion event
//        return td;
//    }
//
//    private TaskDefinition getReportTD() {
//        TaskDefinition td = new TaskDefinition(TASK_STATE_REPORTED, this);
//        td.putExpectedInput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//     // no output --> requires external completion event
//        return td;
//    }
//
//    private DecisionNodeDefinition getWfKickOff(TaskDefinition tdOpen, TaskDefinition tdDoc) {
//        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
//                "workflowKickOff",
//                this,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE);
//        dnd.setOutBranchingType(OutFlowType.SYNC);
//        dnd.addOutBranchDefinition(new DefaultBranchDefinition("openIn", tdOpen, false, false, dnd));
//        dnd.addOutBranchDefinition(new DefaultBranchDefinition("docIn", tdDoc, false, false, dnd));
//        dnd.addMapping(WORKFLOW_TYPE, ROLE_WPTICKET, TASK_STATE_OPEN, ROLE_WPTICKET);
//        dnd.addMapping(WORKFLOW_TYPE, ROLE_WPTICKET, TASK_STATE_DOC, ROLE_WPTICKET);
//        return dnd;
//    }
//
//    private DecisionNodeDefinition getOpen2Closed(TaskDefinition tdOpen, TaskDefinition tdDoc, TaskDefinition tdClosed, TaskDefinition tdRep) {
//        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
//                "open2closed",
//                this,
//                DecisionNodeDefinition.HAVING_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE);
//        dnd.setInBranchingType(InFlowType.OR); // whoever is done first, triggers next steps
//        dnd.setOutBranchingType(OutFlowType.SYNC);
//        dnd.addInBranchDefinition(new DefaultBranchDefinition("openOut", tdOpen, false, false, dnd));
//        dnd.addInBranchDefinition(new DefaultBranchDefinition("docOut", tdDoc, false, false, dnd));
//        dnd.addOutBranchDefinition(new DefaultBranchDefinition("closedIn", tdClosed, false, false, dnd));
//        dnd.addOutBranchDefinition(new DefaultBranchDefinition("repIn", tdRep, false, false, dnd));
//
//        dnd.addMapping(WORKFLOW_TYPE, ROLE_WPTICKET, TASK_STATE_CLOSED, ROLE_WPTICKET);
//        dnd.addMapping(TASK_STATE_DOC, ROLE_DOC, TASK_STATE_REPORTED, ROLE_DOC);
//        return dnd;
//    }
//
//    private DecisionNodeDefinition getClosed2End(TaskDefinition tdClosed, TaskDefinition tdRep) {
//        DecisionNodeDefinition dnd = new DecisionNodeDefinition(
//                "closed2end",
//                this,
//                DecisionNodeDefinition.HAVING_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE,
//                DecisionNodeDefinition.NO_EXTERNAL_RULE);
//        dnd.setInBranchingType(InFlowType.AND);
//        dnd.addInBranchDefinition(new DefaultBranchDefinition("closedOut", tdClosed, false, false, dnd));
//        dnd.addInBranchDefinition(new DefaultBranchDefinition("repOut", tdRep, false, false, dnd));
//        dnd.addMapping(TASK_STATE_REPORTED, ROLE_DOC, WORKFLOW_TYPE, ROLE_DOC);
//        return dnd;
//    }
//}
