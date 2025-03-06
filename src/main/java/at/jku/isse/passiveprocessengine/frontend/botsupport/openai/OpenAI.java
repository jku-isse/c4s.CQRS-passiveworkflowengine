/*
 * Copyright 2023 Sami Ekblad, Vaadin Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * heavily modified by christoph.mayr-dorn@jku.at
 */
package at.jku.isse.passiveprocessengine.frontend.botsupport.openai;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.jku.isse.passiveprocessengine.frontend.botsupport.AbstractBot;
import at.jku.isse.passiveprocessengine.frontend.botsupport.ollama.OllamaAI.ChatResponse;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Java class to interact with Open AI API.
 *
 */
@Slf4j
@Component
@Scope("session")
@ConditionalOnExpression(value = "${openai.enabled:false}")
public class OpenAI extends AbstractBot{

    public static final String API_URL = "https://api.openai.com/v1/chat/completions";

    //public static final String MODEL = "gpt-3.5-turbo";
    public static final String MODEL = "o1-mini";

    private final String apiKey;
    public OpenAI(@Value("${openai.apikey}")String apiKey, 	CodeActionExecuterProvider provider) {
        super(provider);
		this.apiKey = apiKey;
    }  
    
    public BotResult send(BotRequest userInput) {
        try {
            ChatRequest request  = compileRequest(userInput);
            var response = sendRequest(request);
            var promptMsg = request.getMessages().get(request.getMessages().size()-1);
            var msg = response.getChoices().stream()
            		.map(Choice::getMessage).toList().get(0);
            return compileResult(msg.getContent(), userInput, promptMsg.getContent(), msg.getTime());
        }catch (Exception e) {
        	log.warn(e.getMessage());
            return new BotResult(Instant.now(), "system", e.getMessage(), null, null);
        }
    }
    
	@Override
	public String extractAnswerFromResponse(Object response) {
		return ((ChatResponse)response).getChoices().stream()
        		.map(Choice::getMessage).toList().get(0).getContent();
	}
    
    public ChatResponse sendRequest(ChatRequest request) throws Exception {
    	log.info(request.toString());
       String requestBody = objectMapper.writeValueAsString(request);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
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
            APIError error = objectMapper.readValue(errorBody, APIError.class);
            log.info(error.toString());
            throw new Exception(error.getError().message);
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
    	messages.add(new Message("user", prompt, userInput.getTime()));
    	// store request in interactions for later recall and chat interaction recreation
    	interaction.add(userInput);
    	
    	return new ChatRequest(MODEL, messages);
    } 
    
    protected List<BotInteraction> getInteractions() {
    	return interaction;
    }
      
    @Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }

        public String toJsonString() throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private Usage usage;
        private List<Choice> choices;     
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;        
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        @JsonIgnore
        private Instant time;
        private String role;
        private String content;

        public Message() {
            this.time = Instant.now();
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.time = Instant.now();
        }

        public Message(String role, String content, Instant time) {
            this.role = role;
            this.content = content;
            this.time = time;
        }       
        
        protected static Message fromHistory(BotInteraction interaction) {
        	if (interaction instanceof BotRequest req) {
        		return new Message("user", req.getUserPrompt(), req.getTime());
        	} else if (interaction instanceof BotResult res) {
        		return new Message("user", res.getBotResult(), res.getTime());
        	} else 
        		throw new RuntimeException("Unexpected BotInteraction subclass: "+interaction.getClass().toString());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class APIError {
        private ErrorDetail error;        
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {
        private String message;
        private String type;
        private String param;
        private String code;       
    }

}
