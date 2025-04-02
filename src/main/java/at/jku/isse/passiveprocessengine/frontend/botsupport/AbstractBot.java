package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotInteraction;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotResult;
import at.jku.isse.passiveprocessengine.frontend.oclx.CodeActionExecuterProvider;
import at.jku.isse.passiveprocessengine.frontend.oclx.IterativeRepairer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBot implements OCLBot{

	protected static int maxInteractions = 20;
	protected List<BotInteraction> interaction = new ArrayList<>();
	protected ObjectMapper objectMapper = JsonMapper.builder()
	    			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	    			.build();
	protected final CodeActionExecuterProvider provider;

	public AbstractBot(CodeActionExecuterProvider provider) {
		super();
		this.provider = provider;
	}
	
	protected String compilePrompt(BotRequest userInput) {
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
	    var contextType = userInput.getContextType() != null ?
	    		userInput.getContextType().getName() : null;	    	    
	    return compilePrompt(finalSchema, contextType, userInput.getUserPrompt(), userInput.getExistingRule());	
	}
		
	public String compilePrompt(String finalSchema, String contextType, String userPrompt, String existingRule) {
		StringBuffer prompt = new StringBuffer();
    	// then with current request and additional prompt instructions
    	prompt.append(TASK_PROMPT);
    	// check need to add schema (might have been updated)    	
    	if (finalSchema != null) {
    		prompt.append(String.format(SCHEMA_PROMPT_TEMPLATE, finalSchema));
    	}
    	// set which object to use as context
    	if (contextType != null) {
    		prompt.append(String.format(OCL_CONTEXT_PROMPT_TEMPLATE, contextType));
    	}
    	// if example ocl available, remove prior ones (we only provide the latest example)
    	if (existingRule != null) {
    		prompt.append(String.format(OCL_STARTINGPOINT_PROMPT_TEMPLATE, existingRule));
    	}
    	prompt.append(TASKFOLLOWS_PROMPT);
    	// append the actual user task;
    	prompt.append(userPrompt);
    	// augment current request with prompt to focus on OCL creation
    	prompt.append(OUTPUTFORMAT_PROMPT);
		return prompt.toString();
	}

    public void resetSession() {
    	this.interaction.clear();
    }
    
    public BotResult compileResult(String response, BotRequest userInput, String fullLastPrompt, Instant timestamp) {
    	// lets just use the first message
    	
    	log.info(response);
    	String basicOcl = null; //new OCLExtractor(msg.getContent()).extractOCLorNull();
    	String oclError = null;
    	StringBuffer augmentedResult = new StringBuffer();
    	
    	if ( /*basicOcl != null && */ userInput != null && userInput.getContextType() != null && provider != null) {    		
    		try {
    			var repairer = new IterativeRepairer(provider);
    			var result = repairer.checkResponse(userInput.getContextType().getName(), fullLastPrompt, response, 1);
    			
    			if (result.getOclString() == null) { 
    				oclError = "Could not extract OCL string";
    			} else if (result.getOclxString() == null) {
    				basicOcl = result.getOclString();
    				oclError = "Incorrect OCL syntax";
    				augmentedResult.append("\n Found Errors in generated OCL statement: "); 
    				result.getErrors().forEach(issue -> augmentedResult.append("\n - "+issue));	
    			} else if (result.getRemainingError() != null) {
    				if (!isEqualsIgnoreingWhitespace(result.getOclString(),result.getFixedOclString())) {    					
    					augmentedResult.append("\n Auto corrected OCL: \n"+result.getFixedOclString());
    				}    				
    				basicOcl = result.getFixedOclString();
    				oclError = result.getRemainingError();
    				augmentedResult.append("\n Remaining Error: "+result.getRemainingError());
    			} else { //all fine
    				basicOcl = result.getFixedOclString();
    				if (!isEqualsIgnoreingWhitespace(result.getOclString(),result.getFixedOclString())) {       					
    					augmentedResult.append("\n Auto corrected OCL: \n"+result.getFixedOclString());
    				}    				
    			}
    			
    		} catch(Exception e) {
    			log.warn("Error processing LLM response: "+e.getMessage());
    		}
    	}
    	BotResult res = new BotResult(timestamp, "OCLbot", response, augmentedResult.toString(), basicOcl, oclError);
    	interaction.add(res);
    	return res;
    }
    
    private boolean isEqualsIgnoreingWhitespace(String a, String b) {
    	return StringUtils.deleteWhitespace(a).equals(StringUtils.deleteWhitespace(b));
    }
    
//  private  ArlParser parser = new ArlParser(); 
//  
//  protected String checkARL(String rule, PPEInstanceType instanceType) {
//  	 try {
//           parser.parse(rule, ArlType.get(ArlType.TypeKind.INSTANCE, ArlType.CollectionKind.SINGLE, instanceType), null);
//           return "Rule constains no syntax errors";
//       }
//       catch (Exception ex) {             
//           return String.format("Warning: Rule caused parsing error: %s (Line=%d, Column=%d)", ex.getMessage(), parser.getLine(), parser.getColumn());
//       }
//  }
	
}