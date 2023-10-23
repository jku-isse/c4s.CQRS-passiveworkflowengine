package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import at.jku.isse.designspace.core.model.InstanceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;


public interface OCLBot {

    public static final String TASK_PROMPT = "You are tasked with providing an OCL rule based on the detailed description provided further below. \r\n";
    
    public static final String SCHEMA_PROMPT_TEMPLATE = "Only use properties in the OCL rule from the following schema.\r\n  %s \r\n  Make sure you use only these properties in your answer. \r\n";

    public static final String OCL_CONTEXT_PROMPT_TEMPLATE = "The context of the OCL rule is an object of type %s \r\n";
    
    public static final String OCL_STARTINGPOINT_PROMPT_TEMPLATE = "Make sure you use the following OCL rule as a starting point for refinement: \r\n ''' %s \r\n ''' \r\n";
    
    public static final String OUTPUTFORMAT_PROMPT = "Remember, only provide the OCL rule, but no explanation unless explicitly asked for it \r\n"
    		+ "The exact task is the following: \r\n";
    		    
    public static final String FORGET_SCHEMA = "FORGET_SCHEMA"; // used to indicate to not use any schema information, not necessary when providing new schema as this overrides old schema
	
    
    
	default CompletableFuture<BotResult> sendAsync(BotRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public BotResult send(BotRequest request);
	
    public void resetSession();
    
    @Data
    public static abstract class BotInteraction {
    }
    
    @EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
    @Data
    @AllArgsConstructor
	public static class BotRequest extends BotInteraction{
    	final Instant time;
    	final String role;
    	@NonNull final String userPrompt;
    	final InstanceType contextType;
		String schema;
		final String existingRule;
	}
	
    @EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
    @Data
	public static class BotResult extends BotInteraction{
    	final Instant time;
    	final String role;
    	final String botResult;
		final String oclRule;
	}
}
