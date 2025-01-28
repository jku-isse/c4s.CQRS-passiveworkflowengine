package at.jku.isse.passiveprocessengine.frontend.botsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI.ChatRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OpenAI.Message;

class TestInteractionHistory {

	private static final PPEInstanceType CONTEXTTYPE = null;
	private OpenAI openAI;
	
	@BeforeEach
	void setUp() throws Exception {
		openAI = new OpenAI("NOTRELEVANT", null);
	}
	
	
	@Test
	void testFromEmptyHistory() {
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, null, null) );
		assert(req.getMessages().size() == 1);
		Message msg = req.getMessages().get(0); 
		assertFalse(msg.getContent().contains("Only use properties in the OCL rule"));
	}

	
	@Test
	void testReuseSchema() {
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, "TESTSCHEMA1", null) );
		openAI.compileResult(List.of(new Message("system", "Resp2")), null);
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, null, null) );
		assert(openAI.interaction.size() == 3);
		assert(req.getMessages().size() == 3);
		Message msg = req.getMessages().get(2); 
		assert(msg.getContent().contains("TESTSCHEMA1"));
	}
	
	@Test
	void testOverrideSchema() {
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, "TESTSCHEMA1", null) );
		openAI.compileResult(List.of(new Message("system", "Resp2")), null);
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, "TESTSCHEMA2", null) );
		Message msg = req.getMessages().get(2); 
		assert(msg.getContent().contains("TESTSCHEMA2"));
		assert(!msg.getContent().contains("TESTSCHEMA1"));
	}
	
	@Test
	void testInvalidateSchema() {
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, "TESTSCHEMA1", null) );
		openAI.compileResult(List.of(new Message("system", "Resp2")), null);
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, OCLBot.FORGET_SCHEMA, null) );
		Message msg = req.getMessages().get(2); 
		assert(!msg.getContent().contains("Only use properties in the OCL rule"));
		assert(!msg.getContent().contains("TESTSCHEMA1"));
	}
	
	@Test
	void testInvalidateThenOverrideSchema() {
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, "TESTSCHEMA1", null) );
		openAI.compileResult(List.of(new Message("system", "Resp2")), null);
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, OCLBot.FORGET_SCHEMA, null) );
		openAI.compileResult(List.of(new Message("system", "Resp4")), null);
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, "TESTSCHEMA2", null) );
		Message msg = req.getMessages().get(4); 
		assert(msg.getContent().contains("TESTSCHEMA2"));
		assert(!msg.getContent().contains("TESTSCHEMA1"));
	}
	
	@Test
	void testSessionReset() {
		openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS", CONTEXTTYPE, "TESTSCHEMA1", null) );
		openAI.compileResult(List.of(new Message("system", "Resp2")), null);
		openAI.resetSession();
		ChatRequest req = openAI.compileRequest(new BotRequest(Instant.now(), "user", "FIXTHIS2", CONTEXTTYPE, null, null) );
		assert(openAI.interaction.size() == 1);
		assert(req.getMessages().size() == 1);
		Message msg = req.getMessages().get(0); 		
		assert(!msg.getContent().contains("TESTSCHEMA1"));
	}

}
