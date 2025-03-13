package at.jku.isse.passiveprocessengine.frontend.botsupport.ollama;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import at.jku.isse.passiveprocessengine.frontend.botsupport.AbstractBot;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("session")
@ConditionalOnExpression(value = "${ollama.enabled:false}")
public class OllamaAI extends AbstractBot {

	private final String url;
	private final String model;
	
	public OllamaAI(@Value("${ollama.url}") String url, @Value("${ollama.model}")String model, CodeActionExecuterProvider provider) {
		super(provider);
		this.url = url;
		this.model = model;
	}
	
	@Override
	public BotResult send(BotRequest userInput) {
		try {
			var request = compileRequest(userInput);
			var response = sendRequest(request);
			var promptMsg = request.getMessages().get(request.getMessages().size()-1);
			var msg = extractAnswerFromResponse(response);
			return compileResult(msg, userInput, promptMsg.getContent(), Instant.now());			
		}catch (Exception e) {
        	log.warn(e.getMessage());
            return new BotResult(Instant.now(), "system", e.getMessage(), null, null);
        }
	}	
	
	@Override
	public String extractAnswerFromResponse(Object response) {
		return ((ChatResponse)response).getMessage().getContent();
	}
	
	
    protected ChatRequest compileRequest(BotRequest userInput) {
    	// keep history small
    	while (interaction.size() > maxInteractions) {
    		interaction.remove(0);
    	}
    	List<Message> messages = new ArrayList<>();
    	// append prior requests,
    	interaction.stream()
    		.map(part -> Message.fromHistory(part))
    		.forEach(msg -> messages.add(msg));
    	String prompt = compilePrompt(userInput);    	
    	// add to input messages
    	messages.add(new Message("user", prompt));
    	// store request in interactions for later recall and chat interaction recreation
    	interaction.add(userInput);    	
    	return new ChatRequest(model, messages);
    } 
	
	public ChatResponse sendRequest(Object request) throws IOException {
		String requestBody = objectMapper.writeValueAsString(request);

        URL endpointurl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) endpointurl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");        
        conn.setDoOutput(true);

        conn.getOutputStream().write(requestBody.getBytes());
        if (conn.getResponseCode() >= 400) {
            Scanner scanner = new Scanner(conn.getErrorStream());
            StringBuilder responseBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                responseBuilder.append(scanner.nextLine());
            }
            scanner.close();
            String errorBody = responseBuilder.toString();
            log.warn(errorBody);                        
            throw new IOException(errorBody);
        } else {
            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder responseBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                responseBuilder.append(scanner.nextLine());
            }
            scanner.close();
            String responseBody = responseBuilder.toString();
            log.info(responseBody);
            ChatResponse response = objectMapper.readValue(responseBody, ChatResponse.class);            
            return response;
        }
	}
	
	
	
	
	@Data
    @JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChatRequest {
		private final String model;
		private final List<Message> messages;
		private boolean stream = false;
		private Map<String, Object> options = getReplicabilityOptions();
		
		private Map<String, Object> getReplicabilityOptions() {
			Map<String, Object> map = new HashMap<>();
			map.put("seed", 101);
			map.put("temperature", 0);
			return map;
		}
		
	}
		
	@Data
    @JsonIgnoreProperties(ignoreUnknown = true)
	public static class Message {
		private String role;
		private  String content;
		
		public Message() {}
		
		public Message(String role, String content) {
			super();
			this.role = role;
			this.content = content;
		}
		
		protected static Message fromHistory(BotInteraction interaction) {
        	if (interaction instanceof BotRequest req) {
        		return new Message("user", req.getUserPrompt());
        	} else if (interaction instanceof BotResult res) {
        		return new Message("user", res.getBotResult());
        	} else 
        		throw new RuntimeException("Unexpected BotInteraction subclass: "+interaction.getClass().toString());
        }
	}
	
	@Data
    @JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChatResponse {
		private String model;
		//private Instant created_at;
		private Message message;
		private boolean done;
		private long total_duration;
		private long load_duration;
		
		public ChatResponse() {}
	}


}
