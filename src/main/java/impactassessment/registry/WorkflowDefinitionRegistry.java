package impactassessment.registry;

import impactassessment.kiesession.KieSessionFactory;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.persistance.json.DefinitionSerializer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Scope("singleton")
public class WorkflowDefinitionRegistry {

    private Map<String, WorkflowDefinitionContainer> definitions = new HashMap<>();
    private DefinitionSerializer serializer = new DefinitionSerializer();
    private KieSessionFactory kieSessionFactory = new KieSessionFactory();

    public void register(String json, Map<String, String> ruleFiles) {
        WorkflowDefinition wfd = serializer.fromJson(json);
        KieContainer kieContainer = kieSessionFactory.getKieContainerFromStrings(ruleFiles.values());

        persist(wfd.getId(), json, ruleFiles);
        register(wfd.getId(), wfd, kieContainer);
    }

    private void persist(String name, String json, Map<String, String> ruleFiles) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL definitionsURL = classLoader.getResource("processdefinition");
        try {
            File file = new File(definitionsURL.getPath()+name+"/"+name+".json");
            file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(json);
            writer.close();

            for (Map.Entry<String, String> entry : ruleFiles.entrySet()) {
                BufferedWriter drlWriter = new BufferedWriter(new FileWriter(definitionsURL.getPath()+name+"/"+entry.getKey()));
                drlWriter.write(entry.getValue());
                drlWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register(String name, WorkflowDefinition wfd, KieContainer kieContainer) {
        WorkflowDefinitionContainer def = new WorkflowDefinitionContainer(name, wfd, kieContainer);
        definitions.put(name, def);
    }

    public WorkflowDefinitionContainer get(String name) {
        return definitions.get(name);
    }

    public Map<String, WorkflowDefinitionContainer> getAll() {
        return definitions;
    }
}
