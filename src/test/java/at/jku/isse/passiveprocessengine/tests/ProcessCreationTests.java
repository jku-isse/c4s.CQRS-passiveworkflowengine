package at.jku.isse.passiveprocessengine.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint.CreateRequest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes=PPE3Webfrontend.class)
@TestInstance(Lifecycle.PER_CLASS)
class ProcessCreationTests {

	@Autowired ProcessCreationEndpoint creationEndpoint;
	
	@Autowired SchemaRegistry schemaReg;
	
	@Autowired InstanceRepository instanceRepository;
			
	@Autowired ProcessRegistry processRegistry;
	
	TestArtifacts artifactFactory;		
	PPEInstanceType typeJira;
	String typeJiraName;

	
	@BeforeAll
	public void setup() {
		//ruleRepo = ((NodeToDomainResolver) instanceRepository).getRuleRepo();
		instanceRepository.startWriteTransaction();
		artifactFactory = new TestArtifacts(instanceRepository, schemaReg);
		
		typeJira = artifactFactory.getJiraInstanceType();
		typeJiraName = typeJira.getName();
		var procFactory = new TestDTOProcesses(artifactFactory);
		processRegistry.createProcessDefinitionIfNotExisting(procFactory.getSimple2StepProcessDefinition());
//		PPEInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
//		PPEInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
//		PPEInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
//		DemoArtifactProvider demoissueProvider = new DemoArtifactProvider(schemaReg, instanceRepository, artifactFactory);
//		resolver.register(demoissueProvider);
		instanceRepository.concludeTransaction();
	}
	
		
	
	@Test
	void testCreateOrFetchPreexisting() {		
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(TestDTOProcesses.JIRA_IN, new ArtifactIdentifier("jiraA", typeJiraName));
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName5", inputs, "SimpleProc"));
		String jsonResp = response.getBody();		
		System.out.println(jsonResp);
		assertTrue(response.getStatusCode().equals(HttpStatus.CONFLICT) ||
				response.getStatusCode().equals(HttpStatus.CREATED));	
	}
}
