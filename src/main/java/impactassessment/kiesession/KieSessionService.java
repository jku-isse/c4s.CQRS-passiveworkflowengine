package impactassessment.kiesession;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class KieSessionService implements IKieSessionService {

	private final ApplicationContext appContext;

	private Map<String, KieSessionWrapper> kieSessions;

	public KieSessionService(ApplicationContext appContext) {
		this.appContext = appContext;
		kieSessions = new HashMap<>();
	}

	@Override
	public void insertOrUpdate(String id, Object o) {
		kieSessions.get(id).insertOrUpdate(o);
	}

	@Override
	public void create(String id, KieContainer kieContainer) {
		KieSessionWrapper kieSessionWrapper = appContext.getBean(KieSessionWrapper.class);
		if (kieContainer == null) {
			kieSessionWrapper.create();
		} else {
			kieSessionWrapper.create(kieContainer);
		}
		kieSessions.put(id, kieSessionWrapper);
	}

	@Override
	public void fire(String id) {
		kieSessions.get(id).fire();
	}

	@Override
	public void dispose(String id) {
		if (kieSessions.containsKey(id)) {
			kieSessions.get(id).dispose();
			kieSessions.remove(id);
		}
	}

	@Override
	public boolean isInitialized(String id) {
		if (kieSessions.containsKey(id)) {
			return kieSessions.get(id).isInitialized();
		} else {
			return false;
		}
	}

	@Override
	public void setInitialized(String id) {
		kieSessions.get(id).setInitialized(true);
	}

	@Override
	public KieSession getKieSession(String id) {
		KieSessionWrapper wrappedKB = kieSessions.get(id);
		return wrappedKB == null ? null : wrappedKB.getKieSession();
	}

	@Override
	public int getNumKieSessions() {
		return kieSessions.size();
	}

	@Override
	public void remove(String id, Object o) {
		kieSessions.get(id).remove(o);
	}
}
