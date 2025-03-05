package at.jku.isse.passiveprocessengine.frontend.botsupport.ollama;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.ChatResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OllamaUtils {

	final OllamaAI bot;
	
	public ChatResponse unloadModel(String model) throws IOException {
		return bot.sendRequest(new UnloadModel(model));
	}
	
	public ChatResponse loadModel(String model) throws IOException {
		return bot.sendRequest(new LoadModel(model));
	}
	
	@Data
	public static class UnloadModel {
		final String model;
		List messages = Collections.emptyList();
		int keep_alive = 0;
	}
	
	@Data
	public static class LoadModel {
		final String model;
		List messages = Collections.emptyList();		
	}
}
