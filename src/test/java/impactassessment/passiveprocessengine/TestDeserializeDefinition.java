package impactassessment.passiveprocessengine;

import impactassessment.exampleworkflows.DronologyWorkflow;
import impactassessment.exampleworkflows.DronologyWorkflowFixed;
import impactassessment.exampleworkflows.MultiStepSubWPWorkflow;
import impactassessment.exampleworkflows.NestedWorkflow;
import org.junit.Test;
import passiveprocessengine.definition.WorkflowDefinition;
import passiveprocessengine.persistance.json.DefinitionSerializer;

public class TestDeserializeDefinition {

    // MultiStepSubWPWorkflow

    @Test
    public void testSerializationMultiStepSubWPWorkflow() {
        MultiStepSubWPWorkflow wfd = new MultiStepSubWPWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        System.out.println(json);
    }
    @Test
    public void testDeserializationMultiStepSubWPWorkflow() {
        MultiStepSubWPWorkflow wfd = new MultiStepSubWPWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        WorkflowDefinition wfd2 = ser.fromJson(json);
        wfd2.getId();
    }

    // DronologyWorkflowFixed

    @Test
    public void testSerializationDronologyWorkflowFixed() {
        DronologyWorkflowFixed wfd = new DronologyWorkflowFixed();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        System.out.println(json);
    }
    @Test
    public void testDeserializationDronologyWorkflowFixed() {
        DronologyWorkflowFixed wfd = new DronologyWorkflowFixed();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        WorkflowDefinition wfd2 = ser.fromJson(json);
        wfd2.getId();
    }

    // DronologyWorkflow

    @Test
    public void testSerializationDronologyWorkflow() {
        DronologyWorkflow wfd = new DronologyWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        System.out.println(json);
    }

    // NestedWorkflow

    @Test
    public void testSerializationNestedWorkflow() {
        NestedWorkflow wfd = new NestedWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        System.out.println(json);
    }
    @Test
    public void testDeserializationNestedWorkflow() {
        NestedWorkflow wfd = new NestedWorkflow();
        wfd.initWorkflowSpecification();
        DefinitionSerializer ser = new DefinitionSerializer();
        String json = ser.toJson(wfd);
        WorkflowDefinition wfd2 = ser.fromJson(json);
        wfd2.getId();
    }

}
