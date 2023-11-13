package at.jku.isse.passiveprocessengine.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.azure.model.AzureBaseElementType;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.toolbridges.jiraToJama.JiraJamaItemAugmentor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ConnectorTests {
	
	@Autowired
	WorkspaceService workspaceService;
	
	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ProcessRegistry procReg;
	
	@Autowired
	ArtifactResolver artRes;
	
	@Test
	void testFetchAzure() throws ProcessException {
//		Instance gitIssue = artRes.get(new ArtifactIdentifier("p2f.processguidance/issues/4", "git_issue"));
//		assert(gitIssue.name().equalsIgnoreCase("p2f.processguidance/issues/4"));
		Instance azureIssue = artRes.get(new ArtifactIdentifier("CEPS-1/38", "azure_workitem"));
		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("CEPS-1/38"));
		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
	}

//	@Test
//	void testFetchJiraFRQ() throws ProcessException {
//		Instance issue = artRes.get(new ArtifactIdentifier("PVCSG-3", "jira_core_artifact", IJiraService.JiraIdentifier.JiraIssueKey.toString()));
//		assert(((String) issue.getPropertyAsValue("key")).equalsIgnoreCase("PVCSG-3"));
//		
//	}
	
	
	@Test
	void testFetchJiraFRQAndJama() throws ProcessException {
		Instance issue = artRes.get(new ArtifactIdentifier("PVCSG-5048", "jira_core_artifact", IJiraService.JiraIdentifier.JiraIssueKey.toString()));
		Instance jama = issue.getPropertyAsInstance(JiraJamaItemAugmentor.JIRA2JAMALINKPROPERTYNAME);
		assert(jama != null);
		
	}
}
