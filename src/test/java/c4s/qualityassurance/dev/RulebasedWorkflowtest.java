package c4s.qualityassurance.dev;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import com.google.inject.Injector;
import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.EventList2Forwarder;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;

import java.util.Map;

class RulebasedWorkflowtest {

	

	public static void main(String[] args) {
		Injector injector = DevelopmentConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		ProjectionModel pModel = new ProjectionModel(aRegistry);
		IJiraService jiraS = DevelopmentConfig.getJiraService(false);
		JamaService jamaS = injector.getInstance(JamaService.class);
		aRegistry.register(jiraS);
		aRegistry.register(jamaS);
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		WorkflowProjection wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry, new EventList2Forwarder());
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
		ReplayStatus status = ReplayStatus.REGULAR;
		String workflowId = "TestId1";
		//new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );
		
//		IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("PVCSG-9", "IJiraArtifact"), workflowId).get();
//		IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("7230585", "IJamaArtifact"), workflowId).get();
		//jamaArt.getDownstreamItems("").stream().anyMatch(dsi -> dsi.getItemType().equals(""));
		IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("18001185", "IJamaArtifact"), workflowId).get();
		jamaArt.getDownstreamItems().stream().forEach(dsi ->
				System.out.println(dsi.getItemType().toString())
				);
		
		
//		
		wfp.on(new CreatedWorkflowEvt(workflowId, Map.of(new ArtifactIdentifier("18001185", "IJamaArtifact"), "jama"), "DemoProcess3", registry.get("DemoProcess3").getWfd()), status);
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
//		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//		.map(wft -> wft.getAnyOneOutputByRole("ARTIFACT_TYPE_QA_CHECK_DOCUMENT"))
//			.filter(Objects::nonNull)
//			.forEach(qa -> System.out.println(qa));
	}

}
