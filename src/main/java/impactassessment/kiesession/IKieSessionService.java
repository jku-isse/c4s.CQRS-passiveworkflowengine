package impactassessment.kiesession;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public interface IKieSessionService {
    void insertOrUpdate(String id, Object o);

    void create(String id, KieContainer kieContainer);

    void fire(String id);

    void dispose(String id);

    boolean isInitialized(String id);

    void setInitialized(String id);

    KieSession getKieSession(String id);

    int getNumKieSessions();

    void remove(String id, Object o);
}
