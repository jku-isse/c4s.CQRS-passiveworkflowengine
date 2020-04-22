package impactassessment.rulebase;

import impactassessment.mock.artifact.Artifact;
import impactassessment.model.workflowmodel.IdentifiableObject;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
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

    public FactHandle insert(IdentifiableObject o) {
        return kieSession.insert(o);
    }

    public FactHandle insert(Artifact o) {
        return kieSession.insert(o);
    }

    public void update(FactHandle handle, IdentifiableObject o) {
        kieSession.update(handle, o);
    }

    public void update(FactHandle handle, Artifact a) {
        kieSession.update(handle, a);
    }

    public void delete(FactHandle handle) {
        kieSession.delete(handle);
    }

    public void fire() {
        kieSession.fireAllRules();
    }

}
