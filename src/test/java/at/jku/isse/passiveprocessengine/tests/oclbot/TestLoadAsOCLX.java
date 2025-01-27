package at.jku.isse.passiveprocessengine.tests.oclbot;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.ide.assistance.CodeActionExecuter;
import at.jku.isse.passiveprocessengine.frontend.botsupport.GeneratedRulePostProcessor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.TestOclExtractor;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
class TestLoadAsOCLX {


	@Autowired
	ArtifactResolver artRes;
	
	@Autowired
	CodeActionExecuterProvider provider;
	
	@Test
	void testLoadAsOCLX() throws Exception {
		var constraint = "rule TestRule {\r\n"
				+ "					description: \"ignored\"\r\n"
				+ "					context: Bug\r\n"
				+ "					expression: self.isDefined() \r\n"
				+ "				}"				
			;		
		CodeActionExecuter executer = provider.buildExecuter(constraint);
		executer.checkForIssues();
		var issues = executer.getProblems();
		assertEquals(0, issues.size());		
	}
	
	static Stream<Arguments> generateTestData() {
		 
		return Stream.of(Arguments.of(TestOclExtractor.raw1, "Issue")); //, 
//				Arguments.of(TestOclExtractor.raw2, "Issue"), 
//				Arguments.of(TestOclExtractor.raw3, "Issue"), 
//				Arguments.of(TestOclExtractor.raw4, "ProcessStep_CheckingRequirements_RequirementsManagementProcessV2"),
//				Arguments.of(TestOclExtractor.raw5, "ProcessStep_CheckingRequirements_RequirementsManagementProcessV2")
//				)
//												;
	}
	
	
	@ParameterizedTest
	@MethodSource("generateTestData")
	void testLoadRawAsOCLX(String input, String context) {
		var ocl = new OCLExtractor(input).extractOCLorNull();
		assertNotNull(ocl);				
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		processedOCL = wrapInOCLX(processedOCL, context);
		System.out.println("Processing: "+processedOCL);		
		CodeActionExecuter executer = provider.buildExecuter(processedOCL);
		executer.checkForIssues();
		var issues = executer.getProblems();
		issues.forEach(issue -> System.out.println("Problem: "+issue.getMessage()));		
		executer.executeRepairs();
		var repair = executer.getExecutedCodeAction();
		if (repair != null) {
			repair.getEdit().getChanges().values().iterator().next().stream().forEach(edit -> System.out.println("Repair: "+edit.getNewText()));
		}					
	}
	
	private String wrapInOCLX(String constraint, String context) {
		return "rule TestRule {\r\n"
				+ "					description: \"ignored\"\r\n"
				+ "					context: "+context+"\r\n"
				+ "					expression: "+constraint+" \r\n"
				+ "				}";	
	}
}
