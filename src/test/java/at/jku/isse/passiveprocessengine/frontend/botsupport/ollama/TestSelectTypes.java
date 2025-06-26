package at.jku.isse.passiveprocessengine.frontend.botsupport.ollama;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import at.jku.isse.passiveprocessengine.frontend.botsupport.TypeSelectionResponse;
import at.jku.isse.passiveprocessengine.frontend.botsupport.experiment.EvalData;
import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.ChatRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.Message;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
class TestSelectTypes {

	static UIConfig props;
	
	String types1 = "Documentation\r\n"
			+ "ChangeRequest\r\n"
			+ "azure_workitem\r\n"
			+ "FunctionalSpecification\r\n"
			+ "WorkPackage\r\n"
			+ "Epic\r\n"
			+ "CodeReviewResponse\r\n"
			+ "CrIssueFd\r\n"
			+ "Review\r\n"
			+ "workItem_comment\r\n"
			+ "Modeldocumentation\r\n"
			+ "Requirement\r\n"
			+ "Bug\r\n"
			+ "CodeReviewRequest\r\n"
			+ "Testreport\r\n"
			+ "Risk\r\n"
			+ "project\r\n"
			+ "Feature\r\n"
			+ "SharedParameter\r\n"
			+ "user\r\n"
			+ "FeedbackResponse\r\n"
			+ "TestPlan\r\n"
			+ "TestCase\r\n"
			+ "TestSuite\r\n"
			+ "Task\r\n"
			+ "FeedbackRequest\r\n"
			+ "Mathmodel\r\n"
			+ "L3Requirements\r\n"
			+ "Fc\r\n"
			+ "process_config_base\r\n"
			+ "SharedSteps\r\n"
			+ "Issue\r\n"
			+ "Reviewfinding\r\n"
			+ "Bc";
	
	String promptTemplate1 = "Which of these types %s is needed for evaluating following constraint %s Make sure to only return a json list without any explanation.";
	
	OllamaAI bot;
	String model;

	
	@BeforeAll
	void setup() {
		var props = new UIConfig("Test");		
		try {
			InputStream input = new FileInputStream("application.properties") ;
            props.load(input);
            var url = props.getProperty("ollama.url");
            //model = props.getProperty("ollama.model");
            model = "deepseek-r1:70b";
            bot = new OllamaAI(url, model, null); // no code action repair necessary here
		} catch(IOException e) {
			String msg = "No ./application.properties found";
			log.error(msg);
			throw new RuntimeException(msg);
		}		
	}
	
	public String compilePrompt(String constraint) {
		return String.format(promptTemplate1, types1, constraint);
	}
		
	
	private List<String> ask(String prompt) throws Exception {
		return extractAnswer(bot.sendRequest(compileRequest(prompt)));
	}
	
	private ChatRequest compileRequest(String prompt) {
		List<Message> messages = new ArrayList<>();
		messages.add(new Message("user", prompt.toString()));
		var req = new OllamaAI.ChatRequest(model, messages);
		return req;
	}
	
	public List<String> extractAnswer(OllamaAI.ChatResponse response) {		
		String answer = response.getMessage().getContent();		
		try {
			var selection = TypeSelectionResponse.buildFromString(answer);
			return selection.getTypes();
		} catch (Exception e) {
			return Collections.emptyList();
		}
		
	}
	
	public List<Float> calcPrecisionAndRecall(Collection<String> selection, Set<String> groundTruth) {					
			// calc precision and recall:
			var tpSet = new HashSet<String>(groundTruth);
			tpSet.retainAll(selection);
			var fpSet = new HashSet<String>(selection);
			fpSet.removeAll(groundTruth);
			
			float truePos =  tpSet.size();
			float falsePos = fpSet.size();
			float falseNeg = groundTruth.size() - tpSet.size(); 
			var prec = truePos / (truePos + falsePos);
			var recall = truePos / (truePos + falseNeg);
			return List.of( prec, recall);				
	}
	
	@Test
	void testExtractA1() throws Exception {
		var evalData = EvalData.a1;
		
		var groundTruth = evalData.getRelevantSchema(); 		
		var selection = ask(evalData.getHumanReadable());				
		var precAndRecall = calcPrecisionAndRecall(selection, groundTruth);
		
		log.debug(String.format("For %s expected %s and got %s", evalData.getHumanReadable(), evalData.getRelevantSchema(), selection));
		log.debug(precAndRecall.toString());
		
	}
	
	
	
	
	@Test
	void testPrecRecall() {
		var pAndR = calcPrecisionAndRecall(Set.of("A", "B"), Set.of("A", "B"));
		assertEquals(1.0f, pAndR.get(0));
		assertEquals(1.0f, pAndR.get(1));
		
		pAndR = calcPrecisionAndRecall(Set.of("A", "B"), Set.of("A", "B", "C", "E"));
		assertEquals(1.0f, pAndR.get(0));
		assertEquals(0.5f, pAndR.get(1));
		
		pAndR = calcPrecisionAndRecall(Set.of("A", "B", "C", "E"), Set.of("A", "B"));
		assertEquals(0.5f, pAndR.get(0));
		assertEquals(1.0f, pAndR.get(1));
		
		pAndR = calcPrecisionAndRecall(Set.of("C"), Set.of("A", "B"));
		assertEquals(0.0f, pAndR.get(0));
		assertEquals(0.0f, pAndR.get(1));
	}
	
	

	@ParameterizedTest
	@ValueSource(strings = {
			"```json\r\n"
					+ "[\"Review\", \"azure_workitem\"]\r\n"
					+ "```",
			"```json\r\n"
			+ "[\"Issue\", \"Requirement\", \"L3Requirements\"]\r\n"
			+ "```"
	})
	void testExtractSelection(String answer) throws Exception {		
		var selection = TypeSelectionResponse.buildFromString(answer);
		assertNotNull(selection);
	}
	
	/***
	 * {  "id": "chatcmpl-ApsyZ9DAjGkOCz2YDYKJtTe3CDpnX",  "object": "chat.completion",  "created": 1736929419,  "model": "o1-mini-2024-09-12",  "choices": [    {      "index": 0,      "message": {        "role": "assistant",        "content": "```json\n[\"Issue\", \"Requirement\", \"L3Requirements\"]\n```",        "refusal": null      },      "finish_reason": "stop"    }  ],  "usage": {    "prompt_tokens": 194,    "completion_tokens": 348,    "total_tokens": 542,    "prompt_tokens_details": {      "cached_tokens": 0,      "audio_tokens": 0    },    "completion_tokens_details": {      "reasoning_tokens": 320,      "audio_tokens": 0,      "accepted_prediction_tokens": 0,      "rejected_prediction_tokens": 0    }  },  "service_tier": "default",  "system_fingerprint": "fp_f56e40de61"}
	 * 
	 */
	
}
