package at.jku.isse.passiveprocessengine.frontend.botsupport.openai;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import at.jku.isse.passiveprocessengine.frontend.botsupport.TypeSelectionResponse;
import at.jku.isse.passiveprocessengine.frontend.botsupport.openai.OpenAI;
import at.jku.isse.passiveprocessengine.frontend.botsupport.openai.OpenAI.Choice;
import at.jku.isse.passiveprocessengine.frontend.botsupport.openai.OpenAI.Message;
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
	
	String promptTemplate1 = "Your task is to select the relevant object types for answering the following question: '%s' "
			+ " Select from the following list of types and print them as a json list. %s Only return the json list without any explanation.";
	
	@BeforeAll
	static void setup() {
		props = new UIConfig("Test");		
		try {
			InputStream input = new FileInputStream("application.properties") ;
            props.load(input);
		} catch(IOException e) {
			String msg = "No ./application.properties found";
			log.error(msg);
			throw new RuntimeException(msg);
		}
	}
	
	@Test @Disabled
	void testSelectRelevantTypes() throws Exception {
		assertTrue(props.containsKey("openai.apikey"));		
		List<Message> messages = new ArrayList<>();
		var constraint = " from a issue, I like to check whether all succeeding requirements are in status released";
		var prompt = String.format(promptTemplate1, constraint ,types1);
		messages.add(new Message("user", prompt.toString(), Instant.now()));
		var req = new OpenAI.ChatRequest(OpenAI.MODEL, messages);
		OpenAI bot = new OpenAI(props.getProperty("openai.apikey"), null);
		var response = bot.sendRequest(req);
		var responses = response.getChoices().stream().map(Choice::getMessage).collect(Collectors.toList());
		String answer = responses.get(0).getContent();
		System.out.println(answer);
	}

	@Test @Disabled
	void testSelectRelevantType() throws Exception {
		assertTrue(props.containsKey("openai.apikey"));		
		List<Message> messages = new ArrayList<>();
		var constraint = " for a Review, I like to check whether it is in status released";
		var prompt = String.format(promptTemplate1, constraint ,types1);
		messages.add(new Message("user", prompt.toString(), Instant.now()));
		var req = new OpenAI.ChatRequest(OpenAI.MODEL, messages);
		OpenAI bot = new OpenAI(props.getProperty("openai.apikey"), null);
		var response = bot.sendRequest(req);
		var responses = response.getChoices().stream().map(Choice::getMessage).collect(Collectors.toList());
		String answer = responses.get(0).getContent();
		System.out.println(answer);
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
