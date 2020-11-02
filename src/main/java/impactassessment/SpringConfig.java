package impactassessment;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.registry.IRegisterService;
import impactassessment.registry.LocalRegisterService;
import impactassessment.registry.WorkflowDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.exampleworkflows.NestedWorkflow;

@Configuration
public class SpringConfig {

    @Bean
    public IJiraArtifactService getJiraArtifactService() {
        // connects directly to a Jira server
//        return new JiraService();
        // uses JSON image of Jira data in resources folder
        return new JiraJsonService();
    }

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
