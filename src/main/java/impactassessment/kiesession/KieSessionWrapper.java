package impactassessment.kiesession;

import artifactapi.IArtifact;
import artifactapi.IArtifactRegistry;
import impactassessment.command.IGatewayProxy;
import impactassessment.command.IGatewayProxyFactory;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import passiveprocessengine.definition.AbstractIdentifiableObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Scope("prototype")
public class KieSessionWrapper {

    private @Getter KieSession kieSession;
    private Map<String, FactHandle> sessionHandles;
    private @Getter @Setter boolean isInitialized;

   // private final CommandGateway commandGateway;
    private final IArtifactRegistry artifactRegistry;
    private final IGatewayProxyFactory gpf;

    public KieSessionWrapper(IArtifactRegistry artifactRegistry, IGatewayProxyFactory gpf) {
    //    this.commandGateway = commandGateway;
        this.artifactRegistry = artifactRegistry;
        this.gpf = gpf;
        sessionHandles = new HashMap<>();
        isInitialized = false;
    }

    @Deprecated
    public IGatewayProxy create() {
        Properties props = new Properties();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("application.properties").getFile());
            FileReader reader = new FileReader(file);
            props.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ruleFiles =  props.getProperty("ruleFiles");
        ruleFiles.replace(" ", "");
        String[] ruleFilesArray = ruleFiles.split(",");

        this.kieSession = new KieSessionFactory().getKieSession(ruleFilesArray);
        return setGlobals();
    }

    public IGatewayProxy create(KieContainer kieContainer) {
        this.kieSession = kieContainer.newKieSession();
        KieSessionLogger.addRuleRuntimeEventListener(this.kieSession);
        KieSessionLogger.addAgendaEventListener(this.kieSession);
        return setGlobals();
    }

    private IGatewayProxy setGlobals() {
    	IGatewayProxy gw = gpf.instantiateNewProxy();// new GatewayProxy(commandGateway);
        this.kieSession.setGlobal("commandGateway", gw);
        this.kieSession.setGlobal("artifactRegistry", artifactRegistry);
        return gw;
    }

    public void insertOrUpdate(Object o) {
        if (o instanceof AbstractIdentifiableObject) {
            AbstractIdentifiableObject idO = (AbstractIdentifiableObject) o;
            String key = idO.getId() + "[" + idO.getClass().getSimpleName() + "]";
            insertOrUpdate(key, idO);
        } else if (o instanceof IArtifact) {
            IArtifact a = (IArtifact) o;
            String key = a.getArtifactIdentifier().getId() + "[" + a.getClass().getSimpleName() + "]";
            insertOrUpdate(key, a);
        } else {
            // unmanaged objects
            kieSession.insert(o);
        }
    }

    public void remove(Object o) {
        if (o instanceof AbstractIdentifiableObject) {
            AbstractIdentifiableObject idO = (AbstractIdentifiableObject) o;
            String key = idO.getId() + "[" + idO.getClass().getSimpleName() + "]";
            FactHandle handle = sessionHandles.remove(key);
            if (handle != null)
                kieSession.delete(handle);
        } else if (o instanceof IArtifact) {
            IArtifact a = (IArtifact) o;
            String key = a.getArtifactIdentifier().getId() + "[" + a.getClass().getSimpleName() + "]";
            FactHandle handle = sessionHandles.remove(key);
            if (handle != null)
                kieSession.delete(handle);
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
