package impactassessment.passiveprocessengine;

import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import impactassessment.passiveprocessengine.persistance.DefinitionSerializer;
import impactassessment.passiveprocessengine.workflows.MultiStepSubWPWorkflow;
import org.junit.Test;

public class TestDeserializeDefinition {
    @Test
    public void testSerialization() {
        MultiStepSubWPWorkflow wfd = new MultiStepSubWPWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        System.out.println(json);
    }

    @Test
    public void testDeserialization() {
        MultiStepSubWPWorkflow wfd = new MultiStepSubWPWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        WorkflowDefinition wfd2 = ser.fromJson(json);
        wfd2.getId();
    }

}
