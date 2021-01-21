package impactassessment.projection;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.kiesession.KieSessionWrapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class SimpleKieSessionService {

	CommandGateway cg;
	IJiraArtifactService aService;

    private Map<String, KieSessionWrapper> kieSessions;

    public SimpleKieSessionService(CommandGateway cg, IJiraArtifactService aService) {
        kieSessions = new HashMap<>();
        this.cg = cg;
        this.aService = aService;
    }


    public void insertOrUpdate(String id, Object o) {

        kieSessions.get(id).insertOrUpdate(o);
    }

    public void create(String id, KieContainer kieContainer) {
        KieSessionWrapper kieSessionWrapper = new KieSessionWrapper(cg, aService);
        if (kieContainer == null) {
            kieSessionWrapper.create();
        } else {
            kieSessionWrapper.create(kieContainer);
        }
        kieSessions.put(id, kieSessionWrapper);
    }

    public void fire(String id) {
        kieSessions.get(id).fire();
    }

    public void dispose(String id) {
        if (kieSessions.containsKey(id)) {
            kieSessions.get(id).dispose();
            kieSessions.remove(id);
        }
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

}