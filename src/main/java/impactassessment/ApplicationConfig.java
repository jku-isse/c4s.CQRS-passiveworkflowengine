package impactassessment;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.jiraartifact.JiraService;
import impactassessment.kiesession.KieSessionFactory;
import impactassessment.kiesession.KieSessionWrapper;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ApplicationConfig {

    @Bean
    public IJiraArtifactService getJiraArtifactService() {
        // connects directly to a Jira server. URI, username and password are defined in application.properties
//        return new JiraService();
        // uses JSON image of Jira data with the stated filename in resources folder
        return new JiraJsonService("dronology_jira.json");
    }

    @Bean
    @Scope("prototype")
    public KieSessionWrapper getKieSessionWrapper(CommandGateway commandGateway, IJiraArtifactService artifactService) {
        // define which rule files (have to be located in "/resources/rules/") should be taken to build up kieSessions
        KieSession kieSession = new KieSessionFactory().getKieSession("execution_qac.drl", "constraints.drl");
        return new KieSessionWrapper(commandGateway, artifactService, kieSession);
    }

}
