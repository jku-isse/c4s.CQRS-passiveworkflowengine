package impactassessment;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.jiraartifact.JiraJsonService;
import impactassessment.jiraartifact.JiraService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public IJiraArtifactService getJiraArtifactService() {
//        return new JiraService();
        return new JiraJsonService();
    }

}
