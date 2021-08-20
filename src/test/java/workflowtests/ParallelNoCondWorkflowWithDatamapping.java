package workflowtests;

import artifactapi.ArtifactType;
import passiveprocessengine.definition.AbstractWorkflowDefinition;
import passiveprocessengine.definition.ArtifactTypes;
import passiveprocessengine.definition.DecisionNodeDefinition;
import passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import passiveprocessengine.definition.DecisionNodeDefinition.OutFlowType;
import passiveprocessengine.definition.TaskDefinition;
import passiveprocessengine.instance.WorkflowInstance;

/**
 * workflow:
 *
 * DND TD DND TD DND -- DOC -- --REP -- kickoff ---- OPEN ---- open2closed ----
 * CLOSED ----- end
 *
 */
public class ParallelNoCondWorkflowWithDatamapping extends AbstractWorkflowDefinition {

	public static final String WORKFLOW_TYPE = "Parallel-Workflow";

	public static final String TASK_STATE_OPEN = "Prepare Implementation";
	public static final String TASK_STATE_DOC = "Documenting";
	public static final String TASK_STATE_CLOSED = "Conduct Implementation";
	public static final String TASK_STATE_REPORTED = "Reporting";
	public static final String ROLE_TASK = "task";
	public static final String ROLE_DOC = "doc";

	public ParallelNoCondWorkflowWithDatamapping() {
		super(WORKFLOW_TYPE);
	}

	@Override
	public WorkflowInstance createInstance(String id) {
		initWorkflowSpecification();
		return new WorkflowInstance(id, this, pub);
	}

	private void initWorkflowSpecification() {
		DecisionNodeDefinition begin = getWfKickOff();
		DecisionNodeDefinition middle = getOpen2Closed();
		DecisionNodeDefinition end = getClosed2End();

		TaskDefinition tdOpen = getStateOpenTaskDefinition(begin, middle);
		taskDefinitions.add(tdOpen);
		TaskDefinition tdDoc = getDocTD(begin, middle);
		taskDefinitions.add(tdDoc);
		TaskDefinition tdRep = getReportTD(middle, end);
		taskDefinitions.add(tdRep);
		TaskDefinition tdClosed = getStateClosedTaskDefinition(middle, end);
		taskDefinitions.add(tdClosed);

		dnds.add(begin);
		dnds.add(middle);
		dnds.add(end);

		this.putExpectedInput(ROLE_TASK, new ArtifactType("IJiraArtifact"));
		this.putExpectedInput("requirement", new ArtifactType("IJamaArtifact"));
		this.putExpectedOutput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
	}

	/*
	 * -- wpticket> [ Open ] ------ -wpticket- [ Closed ] -- in ticket -- < > OR /
	 * SYNC < > AND --- doc2out -- wpticket> [ doc ] ---doc-- -doc - [ Report ]
	 * -doc-
	 */

	private TaskDefinition getStateOpenTaskDefinition(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASK_STATE_OPEN, this, false, true, inDND, outDND);
		td.putExpectedInput(ROLE_TASK, new ArtifactType("IJiraArtifact"));
		// no output --> requires external completion event
		return td;
	}

	private TaskDefinition getDocTD(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASK_STATE_DOC, this, inDND, outDND);
		td.putExpectedInput(ROLE_TASK, new ArtifactType("IJiraArtifact"));
		td.putExpectedOutput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
		return td;
	}

	private TaskDefinition getStateClosedTaskDefinition(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASK_STATE_CLOSED, this, false, true, inDND, outDND);
		td.putExpectedInput(ROLE_TASK, new ArtifactType("IJiraArtifact"));
		// no output --> requires external completion event
		return td;
	}

	private TaskDefinition getReportTD(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASK_STATE_REPORTED, this, inDND, outDND);
		td.putExpectedInput(ROLE_DOC, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_QA_CHECK_DOCUMENT));
		// no output --> requires external completion event
		return td;
	}

	private DecisionNodeDefinition getWfKickOff() {
		DecisionNodeDefinition dnd = new DecisionNodeDefinition("workflowKickOff", this);
		dnd.setOutBranchingType(OutFlowType.SYNC);
		dnd.addMapping(WORKFLOW_TYPE, ROLE_TASK, TASK_STATE_OPEN, ROLE_TASK);
		dnd.addMapping(WORKFLOW_TYPE, ROLE_TASK, TASK_STATE_DOC, ROLE_TASK);
		return dnd;
	}

	private DecisionNodeDefinition getOpen2Closed() {
		DecisionNodeDefinition dnd = new DecisionNodeDefinition("open2closed", this);
		dnd.setInBranchingType(InFlowType.OR); // whoever is done first, triggers next steps
		dnd.setOutBranchingType(OutFlowType.SYNC);

		dnd.addMapping(WORKFLOW_TYPE, ROLE_TASK, TASK_STATE_CLOSED, ROLE_TASK);
		dnd.addMapping(TASK_STATE_DOC, ROLE_DOC, TASK_STATE_REPORTED, ROLE_DOC);
		return dnd;
	}

	private DecisionNodeDefinition getClosed2End() {
		DecisionNodeDefinition dnd = new DecisionNodeDefinition("closed2end", this);
		dnd.setInBranchingType(InFlowType.AND);
		dnd.addMapping(TASK_STATE_REPORTED, ROLE_DOC, WORKFLOW_TYPE, ROLE_DOC);
		return dnd;
	}
}