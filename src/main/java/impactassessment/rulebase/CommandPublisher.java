package impactassessment.rulebase;

import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import impactassessment.api.DecreaseCmd;

@Component
@Profile("pub")
@RequiredArgsConstructor
public class CommandPublisher {

    private final CommandGateway commandGateway;

    public void publish() {
        commandGateway.send(new DecreaseCmd("1", 1));
    }
}
