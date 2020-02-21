package pingpong.rulebase;

import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pingpong.api.DecreaseCmd;
import pingpong.api.IncreaseCmd;

@Component
@Profile("pub")
@RequiredArgsConstructor
public class CommandPublisher {

    private final CommandGateway commandGateway;

    public void publish() {
        commandGateway.sendAndWait(new IncreaseCmd("1", 1));
    }
}
