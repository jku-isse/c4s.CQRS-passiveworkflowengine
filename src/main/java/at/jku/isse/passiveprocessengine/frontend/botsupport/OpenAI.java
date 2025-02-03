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

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Issue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import at.jku.isse.designspace.rule.arl.parser.ArlParser;
import at.jku.isse.designspace.rule.arl.parser.ArlType;
import at.jku.isse.ide.assistance.CodeActionExecuter;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
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
public class OpenAI implements OCLBot{

    public static final String API_URL = "https://api.openai.com/v1/chat/completions";

    //public static final String MODEL = "gpt-3.5-turbo";
    public static final String MODEL = "o1-mini";

    private static int maxInteractions = 20;
    
    private final String apiKey;
    protected List<BotInteraction> interaction = new ArrayList<>();
    protected ObjectMapper objectMapper =  JsonMapper.builder()
    			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    			.build();
	
	protected final CodeActionExecuterProvider provider;

    public OpenAI(@Value("${openai.apikey}")String apiKey, 	CodeActionExecuterProvider provider) {
        this.apiKey = apiKey;
        this.provider = provider;
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
            var response = sendRequest(request);
            return compileResult(response.getChoices().stream().map(Choice::getMessage).collect(Collectors.toList()), userInput);
        }catch (Exception e) {
        	log.warn(e.getMessage());
            return new BotResult(Instant.now(), "system", e.getMessage(), null, null);
        }
    }
    
    protected ChatResponse sendRequest(ChatRequest request) throws Exception {
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
            String responseBody = responseBuilder.toString();
            log.info(responseBody);
            ChatResponse response = objectMapper.readValue(responseBody, ChatResponse.class);            
            return response;
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
    		.map(req -> req.getSchema()) 
    		, Stream.of(userInput.getSchema()))
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
    	prompt.append(TASKFOLLOWS_PROMPT);
    	// append the actual user task;
    	prompt.append(userInput.getUserPrompt());
    	// augment current request with prompt to focus on OCL creation
    	prompt.append(OUTPUTFORMAT_PROMPT);
    	
    	// add to input messages
    	messages.add(new Message("user", prompt.toString(), userInput.getTime()));
    	// store request in interactions for later recall and chat interaction recreation
    	interaction.add(userInput);
    	
    	return new ChatRequest(MODEL, messages);
    }
    
    
    
    protected BotResult compileResult(List<Message> responses, BotRequest userInput) {
    	// lets just use the first message
    	Message msg = responses.get(0);
    	
    	String basicOcl = new OCLExtractor(msg.getContent()).extractOCLorNull();
    	String oclError = null;
    	StringBuffer content = new StringBuffer(msg.getContent());
    	if (basicOcl != null && userInput != null && userInput.getContextType() != null && provider != null) {    		
        	var ocl = GeneratedRulePostProcessor.init(basicOcl).getProcessedRule();
        	ocl = wrapInOCLX(ocl, userInput.getContextType().getName());
        	CodeActionExecuter executer = provider.buildExecuter(ocl);    		
    		String lastOCLXVersion = null;
    		List<Issue> issues = null;
    		for (int i = 0; i< 5; i++) { // max 5 rounds of repairs
    			executer.checkForIssues();
        		issues = executer.getProblems();
    			if (!issues.isEmpty()) {
    				content.append("\r\n Found Errors in generated OCL statement: "); 
    				issues.forEach(issue -> content.append("\r\n - "+issue.getMessage()));		
    				executer.executeRepairs();
    				var repair = executer.getExecutedCodeAction();
    				if (repair != null) {    					
    					lastOCLXVersion = (executer.getRepairedConstraint());
    					basicOcl = NodeModelUtils.findActualNodeFor(executer.getModel().getConstraints().get(0).getExpression()).getText();
    					executer = provider.buildExecuter(ocl);  //reset executer for new round
    					//repair.getEdit().getChanges().values().iterator().next().stream().forEach(edit -> System.out.println("Repair: "+edit.getNewText()));    				
    				} else {
    					break; // we could repair the top most problem, hence aborting
    				}    			
    			} else 
    				break;
    		}
    		if (lastOCLXVersion != null) {
    			content.append("\r\nApplied automatic repair(s) resulting in:" );
    			content.append("\r\n"+basicOcl);
    		}
    		if (!issues.isEmpty()) {
    			// maintain topmost remaining error message to feed back into LLM iteration
    			oclError = issues.get(0).getMessage();
    			content.append("\r\nRemaining error: "+issues.get(0).getMessage());
    		}
    		//content.append("\r\n" +checkARL(basicOcl, userInput.getContextType()));
    	}
    	BotResult res = new BotResult(msg.getTime(), "OCLbot", content.toString(), basicOcl, oclError);
    	interaction.add(res);
    	return res;
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
    
	private String wrapInOCLX(String constraint, String context) {
		return "rule TestRule {\r\n"
				+ "					description: \"ignored\"\r\n"
				+ "					context: "+context+"\r\n"
				+ "					expression: "+constraint+" \r\n"
				+ "				}";	
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
