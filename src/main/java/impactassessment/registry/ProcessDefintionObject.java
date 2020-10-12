package impactassessment.registry;

import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.kie.api.runtime.KieContainer;

@AllArgsConstructor
public class ProcessDefintionObject {

    private @Getter @Setter String name;
    private @Getter @Setter WorkflowDefinition wfd;
    private @Getter @Setter KieContainer kieContainer;

}
