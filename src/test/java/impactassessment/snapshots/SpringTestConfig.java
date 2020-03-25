package impactassessment.snapshots;

import impactassessment.query.snapshot.CLTool;
import impactassessment.rulebase.RuleBaseService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;

@EnableAutoConfiguration
public class SpringTestConfig {

    @Bean
    @Autowired
    public RuleBaseService ruleBaseService(CommandGateway commandGateway) {
        return new RuleBaseService(commandGateway);
    }

    @Bean
    public CLTool cli() {
        return new CLTool(){
            @Override
            public CompletableFuture<Action> readAction() {
                CompletableFuture<Action> completableFuture = new CompletableFuture<>();
                completableFuture.complete(Action.STOP);
                return completableFuture;
            }
        };
    }
}
