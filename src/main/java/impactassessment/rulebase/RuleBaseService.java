package impactassessment.rulebase;

import impactassessment.mock.artifact.Artifact;
import impactassessment.model.workflowmodel.IdentifiableObject;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RuleBaseService {

    private final CommandGateway commandGateway;
    private KieSession kieSession;
    private Map<String, FactHandle> sessionHandles;

    public RuleBaseService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", this.commandGateway);
        sessionHandles = new HashMap<>();
    }

    public void insertOrUpdate(Object o) {
        if (o instanceof Artifact) {
            Artifact a = (Artifact) o;
            String key = a.getId() + "[" + a.getClass().getSimpleName() + "]";
            insertOrUpdate(key, a);
        } else if (o instanceof IdentifiableObject) {
            IdentifiableObject idO = (IdentifiableObject) o;
            String key = idO.getId() + "[" + idO.getClass().getSimpleName() + "]";
            insertOrUpdate(key, idO);
        } else {
            // unmanaged objects
            kieSession.insert(o);
        }
    }

    private void insertOrUpdate(String key, Object o) {
        if (sessionHandles.containsKey(key)) {
            kieSession.update(sessionHandles.get(key), o);
        } else {
            FactHandle handle = kieSession.insert(o);
            sessionHandles.put(key, handle);
        }
    }

    public void fire() {
        kieSession.fireAllRules();
    }

    public void dispose() {
        kieSession.dispose();
    }

}
