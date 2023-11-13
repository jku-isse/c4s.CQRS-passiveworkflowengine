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

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestSchemaExtractor {

	@Autowired
	ArtifactResolver artRes;
	
	
	@BeforeEach
	void setUp() throws Exception {
	}
	
	@Test
	void testSchemaExtraction() throws Exception {
		
		artRes.getAvailableInstanceTypes().stream()
			.filter(type -> type.name().equalsIgnoreCase("azure_workitem"))
			.map(type -> { 
				Map<InstanceType, Set<Instance>> samples = new HashMap<>();
				try {
					Instance a868 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/868", "azure_workitem"));
					Instance a872 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/872", "azure_workitem"));
					Instance a870 = artRes.get(new ArtifactIdentifier("UserStudy1Prep/870", "azure_workitem"));
					samples.put(type, Set.of(a868,a872,a870));
				} catch (ProcessException e) {
					e.printStackTrace();
				}
				return new HumanReadableSchemaExtractor(samples).getSchemaForInstanceType(type); 
				})
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}

	@Test
	void testAllRegisteredArtifactTypesSchemaExtraction() throws ProcessException {
		artRes.getAvailableInstanceTypes().stream()
		.map(type -> new HumanReadableSchemaExtractor().getSchemaForInstanceTypeAndOneHop(type, false))
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}
	
	@Test
	void testSchemaInclSubtypesExtraction() throws ProcessException {
		artRes.getAvailableInstanceTypes().stream()
		.map(type -> new HumanReadableSchemaExtractor().getSchemaForInstanceTypeAndOneHop(type, true))
		.forEach(schema -> System.out.println("\r\n"+schema));
		
	}
}
