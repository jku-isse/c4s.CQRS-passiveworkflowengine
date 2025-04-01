package at.jku.isse.passiveprocessengine.frontend.botsupport.experiment;

import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.AbstractBot;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot;
import at.jku.isse.passiveprocessengine.frontend.botsupport.experiment.EvalData.ConstraintGroundTruth;
import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI;
import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.ChatRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.openai.OpenAI;
import at.jku.isse.passiveprocessengine.frontend.botsupport.openai.OpenAI.Message;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.passiveprocessengine.frontend.oclx.IterativeRepairer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static at.jku.isse.passiveprocessengine.frontend.botsupport.experiment.EvalData.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class PartialRerun {

	@Autowired SchemaRegistry schemaReg;
	@Autowired ArtifactResolver artRes;
	@Autowired CodeActionExecuterProvider provider;
	
	
	AbstractBot bot;
	private List<Message> openAImessages = new ArrayList<>(); // in case we use openAI
	private List<at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.Message> ollamaAImessages = new ArrayList<>(); // in case we use ollamaAI
	 
	//codestral:latest  qwen2.5-coder:32b        codegeex4:latest       gemma2:27b     llama3.3:latest      deepseek-r1:70b
	private String model = "codegeex4:latest"; // sync with experiment output file naming below!
	private String modelForFilePath = "codegeex4";
	
	HumanReadableSchemaExtractor schemaGen;
	Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.serializeNulls()
				.disableHtmlEscaping()				
				.create();
	IterativeRepairer repairer;

	final String filePath = "oclexperimentoutput.json";
	
	@BeforeAll
	void setup() throws IOException {
		schemaGen = new HumanReadableSchemaExtractor(schemaReg);
		repairer = new IterativeRepairer(provider);		
		bot = new OllamaAI("http://10.78.115.48:11434/api/chat", model, provider);
		
	}
	
	@Test 
	void runEvaluationAndLog() throws Exception {
		var groundTruth = a2;
		var result = runPartialRound(groundTruth);
		var json = gson.toJson(result);
		System.out.println(json);
		if (bot instanceof OllamaAI)
			Files.writeString(Paths.get("Task_"+modelForFilePath+"_"+groundTruth.getTaskId()+"_"+filePath), json);
		else 
			Files.writeString(Paths.get("Task_o1-mini_"+groundTruth.getTaskId()+"_"+filePath), json);
	}
	
	private EvalResult runPartialRound(ConstraintGroundTruth groundTruth) throws Exception {
		openAImessages.clear(); // only for OPENAI
		ollamaAImessages.clear();
		var result = new EvalResult(groundTruth.getTaskId());
		
		//generate prompt
		var prompt = """ 
				 You are tasked with providing an OCL rule based on the detailed description provided further below. \r\n Only use properties in the OCL rule from the following schema.\r\n  \r\nGeneric object type azure_workitem contains following properties:\r\n activatedBy of type  user\r\n activatedDate of type  STRING\r\n affectedByItems of multiple  azure_workitem\r\n affectsItems of multiple  azure_workitem\r\n areaId of type  INTEGER\r\n areaLevel1 of type  STRING\r\n areaLevel2 of type  STRING\r\n areaLevel3 of type  STRING\r\n areaLevel4 of type  STRING\r\n areaLevel5 of type  STRING\r\n areaLevel6 of type  STRING\r\n areaLevel7 of type  STRING\r\n areaPath of type  STRING\r\n artifactLinkItems of multiple  azure_workitem\r\n assignedTo of type  user\r\n attachedFileCount of type  INTEGER\r\n attachedFileItems of multiple  azure_workitem\r\n authorizedAs of type  user\r\n authorizedDate of type  STRING\r\n boardColumn of type  STRING\r\n boardColumnDone of type  BOOLEAN\r\n boardLane of type  STRING\r\n changedBy of type  user\r\n changedDate of type  STRING\r\n childItems of multiple  azure_workitem\r\n closedBy of type  STRING\r\n closedDate of type  STRING\r\n commentCount of type  INTEGER\r\n comments of multiple  workItem_comment\r\n consumesFromItems of multiple  azure_workitem\r\n createdBy of type  user\r\n createdDate of type  STRING\r\n description of type  STRING\r\n duplicateItems of multiple  azure_workitem\r\n duplicateOfItems of multiple  azure_workitem\r\n externalDefaultID of type  STRING\r\n externalLinkCount of type  INTEGER\r\n externalType of type  STRING\r\n externalUrl of type  STRING\r\n history of type  STRING\r\n hyperlinkCount of type  INTEGER\r\n hyperlinkItems of multiple  azure_workitem\r\n id of type  INTEGER\r\n iterationId of type  INTEGER\r\n iterationLevel1 of type  STRING\r\n iterationLevel2 of type  STRING\r\n iterationLevel3 of type  STRING\r\n iterationLevel4 of type  STRING\r\n iterationLevel5 of type  STRING\r\n iterationLevel6 of type  STRING\r\n iterationLevel7 of type  STRING\r\n iterationPath of type  STRING\r\n lastUpdate of type  DATE\r\n markedDeleted of type  BOOLEAN\r\n name of type  STRING\r\n nodeName of type  STRING\r\n parent of type  INTEGER\r\n parentItems of multiple  azure_workitem\r\n personId of type  INTEGER\r\n predecessorItems of multiple  azure_workitem\r\n producesForItems of multiple  azure_workitem\r\n project of type  project\r\n reason of type  STRING\r\n referencedByItems of multiple  azure_workitem\r\n referencesItems of multiple  azure_workitem\r\n relatedItems of multiple  azure_workitem\r\n relatedLinkCount of type  INTEGER\r\n remoteLinkCount of type  INTEGER\r\n remoteRelatedItems of multiple  azure_workitem\r\n resolvedBy of type  STRING\r\n resolvedDate of type  STRING\r\n rev of type  INTEGER\r\n revisedDate of type  STRING\r\n sharedStepsItems of multiple  azure_workitem\r\n state of type  STRING\r\n stateChangeDate of type  STRING\r\n successorItems of multiple  azure_workitem\r\n tags of type  STRING\r\n testCaseItems of multiple  azure_workitem\r\n testedByItems of multiple  azure_workitem\r\n testsItems of multiple  azure_workitem\r\n title of type  STRING\r\n watermark of type  INTEGER\r\n workItemType of type  STRING\r\n\r\nObject type Review contains following properties:\r\n actualAttendee1 of type  user\r\n actualAttendee2 of type  user\r\n actualAttendee3 of type  user\r\n actualAttendee4 of type  user\r\n actualAttendee5 of type  user\r\n actualAttendee6 of type  user\r\n actualAttendee7 of type  user\r\n actualAttendee8 of type  user\r\n calledBy of type  user\r\n calledDate of type  STRING\r\n discipline of type  STRING\r\n integrationBuild of type  STRING\r\n meetingType of type  STRING\r\n minutes of type  STRING\r\n needRereview of type  BOOLEAN\r\n optionalAttendee1 of type  user\r\n optionalAttendee2 of type  user\r\n optionalAttendee3 of type  user\r\n optionalAttendee4 of type  user\r\n optionalAttendee5 of type  user\r\n optionalAttendee6 of type  user\r\n optionalAttendee7 of type  user\r\n optionalAttendee8 of type  user\r\n purpose of type  STRING\r\n remainingWork of type  FLOAT\r\n requiredAttendee1 of type  user\r\n requiredAttendee2 of type  user\r\n requiredAttendee3 of type  user\r\n requiredAttendee4 of type  user\r\n requiredAttendee5 of type  user\r\n requiredAttendee6 of type  user\r\n requiredAttendee7 of type  user\r\n requiredAttendee8 of type  user\r\n resolvedReason of type  STRING\r\n scheduled of type  STRING\r\n size of type  FLOAT\r\n stackRank of type  FLOAT\r\n\r\nObject type Reviewfinding contains following properties:\r\n findingcategory of type  STRING\r\n\r\nObject type Requirement contains following properties:\r\n blocked of type  STRING\r\n committed of type  STRING\r\n finishDate of type  STRING\r\n impactAssessmentHtml of type  STRING\r\n integrationBuild of type  STRING\r\n originalEstimate of type  FLOAT\r\n priority of type  INTEGER\r\n requirementType of type  STRING\r\n resolvedReason of type  STRING\r\n size of type  FLOAT\r\n stackRank of type  FLOAT\r\n startDate of type  STRING\r\n subjectMatterExpert1 of type  user\r\n subjectMatterExpert2 of type  user\r\n subjectMatterExpert3 of type  user\r\n triage of type  STRING\r\n userAcceptanceTest of type  STRING\r\n valueArea of type  STRING\r\n verificationCriteria of type  STRING\r\nGeneric object type ProcessStep contains following properties:\r\n in_CRs of multiple  azure_workitem\r\n out_Bugs of multiple  azure_workitem\r\n out_REQs of multiple  azure_workitem\r\n out_TCs of multiple  azure_workitem \r\n  Make sure you use only these properties in your answer. \r\n The context of the OCL rule is an object of type ProcessStep_ReqStateAnalysis_Task2a \r\n The exact task is the following: \r\nEnsure that all Requirements in state 'released' trace (via 'successor') to at least one Review, all which must not have any 'open' Review Findings. Remember, only provide the OCL rule, but no explanation unless explicitly asked for it \r\n
				""";
		var answer1 = """
				Here's an OCL rule that fulfills the task:\n\n```ocl\ncontext ProcessStep_ReqStateAnalysis_Task2a\ninv: \n  self.out_REQs->exists(req : Requirement | req.state = 'released' and req.successorItems->exists(review : Review | review.reviewfindingItems->forAll(finding : Reviewfinding | finding.findingcategory <> 'open')) )\n```\n\nThis rule checks that for each Requirement in the `out_REQs` collection of a ProcessStep_ReqStateAnalysis_Task2a object, there exists at least one Review in its `successorItems` collection such that all Review Findings associated with that Review are not open.
				""";
		var iterResult1 = repairer.checkResponse(groundTruth.getContext(), prompt, answer1, 0);
		result.getIterations().add(iterResult1);
		String error1 = null;
		if (iterResult1.getRemainingError() != null) // a remaining error or
			error1 = iterResult1.getRemainingError();
		else if (iterResult1.getOclString() == null) // incorrect ocl highlighting in raw text
			error1 = "could not find OCL String";
		else if (iterResult1.getOclxString() == null) // incorrect ocl syntax
			error1 = "Incorrect OCL syntax "+iterResult1.getErrors();
		// if error --> ask LLM again with error message (3x max)
		if (error1 != null) {				
			// set new prompt
			prompt = OCLBot.REPAIR_REQUEST_PREFIX_PROMPT+error1;
		} else
			return result;
		//remaining 2 iterations
		for (int i = 1; i<3 ; i++) {			
			// call LLM
			var answer = getResponse(prompt, bot); 		
			var iterResult = repairer.checkResponse(groundTruth.getContext(), prompt, answer, i);
			result.getIterations().add(iterResult);
		
			String error = null;
			if (iterResult.getRemainingError() != null) // a remaining error or
				error = iterResult.getRemainingError();
			else if (iterResult.getOclString() == null) // incorrect ocl highlighting in raw text
				error = "could not find OCL String";
			else if (iterResult.getOclxString() == null) // incorrect ocl syntax
				error = "Incorrect OCL syntax "+iterResult.getErrors();
			// if error --> ask LLM again with error message (3x max)
			if (error != null) {				
				// set new prompt
				prompt = OCLBot.REPAIR_REQUEST_PREFIX_PROMPT+error;
			} else
				break;
		}
		return result;
	}

	private String compileSchema(String processStep, Set<String> artifactTypes) {
		// resolve context and relevant schema names to Types
				// convert types to schema description
		var steps = schemaReg.findAllInstanceTypesByFQN(processStep);
		assertEquals(1, steps.size());
		var types = new ArrayList<PPEInstanceType>();
		types.addAll(steps);
		var artTypes = artRes.getAvailableInstanceTypes().stream()
				.filter(type -> artifactTypes.contains(type.getName()))
				.toList();
		assertEquals(artifactTypes.size(), artTypes.size());
		types.addAll(artTypes);
		Map<PPEInstanceType, List<PPEInstanceType>> subsetGroups =  schemaGen.clusterTypes(
				types						);
		assertEquals(2, subsetGroups.size());
		StringBuffer sb = new StringBuffer();
		subsetGroups.entrySet().forEach(entry -> {
			var props = schemaGen.processSubgroup(entry.getKey(), entry.getValue());
			var schema = schemaGen.compileSchemaList(entry.getKey(),  entry.getValue(), props.getKey(), props.getValue());
			sb.append(schema);
		});						
		return sb.toString();
	}
	
	
	private String getResponse(String prompt, AbstractBot bot) throws Exception {
		if (bot instanceof OpenAI openAI) {
			openAImessages.add(new Message("user", prompt.toString(), Instant.now()));
			var req = new OpenAI.ChatRequest(OpenAI.MODEL, openAImessages);
			var resp = openAI.sendRequest(req);			
			var rawResp = openAI.extractAnswerFromResponse(resp);
			// prepare system side msg, in case of error
			openAImessages.add(new Message("user", rawResp, Instant.now())); // o1 mini does not know 'system'
			return rawResp;
		}
		if (bot instanceof OllamaAI ollamaAI) {
		//	var req = new OllamaAI.ChatRequest(prompt, null)
			//ollamaAI.sendRequest(ollamaAI.compileRequest())
			ollamaAImessages.add(new at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.Message("user", prompt));
			var req = new ChatRequest(model, ollamaAImessages);
			var resp = ollamaAI.sendRequest(req);
			var rawResp = ollamaAI.extractAnswerFromResponse(resp);
			ollamaAImessages.add(new at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.Message("assistant", rawResp));			
			return rawResp;
		}
		return "";
	}	
	
	@RequiredArgsConstructor
	public static class EvalResult {
		@Getter final String taskId;
		@Getter List<IterativeRepairer.IterationResult> iterations = new ArrayList<>();
	}
}
