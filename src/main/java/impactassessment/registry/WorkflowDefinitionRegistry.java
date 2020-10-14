package impactassessment.registry;

import impactassessment.kiesession.KieSessionFactory;
import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import impactassessment.passiveprocessengine.persistance.DefinitionSerializer;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Scope("singleton")
public class WorkflowDefinitionRegistry {

    private Map<String, WorkflowDefinitionContainer> definitions = new HashMap<>();
    private DefinitionSerializer serializer = new DefinitionSerializer();
    private KieSessionFactory kieSessionFactory = new KieSessionFactory();

    public void register(String workflowName, String json, List<String> ruleFiles) {
        WorkflowDefinition wfd = serializer.fromJson(json);
        KieContainer kieContainer = kieSessionFactory.getKieContainerFromStrings(ruleFiles);
        register(workflowName, wfd, kieContainer);
    }

    public void register(String name, WorkflowDefinition wfd, KieContainer kieContainer) {
        WorkflowDefinitionContainer def = new WorkflowDefinitionContainer(name, wfd, kieContainer);
        definitions.put(name, def);
    }

    public WorkflowDefinitionContainer get(String name) {
        return definitions.get(name);
    }

    public Map<String, WorkflowDefinitionContainer> getDefinitions() {
        return definitions;
    }
}
