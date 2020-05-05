package impactassessment.rulebase;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class RuleBaseService {

    private final CommandGateway commandGateway;
    private KieSession kieSession;

    public RuleBaseService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", this.commandGateway);
    }

    public void insert(Object o) {
        kieSession.insert(o);
    }

    public void fire() {
        kieSession.fireAllRules();
    }

    public void dispose() {
        kieSession.dispose();
    }

}
