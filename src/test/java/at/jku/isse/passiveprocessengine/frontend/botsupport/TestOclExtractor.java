package at.jku.isse.passiveprocessengine.frontend.botsupport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TestOclExtractor {

		
	public final static String raw1 = "```ocl\r\n"
			+ "context Issue\r\n"
			+ "inv: self.successorItems->select(w | w.workItemType = 'Requirement')->forAll(r | r.state = 'Released')\r\n"
			+ "```";   
			
	public final static String raw2 = "```ocl\r\n"
			+ "context Issue\r\n"
			+ "inv AllSucceedingRequirementsReleased:\r\n"
			+ "  self.successorItems->select(r | r.oclIsTypeOf(Requirement))->forAll(r | r.state = 'released')\r\n"
			+ "```";
	
	//TODO: auto replace typename with FQN
	
	public final static String raw3 = "```ocl\r\n"
			+ "context Issue\r\n"
			+ "    inv AllSucceedingRequirementsReleased:\r\n"
			+ "        self.successorItems->select(w | w.workItemType = 'Requirement')->forAll(r | r.state = 'released')\r\n"
			+ "```";

	
	public final static String raw4 = "```ocl\r\n"
			+ "context ProcessStep_CheckingRequirements_RequirementsManagementProcessV2\r\n"
			+ "inv: in_SWRequirementsColl->forAll(r |\r\n"
			+ "  r.out_ReviewFinding->exists(f | f.findingcategory = 'Open') implies r.state <> 'Verified'\r\n"
			+ ")\r\n"
			+ "```"; 
	//TODO 'out_ReviewFinding' is not a known property for InstanceType 'azure_workitem', hence also exists wont work
			
	public final static String raw5 = "```ocl\r\n"
			+ "context ProcessStep_CheckingRequirements_RequirementsManagementProcessV2\r\n"
			+ "inv: self.in_SWRequirementsColl->forAll(req |\r\n"
			+ "    req.out_ReviewFinding->exists(f | f.closedDate = null) implies req.state <> 'Verified'\r\n"
			+ ")\r\n"
			+ "```";
	// TODO: Problem: 'out_ReviewFinding' is not a known property for InstanceType 'azure_workitem'
	
	public final static String raw6 = "```ocl\r\n"
			+ "context ProcessStep_BugReqTrace_Task1a\r\n"
			+ "inv EnsureBugsAffectNonReleasedRequirements:\r\n"
			+ "    out_Bugs->forAll(bug |\r\n"
			+ "        bug.affectsItems->exists(req |\r\n"
			+ "            req.workItemType = 'Requirement' and\r\n"
			+ "            req.state <> 'Released'\r\n"
			+ "        )\r\n"
			+ "    )\r\n"
			+ "```";
	
	static Stream<String> generateTestData() {
		return Stream.of(raw1, raw2, raw3, raw4, raw5, raw6);
	}
	
	@ParameterizedTest
	@MethodSource("generateTestData")
	void testExtract(String input) {
		var ocl = new OCLExtractor(input).extractOCLorNull();
		assertNotNull(ocl);
		System.out.println(ocl);
		
	}
	
	@Test
	void testPostProcessingTypeBrackets() {
		var ocl = new OCLExtractor(raw2).extractOCLorNull();
		assertNotNull(ocl);
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		assertNotNull(processedOCL);
		System.out.println(processedOCL);
		assertTrue(processedOCL.contains("<"));
		assertTrue(processedOCL.contains(">"));
	}
}
