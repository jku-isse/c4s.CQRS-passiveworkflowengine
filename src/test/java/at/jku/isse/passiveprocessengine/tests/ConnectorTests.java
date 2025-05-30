package at.jku.isse.passiveprocessengine.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ConnectorTests {
	
	
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
//		PPEInstance azureIssue = artRes.get(new ArtifactIdentifier("CEPS-1/38", "azure_workitem"));
//		assert(((String) azureIssue.getPropertyAsValue("id")).equalsIgnoreCase("CEPS-1/38"));
//		assertEquals(azureIssue.getPropertyAsValue(AzureBaseElementType.ASSIGNEE),azureIssue.getPropertyAsValue(AzureBaseElementType.CREATOR));
	}

//	@Test
//	void testFetchJiraFRQ() throws ProcessException {
//		Instance issue = artRes.get(new ArtifactIdentifier("PVCSG-3", "jira_core_artifact", IJiraService.JiraIdentifier.JiraIssueKey.toString()));
//		assert(((String) issue.getPropertyAsValue("key")).equalsIgnoreCase("PVCSG-3"));
//		
//	}
	
	
//	@Test
//	void testFetchJiraFRQAndJama() throws ProcessException {
//		PPEInstance issue = artRes.get(new ArtifactIdentifier("PVCSG-5048", "jira_core_artifact", IJiraService.JiraIdentifier.JiraIssueKey.toString()));
//		PPEInstance jama = issue.getPropertyAsInstance(JiraJamaItemAugmentor.JIRA2JAMALINKPROPERTYNAME);
//		assert(jama != null);
//		
//	}
}
