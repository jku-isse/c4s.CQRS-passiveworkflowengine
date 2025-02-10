package at.jku.isse.passiveprocessengine.frontend.botsupport;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;

import at.jku.isse.PPE3Webfrontend;
import at.jku.isse.ide.assistance.CodeActionExecuter;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.botsupport.EvalData.ConstraintGroundTruth;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI.Message;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.passiveprocessengine.frontend.oclx.IterativeRepairer;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI;
import lombok.Getter;
import lombok.Setter;
import at.jku.isse.passiveprocessengine.frontend.botsupport.TestGenerateOCL;
import at.jku.isse.passiveprocessengine.tests.oclbot.TestLoadAsOCLX;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=PPE3Webfrontend.class)
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class TestWithEvalData {

	@Autowired OpenAI openAI;
	@Autowired SchemaRegistry schemaReg;
	@Autowired ArtifactResolver artRes;
	@Autowired CodeActionExecuterProvider provider;
	
	HumanReadableSchemaExtractor schemaGen;
	Gson gson = new Gson();
	IterativeRepairer repairer;
	
	@BeforeAll
	void setup() {
		schemaGen = new HumanReadableSchemaExtractor(schemaReg);
		repairer = new IterativeRepairer(provider);
	}
	
//	@Test
//	void testSerializeEval() {
//		var result = new EvalResult();
//		var iter1 = new IterationResult(0, "someprompt", "response1");
//		iter1.setError("sd");
//		iter1.setOclxString("nada");
//		result.getIterations().add(iter1);
//		
//		var iter2 = new IterationResult(1, "someprompt2", "response2");
//		iter2.setRemainingError("sd2");
//		iter2.setOclString("nada2");
//		result.getIterations().add(iter2);
//		
//		var json = gson.toJson(result);
//		System.out.println(json);
//	}
	
	@Test
	void testEvalLLMGeneration() throws Exception {
		// run for all eval data, then store as json
		// reset bot after each eval constraint data round
		var groundTruth = EvalData.a1;
		var result = runEvalRound(groundTruth);
		var json = gson.toJson(result);
		System.out.println(json);
	}
	

	private EvalResult runEvalRound(ConstraintGroundTruth groundTruth) throws Exception {
		var result = new EvalResult();
		// resolve context and relevant schema names to Types
		// convert types to schema description
		var schema = compileSchema(groundTruth.getContext(), groundTruth.getRelevantSchema());
		//generate prompt
		var prompt = TestGenerateOCL.compilePrompt(schema, groundTruth.getContext(), groundTruth.getHumanReadable());
		List<Message> messages = new ArrayList<>();
		
		for (int i = 0; i<3 ; i++) {
			var req = openAI.sendRequest(compileRequest(prompt, messages));
			// call LLM
			var answer = TestGenerateOCL.extractAnswer(req);
		
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
				// prepare system side msg
				messages.add(new Message("user", iterResult.getRawResponse(), Instant.now())); // o1 mini does not know 'system'
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
	
	public static OpenAI.ChatRequest compileRequest(String prompt, List<Message> messages) {
		messages.add(new Message("user", prompt.toString(), Instant.now()));
		var req = new OpenAI.ChatRequest(OpenAI.MODEL, messages);
		return req;
	}
	
	public static class EvalResult {
		@Getter List<IterativeRepairer.IterationResult> iterations = new ArrayList<>();
	}
}
