package impactassessment;

import c4s.jiralightconnector.ChangeSubscriber;
import c4s.jiralightconnector.InMemoryMonitoringState;
import c4s.jiralightconnector.IssueCache;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.jiraartifact.JiraService;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.exampleworkflows.NestedWorkflow;

@Configuration
public class SpringConfig {

    @Bean
    public IJiraArtifactService getJiraArtifactService() {
        // uses JSON image of Jira data in resources folder
        return new JiraJsonService();
    }

//    @Bean
//    public IJiraArtifactService getJiraArtifactService(IssueCache issueCache, ChangeSubscriber changeSubscriber) {
//        // connects directly to a Jira server
//        return new JiraService(issueCache, changeSubscriber, new InMemoryMonitoringState());
//    }

    @Bean
    public WorkflowDefinition getAbstractWorkflowDefinition() {
        // careful! execution.drl has hardcoded (dronology)workflow-branch names
        // --> just injecting a new workflow here won't work
        return new NestedWorkflow();
//        return new DronologyWorkflowFixed();
    }

    @Bean
    @Autowired
    public IRegisterService getIRegisterService(WorkflowDefinitionRegistry registry) {
        return new LocalRegisterService(registry);
    }

}
