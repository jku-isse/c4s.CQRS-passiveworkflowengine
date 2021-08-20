package workflowtests;

import java.util.Arrays;
import java.util.List;

import artifactapi.ArtifactType;
import passiveprocessengine.definition.AbstractWorkflowDefinition;
import passiveprocessengine.definition.DecisionNodeDefinition;
import passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import passiveprocessengine.definition.DecisionNodeDefinition.OutFlowType;
import passiveprocessengine.definition.NoOpTaskDefinition;
import passiveprocessengine.definition.TaskDefinition;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.instance.WorkflowInstance;

public class ComplexXorWorkflow extends AbstractWorkflowDefinition implements WorkflowDefinition {

	public static final String WORKFLOW_TYPE = "ComplexWorkflowForVisualization";

	public static enum ART {
		ARTIFACT_TYPE_JIRA_TICKET, ARTIFACT_TYPE_RELEASE, ARTIFACT_TYPE_MODEL, ARTIFACT_TYPE_PROOF,
		ARTIFACT_TYPE_SIMULATION, ARTIFACT_TYPE_REVIEW, ARTIFACT_TYPE_RESOURCE_LINK
	};

	public static enum TASKS {
		ImpactAssessment, Designing, Specifying, Documenting, Modeling, Proofing, Simulating, Coding, ManualTesting,
		AutomatedTesting, NoTesting
	};

	public ComplexXorWorkflow() {
		super(WORKFLOW_TYPE);
	}

	@Override
	public WorkflowInstance createInstance(String id) {
		initWorkflowSpecification();
		return new WorkflowInstance(id, this, pub);
	}

	public void initWorkflowSpecification() {
		this.getExpectedInput().put("INPUT_ROLE_WPTICKET", new ArtifactType(ART.ARTIFACT_TYPE_JIRA_TICKET.toString()));
		dnds.addAll(getDNDs());

		taskDefinitions.add(getSpec(dnds.get(0), dnds.get(1)));
		taskDefinitions.add(getImpactAssessment(dnds.get(0), dnds.get(1)));
		taskDefinitions.add(getDesign(dnds.get(1), dnds.get(2)));

		taskDefinitions.add(getModeling(dnds.get(1), dnds.get(5)));
		taskDefinitions.add(getProof(dnds.get(5), dnds.get(6)));
		taskDefinitions.add(getSimulation(dnds.get(5), dnds.get(6)));
		taskDefinitions.add(getNewNoOp(dnds.get(6), dnds.get(2)));

		taskDefinitions.add(getCoding(dnds.get(2), dnds.get(3)));
		taskDefinitions.add(getDocumenting(dnds.get(2), dnds.get(4)));

		taskDefinitions.add(getTesting(dnds.get(3), dnds.get(7)));
		taskDefinitions.add(getAutomatedTesting(dnds.get(3), dnds.get(7)));
		taskDefinitions.add(getNoTesting(dnds.get(3), dnds.get(7)));
		taskDefinitions.add(getNewNoOp(dnds.get(7), dnds.get(4)));
	}

	/*
	 * automated testing no testing spec design coding -3< manual testing >xor(7)-
	 * noop kickoff(0) < > 1 < > xor (2) < > end (4) impact assess proof documenting
	 * modeling -(5)< >or (6)- NoOp simulation
	 */

	private List<DecisionNodeDefinition> getDNDs() {
		DecisionNodeDefinition dnKickoff = new DecisionNodeDefinition("KickOff", this);
		dnKickoff.addMapping(WORKFLOW_TYPE, "INPUT_ROLE_WPTICKET", TASKS.Specifying.toString(), "INPUT_ROLE_WPTICKET");
		dnKickoff.addMapping(WORKFLOW_TYPE, "INPUT_ROLE_WPTICKET", TASKS.ImpactAssessment.toString(),
				"INPUT_ROLE_WPTICKET");
		DecisionNodeDefinition dns2d = new DecisionNodeDefinition("Spec2Design", this);
		DecisionNodeDefinition dnd2c = new DecisionNodeDefinition("Design2Coding", this);
		dnd2c.setInBranchingType(InFlowType.XOR);
		DecisionNodeDefinition dnc2t = new DecisionNodeDefinition("Coding2Testing", this);
		DecisionNodeDefinition dnEnd = new DecisionNodeDefinition("End", this);

		DecisionNodeDefinition dnm2p = new DecisionNodeDefinition("Model2ProofOrSimulate", this);
		dnm2p.addMapping(TASKS.Modeling.toString(), "OUTPUT_ROLE_MODEL", TASKS.Proofing.toString(), "INPUT_ROLE_MODEL");
		dnm2p.addMapping(TASKS.Modeling.toString(), "OUTPUT_ROLE_MODEL", TASKS.Simulating.toString(),
				"INPUT_ROLE_MODEL");
		dnm2p.setOutBranchingType(OutFlowType.ASYNC);
		DecisionNodeDefinition dnp2no = new DecisionNodeDefinition("ProofOrSimulate2NoOp", this);
		dnp2no.setInBranchingType(InFlowType.OR);
		DecisionNodeDefinition dnt2no = new DecisionNodeDefinition("Testing2NoOp", this);
		dnt2no.setInBranchingType(InFlowType.XOR);

		// 0 1 2 3 4 5 6 7
		return Arrays
				.asList(new DecisionNodeDefinition[] { dnKickoff, dns2d, dnd2c, dnc2t, dnEnd, dnm2p, dnp2no, dnt2no });
	}

	private int noopcount = 0;

	private TaskDefinition getNewNoOp(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new NoOpTaskDefinition("NoOp" + noopcount++, this, inDND, outDND);
		return td;
	}

	private TaskDefinition getImpactAssessment(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.ImpactAssessment.toString(), this, false, true, inDND, outDND);
		td.getExpectedInput().put("INPUT_ROLE_WPTICKET", new ArtifactType(ART.ARTIFACT_TYPE_JIRA_TICKET.toString()));
		return td;
	}

	private TaskDefinition getDocumenting(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Documenting.toString(), this, inDND, outDND);
		td.getExpectedInput().put("INPUT_ROLE_RELEASE", new ArtifactType(ART.ARTIFACT_TYPE_RELEASE.toString()));
		return td;
	}

	private TaskDefinition getModeling(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Modeling.toString(), this, inDND, outDND);
		td.getExpectedOutput().put("OUTPUT_ROLE_MODEL", new ArtifactType(ART.ARTIFACT_TYPE_MODEL.toString()));
		return td;
	}

	private TaskDefinition getProof(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Proofing.toString(), this, inDND, outDND);
		td.getExpectedInput().put("INPUT_ROLE_MODEL", new ArtifactType(ART.ARTIFACT_TYPE_MODEL.toString()));
		td.getExpectedOutput().put("OUTPUT_ROLE_PROOF", new ArtifactType(ART.ARTIFACT_TYPE_PROOF.toString()));
		return td;
	}

	private TaskDefinition getSimulation(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Simulating.toString(), this, inDND, outDND);
		td.getExpectedInput().put("INPUT_ROLE_MODEL", new ArtifactType(ART.ARTIFACT_TYPE_MODEL.toString()));
		td.getExpectedOutput().put("OUTPUT_ROLE_SIM", new ArtifactType(ART.ARTIFACT_TYPE_SIMULATION.toString()));
		return td;
	}

	private TaskDefinition getSpec(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Specifying.toString(), this, false, true, inDND, outDND);
		td.getExpectedInput().put("INPUT_ROLE_WPTICKET", new ArtifactType(ART.ARTIFACT_TYPE_JIRA_TICKET.toString()));
		return td;
	}

	private TaskDefinition getDesign(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Designing.toString(), this, false, true, inDND, outDND);
		td.getExpectedOutput().put("OUTPUT_ROLE_SPEC", new ArtifactType(ART.ARTIFACT_TYPE_RESOURCE_LINK.toString()));
		td.getExpectedOutput().put("OUTPUT_ROLE_SSDDREVIEW", new ArtifactType(ART.ARTIFACT_TYPE_REVIEW.toString()));
		return td;
	}

	private TaskDefinition getCoding(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.Coding.toString(), this, false, true, inDND, outDND);
		return td;
	}

	private TaskDefinition getTesting(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.ManualTesting.toString(), this, true, true, inDND, outDND);
		return td;
	}

	private TaskDefinition getAutomatedTesting(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.AutomatedTesting.toString(), this, false, true, inDND, outDND);
		return td;
	}

	private TaskDefinition getNoTesting(DecisionNodeDefinition inDND, DecisionNodeDefinition outDND) {
		TaskDefinition td = new TaskDefinition(TASKS.NoTesting.toString(), this, false, true, inDND, outDND);
		return td;
	}

}