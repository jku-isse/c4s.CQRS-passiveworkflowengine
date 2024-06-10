package at.jku.isse.passiveprocessengine.rest;

import java.util.Collections;
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.rest.ProcessCreationEndpoint.CreateRequest;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class CreationTests {

	@Autowired ProcessCreationEndpoint creationEndpoint;
	
	@Autowired SchemaRegistry schemaReg;
	
	@Autowired InstanceRepository instanceRepository;
	
	@Autowired ProcessRegistry processRegistry;
	
	@Autowired ArtifactResolver resolver;
	
	TestDTOProcesses procFactory;	
	TestArtifacts artifactFactory;
		
	PPEInstanceType typeJira;
	
	/*
	 * This tests only the creation, not whether the process engine works correctly 
	 * as this should be tested with the PPE core not here.
	 * 
	 * */
	
	@BeforeAll
	public void setup() {
		artifactFactory = new TestArtifacts(instanceRepository, schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
		typeJira = artifactFactory.getJiraInstanceType();
		processRegistry.createProcessDefinitionIfNotExisting(procFactory.getSimple2StepProcessDefinition());
		PPEInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		PPEInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		PPEInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		DemoArtifactProvider demoissueProvider = new DemoArtifactProvider(schemaReg, instanceRepository, artifactFactory);
		resolver.register(demoissueProvider);
	}
	
	
	@Test
	void testCreateWithNonExistentDefinition() {
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", Collections.emptyMap(), "NonexistingDef"));
		System.out.println(response.getBody());
		assert(response.getStatusCode().is4xxClientError());		
	}

	@Test
	void testCreateWithNonExistentInput() {
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", Collections.emptyMap(), "SimpleProc"));
		System.out.println(response.getBody());
		assert(response.getStatusCode().is4xxClientError());		
	}

	@Test
	void testCreateWithNonFetchableInput() {		
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(TestDTOProcesses.JIRA_IN, new ArtifactIdentifier("jiraA", "NoSuchType"));
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", inputs, "SimpleProc"));
		System.out.println(response.getBody());
		assert(response.getStatusCode().is4xxClientError());		
	}
	
	@Test
	void testCreateWithNonAvailableInput() {		
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(TestDTOProcesses.JIRA_IN, new ArtifactIdentifier("jiraD", typeJira.getName()));
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", inputs, "SimpleProc"));
		System.out.println(response.getBody());
		assert(response.getStatusCode().is4xxClientError());		
	}
	
	@Test
	void testCreateWithAvailableInputAndDelete() {		
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(TestDTOProcesses.JIRA_IN, new ArtifactIdentifier("jiraA", typeJira.getName()));
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", inputs, "SimpleProc"));
		String jsonResp = response.getBody();		
		System.out.println(jsonResp);
		assert(response.getStatusCode().is2xxSuccessful());
		JsonObject jsonProc = JsonParser.parseString(jsonResp).getAsJsonObject();		
		
		ResponseEntity<String> delResponse = creationEndpoint.deleteProcess(jsonProc.getAsJsonPrimitive("name").getAsString());
		System.out.println(delResponse.getBody());
		assert(delResponse.getStatusCode().is2xxSuccessful());
	}
	
	@Test
	void testCreateFetchAndDelete() {
		Map<String, ArtifactIdentifier> inputs = new HashMap<>();
		inputs.put(TestDTOProcesses.JIRA_IN, new ArtifactIdentifier("jiraA", typeJira.getName()));
		ResponseEntity<String> response = creationEndpoint.createProcess(new CreateRequest("SomeName", inputs, "SimpleProc"));
		String jsonResp = response.getBody();		
		System.out.println(jsonResp);
		assert(response.getStatusCode().is2xxSuccessful());
		JsonObject jsonProc = JsonParser.parseString(jsonResp).getAsJsonObject();		
		String name = jsonProc.getAsJsonPrimitive("name").getAsString();
		String id = jsonProc.getAsJsonPrimitive("internalId").getAsString();
		
		ResponseEntity<String> delResponse = creationEndpoint.deleteProcess(name);
		System.out.println(delResponse.getBody());
		assert(delResponse.getStatusCode().is2xxSuccessful());
		
		ResponseEntity<String> fetchResponse = creationEndpoint.getProcess(name);
		System.out.println(fetchResponse.getBody());
		assert(fetchResponse.getStatusCode().equals(HttpStatus.NOT_FOUND));
		// recreate
		response = creationEndpoint.createProcess(new CreateRequest("SomeName", inputs, "SimpleProc"));
		jsonResp = response.getBody();		
		System.out.println(jsonResp);
		assert(response.getStatusCode().is2xxSuccessful());
		jsonProc = JsonParser.parseString(jsonResp).getAsJsonObject();		
		String name2 = jsonProc.getAsJsonPrimitive("name").getAsString();
		String id2 = jsonProc.getAsJsonPrimitive("internalId").getAsString();
		assert(name.equals(name2));				
		assert(!(id.equals(id2)));
		
		fetchResponse = creationEndpoint.getProcess(name2);
		System.out.println(fetchResponse.getBody());
		assert(fetchResponse.getStatusCode().equals(HttpStatus.OK));
		
		delResponse = creationEndpoint.deleteProcess(name2);
		System.out.println(delResponse.getBody());
		assert(delResponse.getStatusCode().is2xxSuccessful());
	}
}
