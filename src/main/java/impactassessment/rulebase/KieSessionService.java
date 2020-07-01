package impactassessment.rulebase;

import impactassessment.artifact.base.IArtifactService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class KieSessionService {

    private final CommandGateway commandGateway;
    private final IArtifactService artifactService;
    private Map<String, KieSessionWrapper> kieSessions;

    public KieSessionService(CommandGateway commandGateway, IArtifactService artifactService) {
        this.commandGateway = commandGateway;
        this.artifactService = artifactService;
        kieSessions = new HashMap<>();
    }

    public void insertOrUpdate(String id, Object o) {
        getOtherwiseCreate(id).insertOrUpdate(o);
    }

    public void fire(String id) {
        kieSessions.get(id).fire();
    }

    public void dispose(String id) {
        kieSessions.get(id).dispose();
        kieSessions.remove(id);
    }

    public boolean isInitialized(String id) {
        if (kieSessions.containsKey(id)) {
            return kieSessions.get(id).isInitialized();
        } else {
            return false;
        }
    }

    public void setInitialized(String id) {
        kieSessions.get(id).setInitialized(true);
    }

    public KieSession getKieSession(String id) {
        KieSessionWrapper wrappedKB = kieSessions.get(id);
        return wrappedKB == null ? null : wrappedKB.getKieSession();
    }

    public int getNumKieSessions() {
        return kieSessions.size();
    }

    private KieSessionWrapper getOtherwiseCreate(String id) {
        KieSessionWrapper kb;
        if (kieSessions.containsKey(id)) {
            kb = kieSessions.get(id);
        } else {
            kb = new KieSessionWrapper(commandGateway, artifactService);
            kieSessions.put(id, kb);
        }
        return kb;
    }

}
