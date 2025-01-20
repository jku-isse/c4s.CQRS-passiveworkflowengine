package at.jku.isse.passiveprocessengine.tests.oclbot;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.oclx.OclxPackage;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.validation.OCLXValidator;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
class TestLoadAsOCLX {

	@Autowired ParseHelper parser;
		
	@Autowired ValidationTestHelper validator;
	
	@Test
	void testLoadAsOCLX() throws Exception {
		var result = parser.parse("rule TestRule {\r\n"
				+ "					description: \"ignored\"\r\n"
				+ "					context: STRING\r\n"
				+ "					expression: self.isDefined() \r\n"
				+ "				}"				
			);
			Assertions.assertNotNull(result);
			validator.assertError(result, 
				OclxPackage.Literals.CONSTRAINT, 
				OCLXValidator.UNKNOWN_TYPE
			);
			var errors = result.eResource().getErrors();
			Assertions.assertTrue(errors.isEmpty(), "Unexpected errors: "+errors.stream()
			.map(error -> error.toString())
			.collect(Collectors.joining(", \r\n"))
					);
	}
	

}
