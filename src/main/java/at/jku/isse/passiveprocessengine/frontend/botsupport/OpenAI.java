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
 */
package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.jku.isse.designspace.rule.arl.parser.ArlParser;
import at.jku.isse.designspace.rule.arl.parser.ArlType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Java class to interact with Open AI API.
 *
 */
@Slf4j
@Component
@Scope("session")
@ConditionalOnExpression(value = "${openai.enabled:false}")
public class OpenAI implements OCLBot{

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String MODEL = "gpt-3.5-turbo";

    private static int maxInteractions = 20;
    
    private final String apiKey;
    protected List<BotInteraction> interaction = new ArrayList<>();

    public OpenAI(@Value("${openai.apikey}")String apiKey) {
        this.apiKey = apiKey;
    }

    public CompletableFuture<BotResult> sendAsync(BotRequest userInput) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(userInput);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public BotResult send(BotRequest userInput) {
        try {
            ChatRequest request  = compileRequest(userInput);
            log.info(request.toString());
            ObjectMapper objectMapper = new ObjectMapper();
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
                String errorBody = responseBuilder.toString();
                APIError error = objectMapper.readValue(errorBody, APIError.class);
                log.info(error.toString());
                return new BotResult(Instant.now(), "system", error.getError().message, null);
            } else {
                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder responseBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    responseBuilder.append(scanner.nextLine());
                }
                String responseBody = responseBuilder.toString();
                ChatResponse response = objectMapper.readValue(responseBody, ChatResponse.class);
                log.info(response.toString());
                return compileResult(response.getChoices().stream().map(Choice::getMessage).collect(Collectors.toList()), userInput);
            }
        }catch (Exception e) {
        	log.warn(e.getMessage());
            return new BotResult(Instant.now(), "system", e.getMessage(), null);
        }
    }
    
    public void resetSession() {
    	this.interaction.clear();
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
    	StringBuffer prompt = new StringBuffer();
    	// then with current request and additional prompt instructions
    	prompt.append(TASK_PROMPT);
    	// check need to add schema (might have been updated)
    	String finalSchema = Stream.concat(interaction.stream()
    		.filter(BotRequest.class::isInstance)
    		.map(BotRequest.class::cast)
    		.map(req -> req.getSchema()) , Stream.of(userInput.getSchema()))
    		.reduce(null, (prioSchema, currentSchema) -> { 
    			if (currentSchema == null || currentSchema.length() < 10)
    				return prioSchema;
    			else if (currentSchema.equals(FORGET_SCHEMA)) 
    				return null;
    			else 
    				return currentSchema;
    		});
    	userInput.setSchema(finalSchema); // to preserve as otherwise we might forget if setting was done more than 10 requests ago.
    	if (finalSchema != null) {
    		prompt.append(String.format(SCHEMA_PROMPT_TEMPLATE, finalSchema));
    	}
    	// set which object to use as context
    	if (userInput.getContextType() != null) {
    		prompt.append(String.format(OCL_CONTEXT_PROMPT_TEMPLATE, userInput.getContextType().getName()));
    	}
    	// if example ocl available, remove prior ones (we only provide the latest example)
    	if (userInput.getExistingRule() != null) {
    		prompt.append(String.format(OCL_STARTINGPOINT_PROMPT_TEMPLATE, userInput.getExistingRule()));
    	}
    	// augment current request with prompt to focus on OCL creation
    	prompt.append(OUTPUTFORMAT_PROMPT);
    	// finally append the actual user task;
    	prompt.append(userInput.getUserPrompt());
    	// add to input messages
    	messages.add(new Message("user", prompt.toString(), userInput.getTime()));
    	// store request in interactions for later recall and chat interaction recreation
    	interaction.add(userInput);
    	
    	return new ChatRequest(MODEL, messages);
    }
    
    
    
    protected BotResult compileResult(List<Message> responses, BotRequest userInput) {
    	// lets just use the first message
    	Message msg = responses.get(0);
    	
    	String ocl = extractOCLorNull(msg.getContent());
    	String content = msg.getContent();
    	if (ocl != null && userInput != null && userInput.getContextType() != null) {    		
    		content = content + "\r\n" +checkARL(ocl, userInput.getContextType());
    	}
    	BotResult res = new BotResult(msg.getTime(), "OCLbot", content, ocl);
    	interaction.add(res);
    	return res;
    }
    
    protected String extractOCLorNull(String message) {
    	int pos = message.lastIndexOf("inv:");
    	if (pos >= 0) {
    		String ocl = message.substring(pos+4).trim();
    		return ocl;
    	} else
    		return null;
    	
    }
    
    private  ArlParser parser = new ArlParser(); 
    
    protected String checkARL(String rule, PPEInstanceType instanceType) {
    	 try {
             parser.parse(rule, ArlType.get(ArlType.TypeKind.INSTANCE, ArlType.CollectionKind.SINGLE, instanceType), null);
             return "Rule constains no syntax errors";
         }
         catch (Exception ex) {             
             return String.format("Warning: Rule caused parsing error: %s (Line=%d, Column=%d)", ex.getMessage(), parser.getLine(), parser.getColumn());
         }
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
    public static class ChatResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private Usage usage;
        private List<Choice> choices;     
    }

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;        
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }

    @Data
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
        	if (interaction instanceof BotRequest) {
        		BotRequest req = (BotRequest)interaction;
        		return new Message("user", req.getUserPrompt(), req.getTime());
        	} else if (interaction instanceof BotResult) {
        		BotResult res = (BotResult)interaction;
        		return new Message("system", res.getBotResult(), res.getTime());
        	} else 
        		throw new RuntimeException("Unexpected BotInteraction subclass: "+interaction.getClass().toString());
        }
    }

    @Data
    public static class APIError {
        private ErrorDetail error;        
    }

    @Data
    public static class ErrorDetail {
        private String message;
        private String type;
        private String param;
        private String code;       
    }

}
