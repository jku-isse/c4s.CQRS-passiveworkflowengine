package impactassessment.registry;

import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kie.api.runtime.KieContainer;

@AllArgsConstructor
public class ProcessDefintionObject {

    private @Getter String name;
    private @Getter WorkflowDefinition wfd;
    private @Getter KieContainer kieContainer;

}
