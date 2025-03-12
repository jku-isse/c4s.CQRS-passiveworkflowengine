package at.jku.isse.passiveprocessengine.tests.oclbot;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.passiveprocessengine.frontend.oclx.IterativeRepairer;
import at.jku.isse.ide.assistance.CodeActionExecuter;
import at.jku.isse.passiveprocessengine.frontend.botsupport.GeneratedRulePostProcessor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.TestOclExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.experiment.EvalData;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
public class TestLoadAsOCLX {


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
		 
		return Stream.of(
				
				//Arguments.of(TestOclExtractor.raw1a, "ProcessStep_BugReqTrace_Task1a")
				Arguments.of(TestOclExtractor.raw2b_refinement1, "ProcessStep_PriorityReqToReviewTrace_Task2b")
			//	, Arguments.of(TestOclExtractor.raw1, "Issue") 
			//	, Arguments.of(TestOclExtractor.raw2, "Issue") 
			//	, Arguments.of(TestOclExtractor.raw3, "Issue") 
//				,Arguments.of(TestOclExtractor.raw4, "ProcessStep_CheckingRequirements_RequirementsManagementProcessV2"),
//				,Arguments.of(TestOclExtractor.raw5, "ProcessStep_CheckingRequirements_RequirementsManagementProcessV2")
				)
					;
	}
	
	
	@ParameterizedTest
	@MethodSource("generateTestData")
	void testLoadRawAsOCLX(String input, String context) {
		var ocl = new OCLExtractor(input).extractOCLorEmpty();
		assertNotNull(ocl);				
		var processedOCL = GeneratedRulePostProcessor.init(ocl).getProcessedRule();
		processedOCL = IterativeRepairer.wrapInOCLX(processedOCL, context);
		System.out.println("\r\nProcessing: "+processedOCL);		
		CodeActionExecuter executer = provider.buildExecuter(processedOCL);
		executer.checkForIssues();
		var issues = executer.getProblems();
		issues.forEach(issue -> System.out.println("Problem: "+issue.getMessage()));		
		executer.executeFirstExecutableRepair();		
		var repair = executer.getExecutedCodeAction();
		if (repair != null) {
			processedOCL = executer.getRepairedOclxConstraint();
			repair.getEdit().getChanges().values().iterator().next().stream().forEach(edit -> System.out.println("Repair: "+edit.getNewText()));
			System.out.println("Repaired Constraint: "+processedOCL);
			executer = provider.buildExecuter(processedOCL); // we need a new executer (to parse the new text)
			executer.checkForIssues(); // any remaining issues?
			if (executer.getProblems().isEmpty()) {
				System.out.println("Constraint is correct now");
			} else {
				executer.getProblems().forEach(issue -> System.out.println("Remaining Problem: "+issue.getMessage()));	
			}
		}	
		
	}
	
	@Test
	void testRepairRaw2BRefinement1Success() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_PriorityReqToReviewTrace_Task2b", "Irrelevant", TestOclExtractor.raw2b_refinement1, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());
	}
	
	@Test
	void testRepairRaw2ASuccess() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_ReqStateAnalysis_Task2a", "Irrelevant", TestOclExtractor.raw2a, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());
	}
	
	@Test
	void testReplaceWithMostFittingSubclass() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_PriorityReqToReviewTrace_Task2b", "Irrelevant",
		"```ocl context ProcessStep_PriorityReqToReviewTrace_Task2b inv:  self.out_REQs->select(req | req.priority = 1)->forAll(req |    req.testedByItems->exists(tc |      tc.workItemType = 'TestCase' and      tc.successorItems->forAll(r | r.state = 'closed')    )  )```"
				, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());
	}
	
	@Test
	void testUseImplied() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_ReqBugTestLinking_Task2c", "Irrelevant",
		"```ocl context ProcessStep_ReqBugTestLinking_Task2c inv: out_REQs->forAll(r |    r.affectedByItems->exists(b | b.workItemType = 'Bug') implies        r.testCaseItems->exists(tc | tc.testsItems->includes(b)))```"
				, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());
	
	}		
	
	@Test
	void whyTypeAndCardinalityIsNull() {
		var response = """
 ```ocl
context ProcessStep_PriorityReqToReviewTrace_Task2b
inv EnsureReleasedRequirementsTraceToReviewWithoutOpenFindings:
    self.out_REQs->select(r | r.state = 'released')->
        forAll(r | r.successorItems->exists(s |
            s.oclIsTypeOf(Review) and
            not s.relatedItems->exists(rf |
                rf.oclIsTypeOf(Reviewfinding) and
                rf.findingcategory = 'open')))
```""";
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_PriorityReqToReviewTrace_Task2b", "Irrelevant", response, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());			

	}
	
	public static final String raw1a_codestral = "self.out_Bugs->forAll(bug | bug.affectsItems->exists(req : azure_workitem | req.oclIsTypeOf(Requirement) and req.state <> 'Released'))";	
	@Test
	void testWrapTypesInBrackets() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_BugReqTrace_Task1a", "Irrelevant", raw1a_codestral, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());	
	}
	
	public static final String raw1b_codestral = "predecessorItems->forAll(cr | cr.oclIsKindOf(ChangeRequest) and (cr.state = 'Released' or cr.childItems->exists(issue | issue.oclIsKindOf(Issue) and issue.priority = 1)))";
	@Test
	void testTypeAndCardShouldNotBeNull() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_ReqChangeRequestTrace_Task1b", "Irrelevant", raw1b_codestral, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		//assertNull(repairInfo.getRemainingError());	
	}
	
	public static final String raw3a_codestral = "self.out_Bugs->forAll(b | b.priority = 1 and b.affectsItems->exists(req | req.oclIsKindOf(Requirement)) implies (b.state = 'Closed' or self.in_CRs->exists(cr | cr.oclIsKindOf(ChangeRequest) and cr.childItems->exists(issue | issue.oclIsKindOf(Issue) and issue.state <> 'Closed') and issue.predecessorItems->includes(req))))";
	@Test
	void testCorrectSubtypeWithProperty() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse("ProcessStep_AssessingBugStates_Task3a", "Irrelevant", raw3a_codestral, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
	}
	
	public static final String raw2a_codestral = " I apologize for the confusion, but it seems there was an error in my previous response. The property 'successorItems' is not listed among the properties of ProcessStep in your provided schema.\n\nHowever, based on the context and task description, it appears that the correct relationship should be from Requirements to Reviews or Review Findings. Given this, a possible OCL rule could be:\n\n   self.out_REQs->forAll(r | r.oclIsKindOf(Requirement) and r.state = 'released' implies\n     r.successorItems->exists(s | s.oclIsKindOf(Review) and not s.findingItems->exists(f | f.oclIsKindOf(ReviewFinding) and f.state = 'open'))\n   )";
	@Test
	void testWhatErrorGenerated() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse(EvalData.a2.getContext(), "Irrelevant", raw2a_codestral, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
	}
	
	public static final String raw1b_qwen = "```ocl\nself.out_REQs->forAll(req : azure_workitem |\n    req.predecessorItems->forAll(pred : azure_workitem |\n        pred.workItemType = 'ChangeRequest' implies (\n            pred.state = 'Released' or \n            pred.childItems->exists(child : azure_workitem | child.workItemType = 'Issue' and child.priority = 1)\n        )\n    )\n)\n```";
	@Test
	void testFixAllTypeErrors() {
		var repairer = new IterativeRepairer(provider);
		var repairInfo = repairer.checkResponse(EvalData.b1.getContext(), "Irrelevant", raw1b_qwen, 0);
		System.out.println(repairInfo.toRepairInfoOnlyString());
		assertNull(repairInfo.getRemainingError());
	}
}
