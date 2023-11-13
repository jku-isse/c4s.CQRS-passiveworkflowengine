package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "not ${openai.enabled:false}")
public class NoOpBot implements OCLBot {

	@Override
	public BotResult send(BotRequest request) {
		return new BotResult(Instant.now(), "NoOpBot", "No OCLBot configured", null);
	}

	@Override
	public void resetSession() {
		//no op
	}

}
