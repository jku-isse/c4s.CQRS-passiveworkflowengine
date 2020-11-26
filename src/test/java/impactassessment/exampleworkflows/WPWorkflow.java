package impactassessment.exampleworkflows;

import com.google.inject.Inject;
import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

import java.util.UUID;

public class WPWorkflow extends AbstractWorkflowDefinition {

	public static final String WORKFLOW_TYPE = "WP_WORKFLOW_TYPE";	
	
	public static final String TASKTYPE_WP_EXECUTION = "WP_EXECUTION";
	
	public static final String INPUT_ROLE_WPTICKET = "INPUT_ROLE_WPTICKET";
	public static final String OUTPUT_ROLE_SSSREVIEW = "OUTPUT_ROLE_SSSREVIEW";
	public static final String OUTPUT_ROLE_SSDDREVIEW = "OUTPUT_ROLE_SSDDREVIEW";
	public static final String OUTPUT_ROLE_FEDDREVIEW = "OUTPUT_ROLE_FEDDREVIEW";
	public static final String OUTPUT_ROLE_SSS_SRS_MAPPINGREVIEW = "OUTPUT_ROLE_SSS_SRS_MAPPINGREVIEW";
	

	public WPWorkflow() {
		super(WORKFLOW_TYPE);		
	}

	@Inject
	public void initWorkflowSpecification() {
		TaskDefinition tdWPExecution = getWPExecutionTaskDefinition();
		taskDefinitions.add(tdWPExecution);
		dnds.add(getWPKickOff(tdWPExecution));
	}
	
	private TaskDefinition getWPExecutionTaskDefinition() {
		TaskDefinition td = new TaskDefinition(TASKTYPE_WP_EXECUTION, this);
		td.getExpectedInput().put(INPUT_ROLE_WPTICKET, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_JIRA_TICKET));
		td.getExpectedOutput().put(OUTPUT_ROLE_SSSREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
		td.getExpectedOutput().put(OUTPUT_ROLE_SSDDREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
		td.getExpectedOutput().put(OUTPUT_ROLE_FEDDREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
		td.getExpectedOutput().put(OUTPUT_ROLE_SSS_SRS_MAPPINGREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
		return td;
	}
	
	
	
	private DecisionNodeDefinition getWPKickOff(TaskDefinition tdWP) {
		DecisionNodeDefinition dn = new DecisionNodeDefinition("WPKickOff", this, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
		dn.addOutBranchDefinition(new DefaultBranchDefinition("WPin", tdWP, false, true, dn));
		return dn;
	}
	
	


	@Override
	public WorkflowInstance createInstance(String withOptionalId) {
		String wfid = withOptionalId != null ? withOptionalId : this.id+"#"+UUID.randomUUID().toString();
		WorkflowInstance wfi = new WorkflowInstance(wfid, this, pub);
		return wfi;
	}


	
}
