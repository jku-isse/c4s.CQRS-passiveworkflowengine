package impactassessment;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.jiraartifact.JiraService;
import impactassessment.kiesession.KieSessionFactory;
import impactassessment.kiesession.KieSessionWrapper;
import impactassessment.passiveprocessengine.definition.DronologyWorkflow;
import impactassessment.passiveprocessengine.workflowmodel.AbstractWorkflowDefinition;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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
        return new DronologyWorkflow();
    }

}
