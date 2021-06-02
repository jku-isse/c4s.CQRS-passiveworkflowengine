package impactassessment.basebehavior;

import java.util.AbstractMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.ReplayStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import artifactapi.jira.IJiraArtifact;
import at.jku.designspace.sdk.polarion.clientservice.interfaces.IPolarionService;
import at.jku.designspace.sdk.polarion.polarionapi.implementations.PolarionArtifact;
import impactassessment.api.Events.*;
import impactassessment.api.Queries.PrintKBQuery;
import impactassessment.command.MockCommandGateway;
import impactassessment.kiesession.IKieSessionService;
import impactassessment.kiesession.SimpleKieSessionService;
import impactassessment.query.ProjectionModel;
import impactassessment.query.WorkflowProjection;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import impactassessment.ui.IFrontendPusher;
import impactassessment.ui.SimpleFrontendPusher;

public class TestMultiArtifacts {

	ReplayStatus status = ReplayStatus.REGULAR;
	String workflowId = "TestId1";
	String wft = "BASETEST1";
	WorkflowDefinitionRegistry registry;
	WorkflowProjection wfp;
	IArtifactRegistry aRegistry;
	ProjectionModel pModel;
	IKieSessionService kieS;
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		Injector injector = BaseBehaviorTestConfig.getInjector();
		CommandGateway gw = injector.getInstance(CommandGateway.class);
		aRegistry = injector.getInstance(IArtifactRegistry.class);
		pModel = new ProjectionModel(aRegistry);
		aRegistry.register(BaseBehaviorTestConfig.getJiraDemoService());
		IFrontendPusher fp = new SimpleFrontendPusher();
		kieS = new SimpleKieSessionService(gw, aRegistry);
		registry = injector.getInstance(WorkflowDefinitionRegistry.class);
		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
		((MockCommandGateway)gw).setWorkflowProjection(wfp);
	
	}
	
	@Test
	public void runMultiArtifactOutputTest() {
		ArtifactIdentifier ai = new ArtifactIdentifier("DEMOISSUE", "IJiraArtifact");
		Optional<IArtifact> optArt = aRegistry.get(ai, workflowId);
		IJiraArtifact jira = (IJiraArtifact) optArt.get();
//		
		wfp.on(new CreatedWorkflowEvt(workflowId, List.of(new AbstractMap.SimpleEntry<>("jira", ai)), wft, registry.get(wft).getWfd()), status);
		//kieS.getKieSession(workflowId).fireAllRules();
		//wfp.on(new AddedInputToWorkflowEvt(id, new ArtifactInput(new ArtifactWrapper(jiraArt.getKey(), "IJiraArtifact", null, jiraArt), "root")), status);
		wfp.handle(new PrintKBQuery(workflowId));
		wfp.on(new SetPostConditionsFulfillmentEvt(workflowId, "Open#"+workflowId, true), status);
		kieS.getKieSession(workflowId).fireAllRules();
//		pModel.getWorkflowModel(workflowId).getWorkflowInstance().getWorkflowTasksReadonly().stream()
//			.forEach(wft -> { //System.out.println(wft); 
//			System.out.println("InputRelItemsSize: "+wft.getAllInputsByRole("relItems").size());
//			});
		
		wfp.handle(new PrintKBQuery(workflowId));
	}

}
