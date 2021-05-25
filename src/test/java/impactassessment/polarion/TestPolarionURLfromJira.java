package impactassessment.polarion;

import java.net.URL;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import artifactapi.jira.IJiraArtifact;
import artifactapi.jira.subtypes.IJiraIssueField;
import c4s.impactassessment.tools.URLExtractor;
import impactassessment.artifactconnector.jira.IJiraService;

public class TestPolarionURLfromJira {

	IJiraService js = DevelopmentConfig.getJiraDemoService();
	
	@Before
	public void setup() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
		//Injector injector = DevelopmentConfig.getInjector();
		//CommandGateway gw = injector.getInstance(CommandGateway.class);
	//	aRegistry = injector.getInstance(IArtifactRegistry.class);
		//ProjectionModel pModel = new ProjectionModel(aRegistry);
		//ps = injector.getInstance(IPolarionService.class);
		//aRegistry.register(ps);
	//	aRegistry.register(DevelopmentConfig.getJiraDemoService());
//		IFrontendPusher fp = new SimpleFrontendPusher();
//		IKieSessionService kieS = new SimpleKieSessionService(gw, aRegistry);
//		registry = new WorkflowDefinitionRegistry();
//		LocalRegisterService lrs = new LocalRegisterService(registry);
//		lrs.registerAll();
//		wfp = new WorkflowProjection(pModel, kieS,  gw, registry, fp, aRegistry);
//		((MockCommandGateway)gw).setWorkflowProjection(wfp);
		
	}
	
	@Test
	public void runExtractTest() {
		//ArtifactIdentifier ai = new ArtifactIdentifier("DEMOISSUE", "IJiraArtifact");
		Optional<IJiraArtifact> optArt = js.getIssue("DEMOISSUE");
		IJiraArtifact jira = optArt.get();
		IJiraIssueField refField = jira.getField("customfield_10042");
		if (refField !=null) {
			String uriStr = refField.getValue().toString();
			try {
				URL uri = new URL(uriStr);
				String ref = uri.getRef();
				String query = ref.substring(ref.lastIndexOf('?')+1);
				String id = URLExtractor.returnFirstValueFromQueryForKey(query, "id");
				System.out.println(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
	}

}
