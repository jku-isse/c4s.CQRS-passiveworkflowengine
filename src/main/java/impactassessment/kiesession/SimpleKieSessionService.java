package impactassessment.kiesession;

import artifactapi.IArtifactRegistry;
import impactassessment.artifactconnector.jira.IJiraService;
import impactassessment.command.IGatewayProxy;
import impactassessment.command.IGatewayProxyFactory;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

public class SimpleKieSessionService implements IKieSessionService {

	//CommandGateway cg;
	IArtifactRegistry artifactRegistry;
	IGatewayProxyFactory gpf;

    private Map<String, KieSessionWrapper> kieSessions;

    public SimpleKieSessionService(IArtifactRegistry aRegistry, IGatewayProxyFactory gpf) {
        kieSessions = new HashMap<>();
      //  this.cg = cg;
        this.gpf = gpf;
        this.artifactRegistry = aRegistry;

    }

    @Override
    public void insertOrUpdate(String id, Object o) {

        kieSessions.get(id).insertOrUpdate(o);
    }


    @Override
    public IGatewayProxy create(String id, KieContainer kieContainer) {
    	IGatewayProxy gw;
    	KieSessionWrapper kieSessionWrapper = new KieSessionWrapper(artifactRegistry, gpf);
    	if (kieContainer == null) {
    		gw = kieSessionWrapper.create();
    	} else {
    		gw = kieSessionWrapper.create(kieContainer);
    	}
    	kieSessions.put(id, kieSessionWrapper);
    	return gw;
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
