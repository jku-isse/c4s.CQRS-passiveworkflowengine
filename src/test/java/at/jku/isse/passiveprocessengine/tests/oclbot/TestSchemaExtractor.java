package at.jku.isse.passiveprocessengine.tests.oclbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestSchemaExtractor {

	@Autowired
	ArtifactResolver artRes;
	
	@Autowired SchemaRegistry schemaReg;
	
	@BeforeEach
	void setUp() throws Exception {
	}
	
	@Test
	void testSchemaExtraction() throws Exception {
		
		artRes.getAvailableInstanceTypes().stream()
			.filter(type -> type.getName().equalsIgnoreCase("azure_workitem"))
			.map(type -> { 
				Map<PPEInstanceType, Set<PPEInstance>> samples = new HashMap<>();
				try {
					PPEInstance a868 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/868", "azure_workitem"));
					PPEInstance a872 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/872", "azure_workitem"));
					PPEInstance a870 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/870", "azure_workitem"));
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
}
