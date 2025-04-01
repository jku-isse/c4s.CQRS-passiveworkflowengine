package at.jku.isse.passiveprocessengine.frontend.botsupport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import at.jku.isse.passiveprocessengine.frontend.oclx.IterativeRepairer;

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
			
	public final static String raw5 = "```ocl\r\n"
			+ "context ProcessStep_CheckingRequirements_RequirementsManagementProcessV2\r\n"
			+ "inv: self.in_SWRequirementsColl->forAll(req |\r\n"
			+ "    req.out_ReviewFinding->exists(f | f.closedDate = null) implies req.state <> 'Verified'\r\n"
			+ ")\r\n"
			+ "```";
	
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
	
	public static final String raw1a = "```ocl context ProcessStep_BugReqTrace_Task1a inv: out_Bugs->forAll(bug | bug.affectsItems->exists(a | a.oclIsTypeOf(Requirement) and a.state <> 'Released')) \n```";
	
	
	
	
	public static final String raw2b = """
			```ocl
context ProcessStep_PriorityReqToReviewTrace_Task2b
inv: self.out_REQs->select(req | req.priority = 1)->forAll(req |
  req.testedByItems->exists(tc |
    tc.successorItems->forAll(s | s.state = 'closed')
  )
)
```		
			""";
	// upon request: "adapt this constraint to check if at least one Test Case exists"
	public static final String raw2b_refinement1 = """
			```ocl
context ProcessStep_PriorityReqToReviewTrace_Task2b
inv:
  self.out_REQs->select(req | req.oclIsKindOf(Requirement) and req.oclAsType(Requirement).priority = 1)->forAll(req |
    req.testedByItems->exists(tc | tc.oclIsKindOf(TestCase) and
      tc.oclAsType(TestCase).successorItems->forAll(s | s.state = 'closed')
    )
  )
```
			""";
	// repair produces:
	public static final String raw2b_partialRepair1 = """
			```ocl
context ProcessStep_PriorityReqToReviewTrace_Task2b
inv:			
	self.out_REQs->select(req | req.isKindOf(<Requirement>) and req.asType(<Requirement>).priority = 1)
	->forAll(req05 | req05.testedByItems
			->exists(tc | req05.isKindOf(TestCase) 
				and 
					req05.asType(TestCase).successorItems
						->forAll(s | req05.state = 'closed') ) ) 
```			
			""";
	
	public static final String raw2a = """
			```ocl\ncontext ProcessStep_ReqStateAnalysis_Task2a\ninv: out_REQs->select(req | req.state = 'released')->forAll(req |\n    req.successorItems->exists(succ |\n        succ.workItemType = 'Review' and\n        succ.relatedItems->select(r | r.oclIsTypeOf(Reviewfinding))->forAll(rf |\n            rf.findingcategory <> 'open'\n        )\n    )\n)\n```
			""";
	
	
	static Stream<String> generateTestData() {
		return Stream.of(raw1, raw2, raw3, raw4, raw5, raw6);
		
	}
	
	@ParameterizedTest
	@MethodSource("generateTestData")
	void testExtract(String input) {
		var ocl = new OCLExtractor(input).extractOCLorEmpty();
		assertNotEquals("", ocl);
		System.out.println(ocl);
		
	}
	
	@Test
	void testPostProcessingTypeBrackets() {
		var ocl = new OCLExtractor(raw2).extractOCLorEmpty(); //raw2 , raw1a, raw2b, 
		assertNotEquals("", ocl);
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		assertNotNull(processedOCL);
		System.out.println(processedOCL);
		assertTrue(processedOCL.contains("<Requirement>"));
	}
	
	@Test
	void testPostProcessingTypeBracketsAroundReviewfinding() {
		var ocl = new OCLExtractor(raw2a).extractOCLorEmpty(); 
		assertNotEquals("", ocl);
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		assertNotNull(processedOCL);
		System.out.println(processedOCL);
		assertTrue(processedOCL.contains("<Reviewfinding>"));
		
		var oclx = IterativeRepairer.wrapInOCLX(processedOCL, "ignored");
	}
		
	@Test
	void testPostProcessingMultipleTypeBrackets() {
		var ocl = new OCLExtractor(raw2b_refinement1).extractOCLorEmpty(); 
		assertNotEquals("", ocl);
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		assertNotNull(processedOCL);
		System.out.println(processedOCL);
		assertTrue(processedOCL.contains("<TestCase>"));
	}
	
	 	
	@ParameterizedTest
	@ValueSource(strings = {
			"```self```"
			,"```selfNot``` some ```self```"
			, "```selfNot``` some ```self"
			,"```self"
			,"```selfNot``````self```"
			,"```self```some```"
			,"```selfNot``` some ```self``` some more"})
	void testSuccessMultiBackticks(String rawText) {
		var ocl = new OCLExtractor(rawText).extractOCLorEmpty();
		assertNotEquals("", ocl);
		assertEquals("self", ocl);
	}
	
	
	@ParameterizedTest
	@ValueSource(strings = {
			"```"
			,"``` ```"
	})	
	void testFailMultiBackticks(String rawText) {
		var ocl = new OCLExtractor(rawText).extractOCLorEmpty();
		assertEquals("", ocl);		
	}
}
