package impactassessment.aggregates;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
public class AggregateTestConfig {

//    @Bean
//    public CLIActionProvider cli() {
//        return new CLIActionProvider(){
//            @Override
//            public CompletableFuture<Action> readAction() {
//                CompletableFuture<Action> completableFuture = new CompletableFuture<>();
//                completableFuture.complete(Action.STORE);
//                return completableFuture;
//            }
//        };
//    }
//
//    @Bean
//    public WPManagementWorkflow wpManagementWorkflow(){
//        WPManagementWorkflow workflow = new WPManagementWorkflow();
//        workflow.initWorkflowSpecification();
//        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
//        return workflow;
//    }
}
