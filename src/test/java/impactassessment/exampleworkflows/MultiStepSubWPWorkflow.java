package impactassessment.exampleworkflows;

import passiveprocessengine.definition.*;
import passiveprocessengine.instance.WorkflowInstance;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MultiStepSubWPWorkflow extends AbstractWorkflowDefinition implements WorkflowDefinition {

    public static final String WORKFLOW_TYPE = "MULTISTEP_SubWP_WORKFLOW_TYPE";

    public static final String TASKTYPE_SUBWP_SPEC = "SubWP_SPEC";
    public static final String TASKTYPE_SUBWP_DESIGN = "SubWP_DESIGN";
    public static final String TASKTYPE_SUBWP_CODING = "SubWP_CODING";
    public static final String TASKTYPE_SUBWP_TESTING = "SubWP_TESTING";


    public MultiStepSubWPWorkflow() {
        super(WORKFLOW_TYPE);
    }

    @Inject
    public void initWorkflowSpecification() {
        List<TaskDefinition> tDefs = getWPExecutionTaskDefinitions();
        taskDefinitions.addAll(tDefs);
        dnds.add(getWPKickOff(tDefs.get(0)));
        dnds.add(getSpec2Design(tDefs.get(0), tDefs.get(1)));
        dnds.add(getDesign2Coding(tDefs.get(1), tDefs.get(2)));
        dnds.add(getCoding2Testing(tDefs.get(2), tDefs.get(3)));
    }

    private List<TaskDefinition> getWPExecutionTaskDefinitions() {
        TaskDefinition tdSpec = new TaskDefinition(TASKTYPE_SUBWP_SPEC, this);
        TaskDefinition tdDesign = new TaskDefinition(TASKTYPE_SUBWP_DESIGN, this);
        TaskDefinition tdCoding = new TaskDefinition(TASKTYPE_SUBWP_CODING, this);
        TaskDefinition tdTesting = new TaskDefinition(TASKTYPE_SUBWP_TESTING, this);
        tdSpec.getExpectedInput().put("INPUT_ROLE_WPTICKET", new ArtifactType("ARTIFACT_TYPE_JIRA_TICKET"));
        tdDesign.getExpectedOutput().put("OUTPUT_ROLE_SPEC", new ArtifactType("ARTIFACT_TYPE_RESOURCE_LINK"));
        tdDesign.getExpectedOutput().put("OUTPUT_ROLE_SSDDREVIEW", new ArtifactType("ARTIFACT_TYPE_REVIEW"));
//		td.getExpectedOutput().put(OUTPUT_ROLE_FEDDREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
//		td.getExpectedOutput().put(OUTPUT_ROLE_SSS_SRS_MAPPINGREVIEW, new ArtifactType(ArtifactTypes.ARTIFACT_TYPE_RESOURCE_LINK));
        return Arrays.asList(new TaskDefinition[]{tdSpec, tdDesign, tdCoding, tdTesting});
    }

    private DecisionNodeDefinition getWPKickOff(TaskDefinition spec) {
        DecisionNodeDefinition dn = new DecisionNodeDefinition("SubWPKickOff", this, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dn.addOutBranchDefinition(new DefaultBranchDefinition("SubWPin", spec, false, false, dn));
        return dn;
    }

    private DecisionNodeDefinition getSpec2Design(TaskDefinition spec, TaskDefinition design) {
        DecisionNodeDefinition dn = new DecisionNodeDefinition("Spec2Design", this, DecisionNodeDefinition.HAVING_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dn.addInBranchDefinition(new DefaultBranchDefinition("SpecOut", spec, true, false, dn));
        dn.addOutBranchDefinition(new DefaultBranchDefinition("DesignIn", design, false, false, dn));
        return dn;
    }

    private DecisionNodeDefinition getDesign2Coding(TaskDefinition design, TaskDefinition coding) {
        DecisionNodeDefinition dn = new DecisionNodeDefinition("Design2Coding", this, DecisionNodeDefinition.HAVING_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dn.addInBranchDefinition(new DefaultBranchDefinition("DesignOut", design, true, false, dn));
        dn.addOutBranchDefinition(new DefaultBranchDefinition("CodingIn", coding, false, false, dn));
        return dn;
    }

    private DecisionNodeDefinition getCoding2Testing(TaskDefinition coding, TaskDefinition testing) {
        DecisionNodeDefinition dn = new DecisionNodeDefinition("Coding2Testing", this, DecisionNodeDefinition.HAVING_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE, DecisionNodeDefinition.NO_EXTERNAL_RULE);
        dn.addInBranchDefinition(new DefaultBranchDefinition("CodingOut", coding, true, false, dn));
        dn.addOutBranchDefinition(new DefaultBranchDefinition("TestingIn", testing, false, false, dn));
        return dn;
    }

    @Override
    public WorkflowInstance createInstance(String withOptionalId) {
        String wfid = withOptionalId != null ? withOptionalId : this.id+"#"+ UUID.randomUUID().toString();
        WorkflowInstance wfi = new WorkflowInstance(wfid, this, pub);

        return wfi;
    }



}
