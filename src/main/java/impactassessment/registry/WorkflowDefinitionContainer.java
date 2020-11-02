package impactassessment.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.kie.api.runtime.KieContainer;
import passiveprocessengine.definition.WorkflowDefinition;

@AllArgsConstructor
public class WorkflowDefinitionContainer {

    private @Getter @Setter String name;
    private @Getter @Setter WorkflowDefinition wfd;
    private @Getter @Setter KieContainer kieContainer;

}
