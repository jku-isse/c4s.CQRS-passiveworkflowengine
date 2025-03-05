package at.jku.isse.passiveprocessengine.frontend.botsupport.experiment;

import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeAll;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class TestWithEvalData {

	@Autowired SchemaRegistry schemaReg;
	@Autowired ArtifactResolver artRes;
	@Autowired CodeActionExecuterProvider provider;
	
	
	AbstractBot bot;
	private List<Message> openAImessages = new ArrayList<>(); // in case we use openAI
	private List<at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.Message> ollamaAImessages = new ArrayList<>(); // in case we use openAI
	private String model = "codestral:latest"; // FIX also experiement output file naming!
	
	HumanReadableSchemaExtractor schemaGen;
	Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.serializeNulls()
				.disableHtmlEscaping()				
				.create();
	IterativeRepairer repairer;

	final String filePath = "oclexperimentoutput.json";
	
	@BeforeAll
	void setup() {
		schemaGen = new HumanReadableSchemaExtractor(schemaReg);
		repairer = new IterativeRepairer(provider);
		
		bot = new OllamaAI("http://10.78.115.48:11434/api/chat", model, provider);
	}
	
	@Test
	void testEvalLLMGeneration() throws Exception {
		// run for all eval data, then store as json
		// reset bot after each eval constraint data round
		var groundTruth = EvalData.a1;
		var result = runEvalRound(groundTruth);
		var json = gson.toJson(result);
		System.out.println(json);
		Files.writeString(Paths.get("Task_codestral_"+groundTruth.getTaskId()+filePath), json);
	}
	

	private EvalResult runEvalRound(ConstraintGroundTruth groundTruth) throws Exception {
		openAImessages.clear(); // only for OPENAI
		ollamaAImessages.clear();
		var result = new EvalResult(groundTruth.getTaskId());
		// resolve context and relevant schema names to Types
		// convert types to schema description
		var schema = compileSchema(groundTruth.getContext(), groundTruth.getRelevantSchema());
		//generate prompt
		var prompt = bot.compilePrompt(schema, groundTruth.getContext(), groundTruth.getHumanReadable(), null);
				
		for (int i = 0; i<3 ; i++) {			
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
