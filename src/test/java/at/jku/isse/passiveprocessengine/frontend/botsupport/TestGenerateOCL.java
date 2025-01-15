package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI.Choice;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI.Message;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
class TestGenerateOCL {


	OpenAI bot;

	
	@BeforeAll
	void setup() {
		var props = new UIConfig("Test");		
		try {
			InputStream input = new FileInputStream("application.properties") ;
            props.load(input);
            bot = new OpenAI(props.getProperty("openai.apikey"));
		} catch(IOException e) {
			String msg = "No ./application.properties found";
			log.error(msg);
			throw new RuntimeException(msg);
		}		
	}
	
	@Test
	void testGenerateIssueToRequirementsStatusOCL() throws Exception {
		StringBuffer prompt = new StringBuffer();
    	prompt.append(OCLBot.TASK_PROMPT);
    	prompt.append(String.format(OCLBot.SCHEMA_PROMPT_TEMPLATE, schemaReqAndIssue));
    	prompt.append(String.format(OCLBot.OCL_CONTEXT_PROMPT_TEMPLATE, "Issue"));
    	prompt.append(OCLBot.TASKFOLLOWS_PROMPT);
    	prompt.append("From a issue, I like to check whether all succeeding requirements are in status released \r\n");
    	prompt.append(OCLBot.OUTPUTFORMAT_PROMPT);
    	String answer = ask(prompt.toString());
    	System.out.println(answer);
    	
//    	```ocl
//    	context Issue
//    	inv: self.successorItems->select(w | w.workItemType = 'Requirement')->forAll(r | r.state = 'Released')
//    	```
	}

	private String ask(String prompt) throws Exception {
		return extractAnswer(bot.sendRequest(compileRequest(prompt)));
	}
	
	private OpenAI.ChatRequest compileRequest(String prompt) {
		List<Message> messages = new ArrayList<>();
		messages.add(new Message("user", prompt.toString(), Instant.now()));
		var req = new OpenAI.ChatRequest(OpenAI.MODEL, messages);
		return req;
	}
	
	private String extractAnswer(OpenAI.ChatResponse response) {
		var responses = response.getChoices().stream().map(Choice::getMessage).collect(Collectors.toList());
		String answer = responses.get(0).getContent();
		return answer;
	}
	
	static String schemaReqAndIssue = 
			"Generic object type azure_workitem contains following properties:\r\n"
			+ " activatedBy of type  user\r\n"
			+ " activatedDate of type  STRING\r\n"
			+ " affectedByItems of multiple  azure_workitem\r\n"
			+ " affectsItems of multiple  azure_workitem\r\n"
			+ " areaId of type  INTEGER\r\n"
			+ " areaLevel1 of type  STRING\r\n"
			+ " areaLevel2 of type  STRING\r\n"
			+ " areaLevel3 of type  STRING\r\n"
			+ " areaLevel4 of type  STRING\r\n"
			+ " areaLevel5 of type  STRING\r\n"
			+ " areaLevel6 of type  STRING\r\n"
			+ " areaLevel7 of type  STRING\r\n"
			+ " areaPath of type  STRING\r\n"
			+ " artifactLinkItems of multiple  azure_workitem\r\n"
			+ " assignedTo of type  user\r\n"
			+ " attachedFileCount of type  INTEGER\r\n"
			+ " attachedFileItems of multiple  azure_workitem\r\n"
			+ " authorizedAs of type  user\r\n"
			+ " authorizedDate of type  STRING\r\n"
			+ " boardColumn of type  STRING\r\n"
			+ " boardColumnDone of type  BOOLEAN\r\n"
			+ " boardLane of type  STRING\r\n"
			+ " changedBy of type  user\r\n"
			+ " changedDate of type  STRING\r\n"
			+ " childItems of multiple  azure_workitem\r\n"
			+ " closedBy of type  STRING\r\n"
			+ " closedDate of type  STRING\r\n"
			+ " commentCount of type  INTEGER\r\n"
			+ " comments of multiple  workItem_comment\r\n"
			+ " consumesFromItems of multiple  azure_workitem\r\n"
			+ " createdBy of type  user\r\n"
			+ " createdDate of type  STRING\r\n"
			+ " description of type  STRING\r\n"
			+ " duplicateItems of multiple  azure_workitem\r\n"
			+ " duplicateOfItems of multiple  azure_workitem\r\n"
			+ " externalDefaultID of type  STRING\r\n"
			+ " externalLinkCount of type  INTEGER\r\n"
			+ " externalType of type  STRING\r\n"
			+ " externalUrl of type  STRING\r\n"
			+ " history of type  STRING\r\n"
			+ " hyperlinkCount of type  INTEGER\r\n"
			+ " hyperlinkItems of multiple  azure_workitem\r\n"
			+ " id of type  INTEGER\r\n"
			+ " iterationId of type  INTEGER\r\n"
			+ " iterationLevel1 of type  STRING\r\n"
			+ " iterationLevel2 of type  STRING\r\n"
			+ " iterationLevel3 of type  STRING\r\n"
			+ " iterationLevel4 of type  STRING\r\n"
			+ " iterationLevel5 of type  STRING\r\n"
			+ " iterationLevel6 of type  STRING\r\n"
			+ " iterationLevel7 of type  STRING\r\n"
			+ " iterationPath of type  STRING\r\n"
			+ " lastUpdate of type  DATE\r\n"
			+ " markedDeleted of type  BOOLEAN\r\n"
			+ " name of type  STRING\r\n"
			+ " nodeName of type  STRING\r\n"
			+ " parent of type  INTEGER\r\n"
			+ " parentItems of multiple  azure_workitem\r\n"
			+ " personId of type  INTEGER\r\n"
			+ " predecessorItems of multiple  azure_workitem\r\n"
			+ " producesForItems of multiple  azure_workitem\r\n"
			+ " project of type  project\r\n"
			+ " reason of type  STRING\r\n"
			+ " referencedByItems of multiple  azure_workitem\r\n"
			+ " referencesItems of multiple  azure_workitem\r\n"
			+ " relatedItems of multiple  azure_workitem\r\n"
			+ " relatedLinkCount of type  INTEGER\r\n"
			+ " remoteLinkCount of type  INTEGER\r\n"
			+ " remoteRelatedItems of multiple  azure_workitem\r\n"
			+ " resolvedBy of type  STRING\r\n"
			+ " resolvedDate of type  STRING\r\n"
			+ " rev of type  INTEGER\r\n"
			+ " revisedDate of type  STRING\r\n"
			+ " sharedStepsItems of multiple  azure_workitem\r\n"
			+ " state of type  STRING\r\n"
			+ " stateChangeDate of type  STRING\r\n"
			+ " successorItems of multiple  azure_workitem\r\n"
			+ " tags of type  STRING\r\n"
			+ " testCaseItems of multiple  azure_workitem\r\n"
			+ " testedByItems of multiple  azure_workitem\r\n"
			+ " testsItems of multiple  azure_workitem\r\n"
			+ " title of type  STRING\r\n"
			+ " watermark of type  INTEGER\r\n"
			+ " workItemType of type  STRING\r\n"
			+ "\r\n"
			+ "Object type CrIssueFd contains following properties:\r\n"
			+ " category of type  STRING\r\n"
			+ " ccbDecision of type  STRING\r\n"
			+ " impactOnArchitecture of type  STRING\r\n"
			+ " impactOnDevelopment of type  STRING\r\n"
			+ " impactOnTest of type  STRING\r\n"
			+ " severity of type  STRING\r\n"
			+ " size of type  FLOAT\r\n"
			+ " stackRank of type  FLOAT\r\n"
			+ " targetVersion of type  STRING\r\n"
			+ "\r\n"
			+ "Object type Requirement contains following properties:\r\n"
			+ " blocked of type  STRING\r\n"
			+ " committed of type  STRING\r\n"
			+ " finishDate of type  STRING\r\n"
			+ " impactAssessmentHtml of type  STRING\r\n"
			+ " integrationBuild of type  STRING\r\n"
			+ " originalEstimate of type  FLOAT\r\n"
			+ " priority of type  INTEGER\r\n"
			+ " requirementType of type  STRING\r\n"
			+ " resolvedReason of type  STRING\r\n"
			+ " size of type  FLOAT\r\n"
			+ " stackRank of type  FLOAT\r\n"
			+ " startDate of type  STRING\r\n"
			+ " subjectMatterExpert1 of type  user\r\n"
			+ " subjectMatterExpert2 of type  user\r\n"
			+ " subjectMatterExpert3 of type  user\r\n"
			+ " triage of type  STRING\r\n"
			+ " userAcceptanceTest of type  STRING\r\n"
			+ " valueArea of type  STRING\r\n"
			+ " verificationCriteria of type  STRING\r\n"
			+ "\r\n"
			+ "Object type Issue contains following properties:\r\n"
			+ " analysis of type  STRING\r\n"
			+ " correctiveActionActualResolution of type  STRING\r\n"
			+ " correctiveActionPlan of type  STRING\r\n"
			+ " discipline of type  STRING\r\n"
			+ " escalate of type  STRING\r\n"
			+ " integrationBuild of type  STRING\r\n"
			+ " originalEstimate of type  FLOAT\r\n"
			+ " priority of type  INTEGER\r\n"
			+ " remainingWork of type  FLOAT\r\n"
			+ " resolvedReason of type  STRING\r\n"
			+ " severity of type  STRING\r\n"
			+ " size of type  FLOAT\r\n"
			+ " stackRank of type  FLOAT\r\n"
			+ " targetResolveDate of type  STRING\r\n"
			+ " triage of type  STRING";
			
}
