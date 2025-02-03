package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

public class EvalData {

	public static ConstraintGroundTruth a1 = new ConstraintGroundTruth(
			"Ensure that all bugs trace (via 'affects') to at least one requirement that is not in status 'Released'."
			, "ProcessStep_BugReqTrace_Task1a"
			, Set.of("Requirement", "Bug")
			, "self.out_Bugs->forAll(bug | bug.affectsItems\r\n"
					+ "->exists(item | item.externalType = 'Requirement'\r\n"
					+ "              and item.state <> 'Released')\r\n"
					+ ")");
	
	public static ConstraintGroundTruth b1 = new ConstraintGroundTruth(
			"Ensure that each Requirement traces (via 'predecessor') only to other Change Requests which are in state 'Released' or have a priority 1 Issue as a child."
			, "ProcessStep_ReqChangeRequestTrace_Task1b"
			, Set.of("Requirement", "ChangeRequest", "Issue")
			, "self.out_REQs\r\n"
					+ "->forAll(req | req.predecessorItems\r\n"
					+ "->select( pre | pre.externalType='Change Request')    \r\n"
					+ "->difference(self.in_CRs)\r\n"
					+ "->forAll(cr |   cr.state = 'Released' \r\n"
					+ "             or cr.successorItems\r\n"
					+ "               ->select(suc | suc.externalType='Issue')\r\n"
					+ "               ->exists(issue : <root/types/azure/Issue> | issue.priority = 1)\r\n"
					+ "   )\r\n"
					+ ")");
	
	public static ConstraintGroundTruth c1 = new ConstraintGroundTruth(
			"Ensure that each Requirement traces (via 'tested by') to at least one Test Case in state 'Released'."
			, "ProcessStep_ReqTestTrace_Task1c"
			, Set.of("Requirement", "TestCase")
			, "self.out_REQs\r\n"
					+ "->forAll(req | \r\n"
					+ "   req.testedByItems->size() > 0 \r\n"
					+ "   and\r\n"
					+ "   req.testedByItems\r\n"
					+ " ->forAll(tc |  tc.externalType = 'Test Case' \r\n"
					+ "               and tc.state = 'Released')\r\n"
					+ ")");
	
	
	public static ConstraintGroundTruth a2 = new ConstraintGroundTruth(
			"Ensure that all Requirements in state 'released' trace (via 'successor') to at least one Review, all which must not have any 'open' Review Findings."
			, "ProcessStep_ReqStateAnalysis_Task2a"
			, Set.of("Requirement", "Review", "Reviewfinding")
			, "self.out_REQs\r\n"
					+ "->select (req2 | req2.state = 'Released')\r\n"
					+ "->forAll(req  | req.successorItems\r\n"
					+ "->exists(item3 | item3.externalType = 'Review')\r\n"
					+ "and \r\n"
					+ "req.successorItems\r\n"
					+ "->select(item | item.externalType = 'Review')\r\n"
					+ "->forAll(rev  | \r\n"
					+ "     rev.successorItems\r\n"
					+ "       ->select(item2 | item2.externalType = \r\n"
					+ "                                 'ReviewFinding')\r\n"
					+ "       ->forAll(finding  \r\n"
					+ "          | finding.state <> 'Open')\r\n"
					+ "  )\r\n"
					+ ")");
	
	
	public static ConstraintGroundTruth b2 = new ConstraintGroundTruth(
			"Ensure that each Requirement with priority '1' traces (via 'tested by') to at least one Test Case that in turn has all Reviews (via 'successor')) in state 'closed'."
			, "ProcessStep_PriorityReqToReviewTrace_Task2b"
			, Set.of("Requirement", "TestCase", "Review")
			, "self.out_REQs\r\n"
					+ "->select(req : <root/types/azure/Requirement>| req.priority = 1)\r\n"
					+ "->forAll(vipreq   | \r\n"
					+ "           vipreq.testedByItems\r\n"
					+ " ->select(item | item.externalType = 'Test Case')\r\n"
					+ " ->exists(tc  | \r\n"
					+ "         tc.successorItems\r\n"
					+ "    ->exists(item2 | item2.externalType='Review')\r\n"
					+ "and\r\n"
					+ "tc.successorItems\r\n"
					+ "    ->select(item3 | item3.externalType='Review')\r\n"
					+ "    ->forAll(rev | \r\n"
					+ "               rev.state='Closed')\r\n"
					+ " ) \r\n"
					+ ")");
	
	public static ConstraintGroundTruth c2 = new ConstraintGroundTruth(
			"Ensure for each Requirement that is 'affected by' a Bug has at least one of the requirement's  'tested by' Test Cases trace (via 'tests') to this Bug."
			, "ProcessStep_ReqBugTestLinking_Task2c"
			, Set.of("Requirement", "Bug", "TestCase")
			, "self.out_REQs\r\n"
					+ "->forAll(req | req.affectedByItems\r\n"
					+ " ->select(item | item.externalType='Bug')\r\n"
					+ " ->forAll(bug  | \r\n"
					+ "    bug.testedByItems\r\n"
					+ "    ->intersection(\r\n"
					+ "        req.testedByItems\r\n"
					+ "         ->select(item3 | \r\n"
					+ "         item3.externalType='Test Case')\r\n"
					+ "    )->size() >0\r\n"
					+ " )\r\n"
					+ ")");
	
	public static ConstraintGroundTruth a3 = new ConstraintGroundTruth(
			"Ensure that either all Bugs for priority '1' 'affects' Requirements are 'Closed' or otherwise that the Requirement is traced (as 'predecessor') from one of the Change Request's 'active' child Issues."
			, "ProcessStep_AssessingBugStates_Task3a"
			, Set.of("Requirement", "Bug", "ChangeRequest", "Issue")
			, "self.out_REQs\r\n"
					+ "->select(req  : <root/types/azure/Requirement> | req.priority=1)\r\n"
					+ "->forAll(prioreq : <root/types/azure/azure_workitem>| \r\n"
					+ "    ( prioreq.affectedByItems\r\n"
					+ "        ->select( item | \r\n"
					+ "                  item.externalType = 'Bug'  )\r\n"
					+ "        ->forAll( bug | bug.state = 'Closed')\r\n"
					+ "    )\r\n"
					+ "   or\r\n"
					+ "    (  prioreq.successorItems\r\n"
					+ "        ->exists(item3 |\r\n"
					+ "              item3.externalType = 'Issue' \r\n"
					+ "              and \r\n"
					+ "              item3.state='Active' \r\n"
					+ "              and \r\n"
					+ "              item3.parentItems            \r\n"
					+ "              ->intersection(self.in_CRs)\r\n"
					+ "                    .size() > 0\r\n"
					+ "            )\r\n"
					+ "    )\r\n"
					+ ")");
	
	public static ConstraintGroundTruth b3 = new ConstraintGroundTruth(
			"Ensure all Requirements with a 'successor' trace to an 'active' Issue (if any), need to have that Issue traced as a 'child' from the Requirement's current (non-released) Change Request."
			, "ProcessStep_AssessingReqIssueTraceability_Task3b"
			, Set.of("Requirement", "ChangeRequest", "Issue")
			, "self.out_REQs\r\n"
					+ "->select(req | req.successorItems\r\n"
					+ "  ->exists(item | item.externalType='Issue'\r\n"
					+ "        and item.state='Active')\r\n"
					+ ")\r\n"
					+ "->forAll(req2  | \r\n"
					+ "     req2.successorItems\r\n"
					+ "        ->select(item2 | \r\n"
					+ "               item2.externalType='Issue'\r\n"
					+ "               and \r\n"
					+ "               item2.state='Active' )\r\n"
					+ "        ->collect(issue : \r\n"
					+ "           <root/types/azure/azure_workitem>  | \r\n"
					+ "            issue.parentItems->asList()->first()\r\n"
					+ "           ->asType( \r\n"
					+ "           <root/types/azure/azure_workitem> )\r\n"
					+ "           )     \r\n"
					+ "       ->intersection(\r\n"
					+ "              req2.predecessorItems\r\n"
					+ "                ->select(item3 | \r\n"
					+ "                  item3.externalType\r\n"
					+ "                             ='Change Request' \r\n"
					+ "                  and \r\n"
					+ "                  item3.state<>'Released')\r\n"
					+ "        ) ->size() > 0\r\n"
					+ ")");
	
	public static ConstraintGroundTruth c3 = new ConstraintGroundTruth(
			"Ensure that every Bug's Testcase directly 'tests' at least one of the Requirements 'affected by' the Bug."
			, "ProcessStep_AssessingTraceability_Task3c"
			, Set.of("Requirement", "Bug", "TestCase")
			, "self.out_Bugs\r\n"
					+ "->forAll( bug | \r\n"
					+ "   bug.testedByItems\r\n"
					+ "   ->forAll( tc | \r\n"
					+ "      tc.testsItems\r\n"
					+ "      ->select( item | \r\n"
					+ "            item.externalType = 'Requirement') \r\n"
					+ "      ->intersection(\r\n"
					+ "        bug.affectsItems\r\n"
					+ "        ->select( item2 |\r\n"
					+ "           item2.externalType = 'Requirement')\r\n"
					+ "    )\r\n"
					+ "     ->size() > 0\r\n"
					+ "  )\r\n"
					+ ")");
	
	public static List<ConstraintGroundTruth> abc123 = List.of(a1, b1, c1, a2, b2, c2, a3, b3, c3);
	
	public static Map<String, ConstraintGroundTruth> data = Map.of("1a", a1, "1b", b1, "1c", c1, "2a", a2, "2b", b2, "2c", c2, "3a", a3, "3b", b3, "3c", c3);
	
	@Data
	public static class ConstraintGroundTruth{
		final String humanReadable;
		final String context;
		final Set<String> relevantSchema;
		final String manualConstraint;
	}
	
}
