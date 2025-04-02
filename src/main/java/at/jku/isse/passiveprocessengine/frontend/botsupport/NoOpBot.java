package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;

@Component
@ConditionalOnExpression(value = "not ${openai.enabled:false} and not ${ollama.enabled:false}")
public class NoOpBot extends AbstractBot {

	public NoOpBot() {
		super(null);
	}

	@Override
	public BotResult send(BotRequest request) {
		return new BotResult(Instant.now(), "NoOpBot", "No OCLBot configured", null, null, null);
	}

	@Override
	public void resetSession() {
		//no op
	}

	@Override
	public String extractAnswerFromResponse(Object response) {		
		return "";
	}

}
