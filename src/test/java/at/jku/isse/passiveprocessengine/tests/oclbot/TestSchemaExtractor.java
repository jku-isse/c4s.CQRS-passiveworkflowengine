package at.jku.isse.passiveprocessengine.tests.oclbot;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
class TestSchemaExtractor {

	@Autowired
	ArtifactResolver artRes;
	
	@Autowired NodeToDomainResolver schemaReg;
	
	@BeforeEach
	void setUp() throws Exception {
	}
	
	@Test
	void testSchemaExtraction() throws Exception {
		artRes.getAvailableInstanceTypes().stream().forEach(type -> System.out.println(type.getName()));
		
		
		artRes.getAvailableInstanceTypes().stream()
			.filter(type -> type.getName().equalsIgnoreCase("azure_workitem"))
			.map(type -> { 
				Map<PPEInstanceType, Set<PPEInstance>> samples = new HashMap<>();
				try {
					PPEInstance a868 = artRes.get(new ArtifactIdentifier("868", "azure_workitem"));
					PPEInstance a872 = artRes.get(new ArtifactIdentifier("872", "azure_workitem"));
					PPEInstance a870 = artRes.get(new ArtifactIdentifier("870", "azure_workitem"));
					samples.put(type, Set.of(a868,a872,a870));
				} catch (ProcessException e) {
					e.printStackTrace();
				}
				return new HumanReadableSchemaExtractor(schemaReg).getSchemaForInstanceType(type); 
				})
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}

	@Test
	void testAllRegisteredArtifactTypesSchemaExtraction() throws ProcessException {
		artRes.getAvailableInstanceTypes().stream()
		.map(type -> new HumanReadableSchemaExtractor(schemaReg).getSchemaForInstanceTypeAndOneHop(type, false))
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}
	
	@Test
	void testSchemaInclSubtypesExtraction() throws ProcessException {
		artRes.getAvailableInstanceTypes().stream()
		.map(type -> new HumanReadableSchemaExtractor(schemaReg).getSchemaForInstanceTypeAndOneHop(type, true))
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}
	
	@Test
	void testPrintSchemaSubselection() throws ProcessException {
		var schemaExtractor = new HumanReadableSchemaExtractor(schemaReg);
		//CrIssueFd
		var subsetIds = List.of("CrIssueFd", "Issue", "Requirement", "L3Requirements");
		Map<PPEInstanceType, List<PPEInstanceType>> subsetGroups =  schemaExtractor.clusterTypes(
				artRes.getAvailableInstanceTypes().stream()
						.filter(type -> subsetIds.contains(type.getName()))
						.toList()
						);
		assertEquals(1, subsetGroups.size());
		subsetGroups.entrySet().forEach(entry -> {
			var props = schemaExtractor.processSubgroup(entry.getKey(), entry.getValue());
			var schema = schemaExtractor.compileSchemaList(entry.getKey(),  entry.getValue(), props.getKey(), props.getValue());
			System.out.println(schema);
		});						
	}
	
	@Test
	void testPrintSchemaSubselectionFromStep() throws ProcessException {
		var schemaExtractor = new HumanReadableSchemaExtractor(schemaReg);
		//CrIssueFd
		var steps = schemaReg.findAllInstanceTypesByFQN("ProcessStep_CheckingRequirements_RequirementsManagementProcessV2");
		assertEquals(1, steps.size());
		var types = new ArrayList<PPEInstanceType>();
		types.addAll(steps);
		var subsetIds = List.of("Review", "Reviewfinding", "Requirement", "L3Requirements");
		types.addAll(artRes.getAvailableInstanceTypes().stream()
						.filter(type -> subsetIds.contains(type.getName()))
						.toList());
		
		Map<PPEInstanceType, List<PPEInstanceType>> subsetGroups =  schemaExtractor.clusterTypes(
				types						);
		assertEquals(2, subsetGroups.size());
		subsetGroups.entrySet().forEach(entry -> {
			var props = schemaExtractor.processSubgroup(entry.getKey(), entry.getValue());
			var schema = schemaExtractor.compileSchemaList(entry.getKey(),  entry.getValue(), props.getKey(), props.getValue());
			System.out.println(schema);
		});						
	}
	
	@Test
	void testPrintSchemaSubselectionFromStep2() throws ProcessException {
		var schemaExtractor = new HumanReadableSchemaExtractor(schemaReg);
		//CrIssueFd
		var steps = schemaReg.findAllInstanceTypesByFQN("ProcessStep_CheckingRequirements_RequirementsManagementProcessV2");
		assertEquals(1, steps.size());
		var types = new ArrayList<PPEInstanceType>();
		types.addAll(steps);
		var subsetIds = List.of("Bug", "Requirement", "L3Requirements");
		types.addAll(artRes.getAvailableInstanceTypes().stream()
						.filter(type -> subsetIds.contains(type.getName()))
						.toList());
		
		Map<PPEInstanceType, List<PPEInstanceType>> subsetGroups =  schemaExtractor.clusterTypes(
				types						);
		assertEquals(2, subsetGroups.size());
		subsetGroups.entrySet().forEach(entry -> {
			var props = schemaExtractor.processSubgroup(entry.getKey(), entry.getValue());
			var schema = schemaExtractor.compileSchemaList(entry.getKey(),  entry.getValue(), props.getKey(), props.getValue());
			System.out.println(schema);
		});						
	}
}
