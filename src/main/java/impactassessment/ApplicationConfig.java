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
//        return new JiraService();
        return new JiraJsonService();
    }

    @Bean
    @Scope("prototype")
    public KieSessionWrapper getKieSessionWrapper(CommandGateway commandGateway, IJiraArtifactService artifactService) {
        KieSession kieSession = new KieSessionFactory().getKieSession("execution_qac.drl", "constraints.drl");
        return new KieSessionWrapper(commandGateway, artifactService, kieSession);
    }

}
