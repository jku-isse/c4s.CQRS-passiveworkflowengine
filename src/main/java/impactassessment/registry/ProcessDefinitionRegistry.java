package impactassessment.registry;

import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Scope("singleton")
public class ProcessDefinitionRegistry {

    private Map<String, ProcessDefintionObject> definitions = new HashMap<>();

    public void register(String name, WorkflowDefinition wfd, KieContainer kieContainer) {
        ProcessDefintionObject def = new ProcessDefintionObject(name, wfd, kieContainer);
        definitions.put(name, def);
    }

    public ProcessDefintionObject get(String name) {
        return definitions.get(name);
    }

    public Map<String, ProcessDefintionObject> getDefinitions() {
        return definitions;
    }
}
