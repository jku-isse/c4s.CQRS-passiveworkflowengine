package c4s.qualityassurance.dev;

import java.util.AbstractMap;
import java.util.List;
import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifactRegistry;
import artifactapi.jama.IJamaArtifact;
import impactassessment.artifactconnector.ArtifactRegistry;
import impactassessment.artifactconnector.jama.JamaService;
import impactassessment.artifactconnector.jira.JiraService;
import impactassessment.command.MockCommandGateway;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import com.google.inject.Injector;

import impactassessment.DevelopmentConfig;
import impactassessment.api.Events.CreatedWorkflowEvt;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

class RulebasedWorkflowtest {

	

	public static void main(String[] args) {
		Injector injector = DevelopmentConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		ProjectionModel pModel = new ProjectionModel();
		IArtifactRegistry aRegistry = new ArtifactRegistry();
		JiraService jiraS = DevelopmentConfig.getJiraService();
		JamaService jamaS = injector.getInstance(JamaService.class);
		aRegistry.register(jiraS);
		aRegistry.register(jamaS);
		IFrontendPusher fp = new SimpleFrontendPusher();
		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
		WorkflowDefinitionRegistry registry = new WorkflowDefinitionRegistry();
		LocalRegisterService lrs = new LocalRegisterService(registry);
		lrs.registerAll();
		WorkflowProjection wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
		ReplayStatus status = ReplayStatus.REGULAR;
		String workflowId = "TestId1";
		//new AddOutputCmd(id, id, null, "outDoc", new ArtifactType("IJiraArtifact") );
		
//		IJiraArtifact jiraArt = (IJiraArtifact) jiraS.get(new ArtifactIdentifier("PVCSG-9", "IJiraArtifact"), workflowId).get();
//		IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("7230585", "IJamaArtifact"), workflowId).get();
		//jamaArt.getDownstreamItems("").stream().anyMatch(dsi -> dsi.getItemType().equals(""));
		IJamaArtifact jamaArt = (IJamaArtifact) jamaS.get(new ArtifactIdentifier("7801212", "IJamaArtifact"), workflowId).get();
//		
		wfp.on(new CreatedWorkflowEvt(workflowId, List.of(new AbstractMap.SimpleEntry<>("jama",jamaArt)), "DemoProcess2", registry.get("DemoProcess2").getWfd()), status);
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
//		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//		.map(wft -> wft.getAnyOneOutputByRole("ARTIFACT_TYPE_QA_CHECK_DOCUMENT"))
//			.filter(Objects::nonNull)
//			.forEach(qa -> System.out.println(qa));
	}

}
