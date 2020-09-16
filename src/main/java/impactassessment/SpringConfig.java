package impactassessment;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.passiveprocessengine.definition.AbstractWorkflowDefinition;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflow;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflowFixed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public AbstractWorkflowDefinition getAbstractWorkflowDefinition() {
        // careful! execution.drl has hardcoded (dronology)workflow-branch names
        // --> just injecting a new workflow here won't work
        return new DronologyWorkflowFixed();
    }

}
