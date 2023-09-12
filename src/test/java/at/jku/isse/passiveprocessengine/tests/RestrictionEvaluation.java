package at.jku.isse.passiveprocessengine.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.azure.api.AzureApi;
import at.jku.isse.designspace.azure.model.AzureBaseElementType;
import at.jku.isse.designspace.azure.service.AzureService;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ListProperty;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RestrictionEvaluation {

	@Autowired
	WorkspaceService workspaceService;

	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ProcessRegistry procReg;

	@Autowired
	ArtifactResolver artRes;

	@Autowired
	AzureService azureService;

	static Workspace ws;

	void loadAll() throws ProcessException
	{
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/907", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/908", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/909", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/910", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/911", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/917", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/918", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/919", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/920", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/921", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/922", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/923", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/924", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/925", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/926", "azure_workitem"));
		
		
	}
	@BeforeEach
	void setup() throws Exception {
		//RuleService.setEvaluator(new ArlRuleEvaluator());
		//	WorkspaceService.\
		System.out.println(azureService.toString());
		//ws=azureService.getWorkspace();
		//ws = WorkspaceService.createWorkspace("Public", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		ws = WorkspaceService.PUBLIC_WORKSPACE;
		//	RuleService.currentWorkspace = ws;
		//	typeJira = TestArtifacts.getJiraInstanceType(ws);
	}
	
	/* Assessing Requirement Approved Conditions
	 * Requirement:  Users, including healthcare providers, administrators, and patients, should authenticate 
	 * themselves before accessing the system or patient data.
	 * Child Requirement: healthcare Provider authentication, administrator authentication, patient authentication
	 * Priority: 4
	 * Constraint: A requirement can only be approved if it's associated requirements are approved, test cases > 0 & bugs are closed, 
	 * and change requests are released. 
	 * Start: user loggs into azure devops services. The process is instantialized on REQ workitem req, which have 3 child requirements 
	 * (2 in state approved & 1 in In progress), 0 test cases, 2 bugs (both in state active), 1 change request in state reviewed.
	 * End: When participant declares to be done.
	 * Success Criteria: After participant declares to be done the requirement 
	*/ 
	
	@Test 
	void testGroup1114() throws ProcessException
	{
		loadAll();
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/920", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.affectedbyItems->select(item: <"+azureType.getQualifiedName()+"> | item.workItemType='Bug')"
						+ "->forAll(bug: <"+azureType.getQualifiedName()+"> | bug.testedbyItems->intersection("
								+ "self.testedbyItems-> select(item3: <"+azureType.getQualifiedName()+"> | item3.workItemType='Test Case')"
										+ "-> collect(tc: <"+azureType.getQualifiedName()+"> | tc.testsItems) ->"
												+ "select(item2: <"+azureType.getQualifiedName()+"> | item2.workItemType='Bug') ) -> size()>0 )");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		System.out.println(crt.toString().substring(69,150));
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	@Test 
	void testGroup1_3() throws ProcessException
	{
		loadAll();
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		Instance azureIssue1 = artRes.get(new ArtifactIdentifier("UserStudy2Prep/911", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.workItemType='Requirement' then "
				+ "self.childItems->select(c1: <"+azureType.getQualifiedName()+"> | c1.workItemType='Change Request')"
						+ "->forAll(c101:<"+azureType.getQualifiedName()+"> | c101.project.name<>self.project.name)"
								+ "else true endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	@Test 
	void testGroup1_2() throws ProcessException
	{
		loadAll();
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.workItemType='Requirement' then "
						+"if self.state='Released' then "
						+ "self.childItems->select(c1: <"+azureType.getQualifiedName()+"> | c1.workItemType='Change Request') "
								+ "->forAll(c101:<"+azureType.getQualifiedName()+"> | c101.state='Released') and"
										+ " self.testedbyItems->select(t1:<"+azureType.getQualifiedName()+"> | t1.workItemType='Test Case')"
										+ "->forAll(t101: <"+azureType.getQualifiedName()+"> | t101.state='Closed') and"
												+ " self.relatedItems->select(r1:<"+azureType.getQualifiedName()+"> |r1.workItemType='Risk')"
														+ "->forAll(r101: <"+azureType.getQualifiedName()+">| r101.state='Closed')"
												+ " else self.state<>'Released' endif else true endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	@Test 
	void testGroup1_1() throws ProcessException
	{
		loadAll();
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.name.contains(self.name))");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	@Test 
	void testGroup1() throws ProcessException
	{
		loadAll();
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		/*ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				" if self.priority=4 "
							+ "then self.testedbyItems.size()>3 and "
							+ "self.testedbyItems->forAll(t1:<"+azureType.getQualifiedName()+"> | t1.priority<=self.priority) "
							+ "else true endif");*/
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.workItemType='Requirement' then"
						+ " if self.priority=4 "
							+ "then self.testedbyItems.size()>3 and "
							+ "self.testedbyItems->forAll(t1:<"+azureType.getQualifiedName()+"> | t1.priority<=self.priority) "
							+ "else if self.priority=3"
							+ " then self.testedbyItems.size()>2 and "
							+ "self.testedbyItems->forAll(t2:<"+azureType.getQualifiedName()+"> | t2.priority<=self.priority) "
							+ " else if self.priority<=2 then"
							+ " self.testedbyItems.size()>=1 and"
							+ " self.testedbyItems->forAll(t3:<"+azureType.getQualifiedName()+"> | t3.priority<=self.priority) "
							+ " else true endif endif endif else true endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	
	
	// Further List of Possible constraints
	//TODO: Constraint: Test cases should be associated with the same requirement as their parent work items. X
	//TODO: Constraint: Bugs should only be assigned to team members who are responsible for the related requirement.
	//TODO: Change requests should only be linked to requirements that belong to the same project and have a higher priority.
	//Description, Start Condition, Success Condition
	@Test
	void testExperimentTask503() throws ProcessException
	{
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/902", "azure_workitem"));
		artRes.get(new ArtifactIdentifier("UserStudy2Prep/903", "azure_workitem"));
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.priority<=2 then self.affectedbyItems-> select(a: <"+azureType.getQualifiedName()+">| "
						+ "a.workItemType='Risk' or a.workItemType='ModelDocumentation' or a.workItemType='Task').size()>0 "
				+ "else true endif ");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	@Test
	void testExperimentTask501() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/903", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/903"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.priority=1 then self.testedbyItems.size()>4 "
				+ "and self.testedbyItems -> forAll(t: <"+azureType.getQualifiedName()+"> | t.referencesItems.size()>2)"
				+ " else if self.priority=2 then self.testedbyItems.size()>2 "
				+ "and self.testedbyItems -> forAll(t1: <"+azureType.getQualifiedName()+"> | t1.referencesItems.size()>1)"
				+ " else if self.priority=3 then self.testedbyItems.size()>1"
				+ " else self.testedbyItems.size()>=1"
				+ " endif endif endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	@Test
	void testExperimentTask101() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/905", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/905"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.testedbyItems->exists(t: <"+azureType.getQualifiedName()+"> | t.state<>'Closed' ) then self.state='Active'"
						+ "else if self.testedbyItems -> forAll(t1: <"+azureType.getQualifiedName()+"> | t1.state='Closed') then self.state='Released'"
								+ "else true endif endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	
	@Test
	void testExperimentTask0011() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/897", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/897"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> select(child: <"+azureType.getQualifiedName()+"> | child.workItemType='Requirement').size()>0");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	@Test
	void testExperimentTask4() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"if self.affectedbyItems -> exists(b: <"+azureType.getQualifiedName()+"> | b.state<>'Closed') then self.state<>'Released' "
						+ "and self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.state='Work in Progress') and "
						+ "self.affectedbyItems -> select(b1: <"+azureType.getQualifiedName()+"> | b1.state<>'Closed') -> "
						+ "forAll(bug: <"+azureType.getQualifiedName()+"> | bug.relatedItems ->"
						+ " exists(t: <"+azureType.getQualifiedName()+"> | t.workItemType='ChangeRequest'))"
						+ "else if self.affectedbyItems -> forAll(b2: <"+azureType.getQualifiedName()+"> | b2.state='Closed')"
						+ " then self.state='Released'"
						+ "else self.state='Work in Progress' endif"
						+ " endif");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	//TODO: Check for restrictions.
	// Could be warm up task.
	@Test
	void testExperimentTask0() throws ProcessException
	{
		Instance azureIssue2 =artRes.get(new ArtifactIdentifier("UserStudy2Prep/911", "azure_workitem"));
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/906", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/906"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.NAME));
		System.out.println(azureIssue2.getPropertyAsValue(AzureBaseElementType.NAME));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.name.contains(self.name))");
		/*ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> select(child: <"+azureType.getQualifiedName()+"> | child.name.contains(self.name)).size()=self.childItems.size()");*/
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	// In this both with restriction and without restriction makes sense.
	@Test
	void testExperimentTask3() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/901", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/901"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		// related item state check missing.
		// features or bugs as well. change requests.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.testsItems -> forAll(par: <"+azureType.getQualifiedName()+"> | par.relatedItems.size()>1 or par.state='Work in Progress')");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				//ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	@Test
	void testExperimentTask2() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.testedbyItems.size()>0 and "
						+ "child.testedbyItems -> forAll(test: <"+azureType.getQualifiedName()+"> | test.state='Draft'))");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				//ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	@Test
	void testExperimentTask111() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems->select(c: <"+azureType.getQualifiedName()+"> | c.workItemType='Requirement').size()>3 and self.childItems->select(ch: <"+azureType.getQualifiedName()+"> | ch.workItemType='Requirement') -> forAll(child: <"+azureType.getQualifiedName()+"> | child.testedbyItems.size()>1)");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				//ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

// TODO: Minor bug in restriction
	@Test
	void testTask4() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/898", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/898"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		Collection<Property> prop=azureIssue.getProperties();
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		//System.out.println(azureService);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "ForAllTest", 
				"self.childItems"// and child.testedbyItems.size()>0 
				+ "->forAll(child : <"+azureType.getQualifiedName()+">  | child.name.startsWith(self.name)and "
				+ "child.testedbyItems -> exists(test: <"+azureType.getQualifiedName()+"> | test.name.contains(child.name) ))");
		ws.concludeTransaction();
		System.out.println(crt.ruleError());
		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		RepairNode rp=RuleService.repairTree(list.get(0));
		crt.consistencyRuleEvaluations().getValue().forEach
		(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		if(repairTree!=null) 
		{
			ConsistencyUtils.printRepairTree(repairTree);
		}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}






	// 3 test cases but 2 req 1 would not be linked.
	//Test Cases
	// For system authentication requirement there should be at least 3 child requirements with at least one test case associated.
	@Test
	void testExperimentTask1() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems.size()>2 and self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.testedbyItems.size()>1)");
		ws.concludeTransaction();
		System.out.println(crt.getProperty("ruleError").toString());
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> 
		{ 
			RepairNode repairTree = RuleService.repairTree(cr);
			if(repairTree!=null) 
			{
				//ConsistencyUtils.printRepairTree(repairTree);
			}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}


	public static void printRepairActions(RepairNode rnode) {
		StringBuffer sb = new StringBuffer();
		compileRestrictedRepairTree(rnode, 1, sb);
		System.out.println(sb.toString());

	}

	public static void compileRestrictedRepairTree(RepairNode node, int position, StringBuffer printInto) {
		String treeLevel = "\n";
		for (int i = 0; i < position; i++) 
			treeLevel = treeLevel.concat(" -- ");
		if (node instanceof AbstractRepairAction) {
			AbstractRepairAction ra = (AbstractRepairAction)node;
			RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
			if (rootNode != null) {
				printInto.append(treeLevel.concat(compileRestrictedRepair(ra,rootNode.printNodeTree(false, 40))));
				printInto.append("\n"+ rootNode.toTreeString(40));
			} else	
				printInto.append(treeLevel.concat(node.toString()));
		} else
			printInto.append(treeLevel.concat(node.toString()));
		for (RepairNode child : node.getChildren()) {
			compileRestrictedRepairTree(child, position + 1, printInto);
		}
	}
	public static String compileRestrictedRepair(AbstractRepairAction ra, String restriction) {
		String target = ra.getElement() != null ? ((Instance)ra.getElement()).name() : "";
		StringBuffer list = new StringBuffer();
		switch(ra.getOperator()) {
		case ADD:							 
			list.append(String.format("Add to %s of ", ra.getProperty()));
			list.append(target);
			list.append(restriction);
			break;
		case MOD_EQ:
		case MOD_GT:
		case MOD_LT:
		case MOD_NEQ:				
			list.append(String.format("Set the %s of ", ra.getProperty()));
			list.append(target);			
			list.append(" to");
			list.append(restriction);
			break;
		case REMOVE:					
			list.append(String.format("Remove from %s of ", ra.getProperty()));
			list.append(target);
			list.append(restriction);
			break;
		default:
			break;		
		}
		return list.toString();
	}

	@Test
	void testFetchAzure() throws ProcessException {
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		//System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		AzureApi api = new AzureApi();
		byte[] rawJson = api.getWorkItem("UserStudy2Prep", 893);
		String json = new String(rawJson);
		System.out.println(json);
	}

	@Test
	public void testFetchReview() throws Exception {	
		AzureApi api = new AzureApi();
		byte[] rawJson = api.getWorkItem("UserStudy2Prep", 893);
		String json = new String(rawJson);
		System.out.println(json);
	}
	
	@Test
	void testWarmUpTask() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		Collection<Property> prop=azureIssue.getProperties();
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		//System.out.println(azureService);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "crd_TranverseTest", 
				"self.childItems -> forAll(child: <"+azureType.getQualifiedName()+"> | child.name.startsWith(self.name))");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		printRepairActions(rnodeA);	
	}


	@Test
	void testTask1() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/898", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/898"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		Collection<Property> prop=azureIssue.getProperties();
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		//System.out.println(azureService);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "ForAllTest", 
				"self.childItems"//
				+ "->forAll(child : <"+azureType.getQualifiedName()+">  | child.name.startsWith(self.name)) and"
				+ " self.testedbyItems.size()>0");
		ws.concludeTransaction();
		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		RepairNode rp=RuleService.repairTree(list.get(0));
		crt.consistencyRuleEvaluations().getValue().forEach
		(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		if(repairTree!=null) 
		{
			ConsistencyUtils.printRepairTree(repairTree);
		}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}
	@Test
	void testTask2() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		Collection<Property> prop=azureIssue.getProperties();
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		//System.out.println(azureService);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "TranverseTest", 
				"self.childItems.size()>2 and self.childItems -> forAll(child: <"+azureType.getQualifiedName()+">  child.name='X'");
		
		ws.concludeTransaction();
		System.out.println(crt.ruleError());
		/*Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		RepairNode rp=RuleService.repairTree(list.get(0));*/
		crt.consistencyRuleEvaluations().getValue().forEach
		(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		if(repairTree!=null) 
		{
			ConsistencyUtils.printRepairTree(repairTree);
		}
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		if(rnodeA!=null)
			printRepairActions(rnodeA);	
	}

	//TODO: String handling
	@Test
	void testWarmUpTask1() throws ProcessException
	{
		Instance azureIssue = artRes.get(new ArtifactIdentifier("UserStudy2Prep/893", "azure_workitem"));
		InstanceType azureType=azureIssue.getInstanceType();
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("UserStudy2Prep/893"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
		Collection<Property> prop=azureIssue.getProperties();
		System.out.println(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE));
		Instance azureIssue1 = artRes.get(new ArtifactIdentifier("UserStudy2Prep/895", "azure_workitem"));
		String x = azureIssue1.getPropertyAsValue("verificationCriteria").toString();
		//System.out.println(azureService);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, azureType, "TranverseTest", "self.verificationCriteria=\'"+x+"\'");
		System.out.println(crt.ruleError());
		ws.concludeTransaction();

		Collection<ConsistencyRule> prop1=crt.consistencyRuleEvaluations().getValue();
		List<ConsistencyRule> list = new ArrayList<>(prop1);
		RepairNode rp=RuleService.repairTree(list.get(0));
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(azureIssue))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		printRepairActions(rnodeA);	
	}
	
	/*
	@Test
	void test() {
		Optional<Instance> itemOpt = azureService.transferAzureWorkItem("UserStudy1Prep", 875, true);
		assert(itemOpt.isPresent());
		Instance item = itemOpt.get();
		item.name();
	}

	@Test
	void refetchtest() {
		Optional<Instance> itemOpt = azureService.transferAzureWorkItem("UserStudy1Prep", 875, true);
		assert(itemOpt.isPresent());
		Instance item = itemOpt.get();
		item.name();

		Optional<Instance> itemOpt2 = azureService.transferAzureWorkItem("UserStudy1Prep", 875, false);
		assert(itemOpt2.isPresent());
	}
	 */
}
