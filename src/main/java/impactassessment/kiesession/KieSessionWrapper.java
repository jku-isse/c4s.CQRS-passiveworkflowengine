package impactassessment.kiesession;

import impactassessment.jiraartifact.IJiraArtifact;
import impactassessment.jiraartifact.IJiraArtifactService;
import impactassessment.passiveprocessengine.workflowmodel.IdentifiableObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@Scope("prototype")
public class KieSessionWrapper {

    private @Getter KieSession kieSession;
    private Map<String, FactHandle> sessionHandles;
    private @Getter @Setter
    boolean isInitialized;

    public KieSessionWrapper(CommandGateway commandGateway, IJiraArtifactService artifactService) {
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
        this.kieSession.setGlobal("commandGateway", commandGateway);
        this.kieSession.setGlobal("artifactService", artifactService);
        sessionHandles = new HashMap<>();
        isInitialized = false;
    }

    public void insertOrUpdate(Object o) {
        if (o instanceof IJiraArtifact) {
            IJiraArtifact a = (IJiraArtifact) o;
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
