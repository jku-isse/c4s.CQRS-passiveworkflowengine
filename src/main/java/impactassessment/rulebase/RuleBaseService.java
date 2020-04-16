package impactassessment.rulebase;

import impactassessment.mock.artifact.Artifact;
import impactassessment.model.workflowmodel.IdentifiableObject;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;

@Service
@Scope("prototype")
public class RuleBaseService {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private KieSession kieSession;

    public RuleBaseService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", this.commandGateway);
        kieSession.setGlobal("log", log);
    }

    public void insertAndFire(IdentifiableObject o) {
        kieSession.insert(o);
        kieSession.fireAllRules();
    }

    public void insert(IdentifiableObject o) {
        kieSession.insert(o);
    }

    public void insert(Artifact o) {
        kieSession.insert(o);
    }

    public void fire() {
        kieSession.fireAllRules();
    }

}
