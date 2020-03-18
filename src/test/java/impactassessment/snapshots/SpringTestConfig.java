package impactassessment.snapshots;

import impactassessment.rulebase.RuleBaseService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
public class SpringTestConfig {

    @Bean
    public RuleBaseService ruleBaseService() {
        return new RuleBaseService(null);
    }

}
