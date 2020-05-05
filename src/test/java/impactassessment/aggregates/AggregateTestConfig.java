package impactassessment.aggregates;

import impactassessment.model.definition.WPManagementWorkflow;
import impactassessment.query.snapshot.CLTool;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;

@EnableAutoConfiguration
public class AggregateTestConfig {

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

    @Bean
    public WPManagementWorkflow wpManagementWorkflow(){
        WPManagementWorkflow workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        return workflow;
    }
}
