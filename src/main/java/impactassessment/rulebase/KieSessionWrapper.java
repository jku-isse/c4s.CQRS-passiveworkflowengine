package impactassessment.rulebase;

import impactassessment.artifact.base.IArtifact;
import impactassessment.artifact.base.IArtifactService;
import impactassessment.model.workflowmodel.IdentifiableObject;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.HashMap;
import java.util.Map;

public class KieSessionWrapper {

    private @Getter
    KieSession kieSession;
    private Map<String, FactHandle> sessionHandles;
    private @Getter @Setter
    boolean isInitialized;

    public KieSessionWrapper(CommandGateway commandGateway, IArtifactService artifactService) {
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", commandGateway);
        kieSession.setGlobal("artifactService", artifactService);
        sessionHandles = new HashMap<>();
        isInitialized = false;
    }

    public void insertOrUpdate(Object o) {
        if (o instanceof IArtifact) {
            IArtifact a = (IArtifact) o;
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
