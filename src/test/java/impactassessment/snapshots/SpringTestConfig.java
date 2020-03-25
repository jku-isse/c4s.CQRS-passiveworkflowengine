package impactassessment.snapshots;

import impactassessment.query.snapshot.Snapshotter;
import impactassessment.rulebase.RuleBaseService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
public class SpringTestConfig {

    @Bean
    @Autowired
    public RuleBaseService ruleBaseService(CommandGateway commandGateway) {
        return new RuleBaseService(commandGateway);
    }

}
