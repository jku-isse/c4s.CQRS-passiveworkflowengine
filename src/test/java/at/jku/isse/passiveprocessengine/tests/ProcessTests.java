package at.jku.isse.passiveprocessengine.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ProcessTests {

	@Autowired
	WorkspaceService workspaceService;
	
	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ProcessRegistry procReg;
	
	@Autowired
	ArtifactResolver artRes;
	
	@Autowired
	RepairAnalyzer repairanalyzer;
	
	@Autowired
	ProcessQAStatsMonitor qastats;
	
	@Autowired 
	UsageMonitor usageMonitor;
	
	@BeforeEach
	void setUp() throws Exception {
	}
	

//	@Test
//	void testPrematureGithubIssueWithSielaV3() throws ProcessException {
//		ArtifactIdentifier gitAI = new ArtifactIdentifier("p2f.processguidance/issues/16", "git_issue");
//		//Instance gitIssue = artRes.get(gitAI);
//		//reqDelegate.initialize();
//		reqDelegate.instantiateProcess("TestProc", Map.of("story" , gitAI), "SIELA-Github-v3");
//		
//		ProcessInstance proc = reqDelegate.getProcess("SIELA-Github-v3_[story:p2f.processguidance/issues/16]");
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//	}
//
//	@Test
//	void testUserstudyRAv1() throws ProcessException {
//		ArtifactIdentifier gitAI = new ArtifactIdentifier("UserStudy1Prep/117", "azure_workitem");
//		reqDelegate.instantiateProcess("TestProc", Map.of("CR" , gitAI), "Userstudy1-RequirementsMangement-V1");
//		
//		ProcessInstance proc = reqDelegate.getProcess("Userstudy1-RequirementsMangement-V1_[CR:CR1]");
//		
//		System.out.println(repairanalyzer.stats2Json(repairanalyzer.getSerializableStats()));
//		System.out.println(qastats.stats2Json(qastats.stats.values()));
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//	}
//	
	@Test
	void testSielaWithJiraV1() throws ProcessException {
		ArtifactIdentifier gitAI = new ArtifactIdentifier("SIELA-20", IJiraService.JiraIdentifier.JiraIssueKey.toString());
		//Instance gitIssue = artRes.get(gitAI);
		//reqDelegate.initialize();
		//ProcessInstance proc = reqDelegate.instantiateProcess("TestProc", Map.of("story" , gitAI), "SIELA-jira");
//				
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//		TestUtils.printProcessWithRepairs(proc);
	}
//	
//	@Test
//	void testAzureSielaV2() throws ProcessException {
//		
//		ArtifactIdentifier gitAI = new ArtifactIdentifier("SIELA-Eval/833", "azure_workitem");
//		ProcessInstance proc = reqDelegate.instantiateProcess("TestProc", Map.of("story" , gitAI), "SIELA-azure-V2");
//
//		System.out.println(repairanalyzer.stats2Json(repairanalyzer.getSerializableStats()));
//		System.out.println(qastats.stats2Json(qastats.stats.values()));
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//	}
//	
//	
//	@Test
//	void testAzureMVPv1() throws ProcessException {
//		ArtifactIdentifier gitAI = new ArtifactIdentifier("UserStudy1Prep/868", "azure_workitem");
//		reqDelegate.instantiateProcess("TestProc", Map.of("UserRequirements" , gitAI), "SystemRequirementsAnalysis");
////		
//		ProcessInstance proc = reqDelegate.getProcess("SystemRequirementsAnalysis_[L3 Requirements:UserReqCollection]");
////		
//		System.out.println(repairanalyzer.stats2Json(repairanalyzer.getSerializableStats()));
//		System.out.println(qastats.stats2Json(qastats.stats.values()));
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//	}
	
}
