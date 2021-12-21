package impactassessment.kiesession;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import impactassessment.command.IGatewayProxy;

public class MockKieSessionService implements IKieSessionService {

    @Override
    public void insertOrUpdate(String id, Object o) {

    }

    @Override
    public IGatewayProxy create(String id, KieContainer kieContainer) {
    	return null;
    }

    @Override
    public void fire(String id) {

    }

    @Override
    public void dispose(String id) {

    }

    @Override
    public boolean isInitialized(String id) {
        return false;
    }

    @Override
    public void setInitialized(String id) {

    }

    @Override
    public KieSession getKieSession(String id) {
        return null;
    }

    @Override
    public int getNumKieSessions() {
        return 0;
    }

    @Override
    public void remove(String id, Object o) {

    }
}
