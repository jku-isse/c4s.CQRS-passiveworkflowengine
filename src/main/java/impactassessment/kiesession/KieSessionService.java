package impactassessment.kiesession;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class KieSessionService {

    private final ApplicationContext appContext;

    private Map<String, KieSessionWrapper> kieSessions;

    public KieSessionService(ApplicationContext appContext) {
        this.appContext = appContext;
        kieSessions = new HashMap<>();
    }


    public void insertOrUpdate(String id, Object o) {
        kieSessions.get(id).insertOrUpdate(o);
    }

    public void create(String id, KieContainer kieContainer) {
        KieSessionWrapper kieSessionWrapper = appContext.getBean(KieSessionWrapper.class);
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