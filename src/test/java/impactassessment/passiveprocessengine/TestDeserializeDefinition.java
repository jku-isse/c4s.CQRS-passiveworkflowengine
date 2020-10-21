package impactassessment.passiveprocessengine;

import impactassessment.passiveprocessengine.definition.WorkflowDefinition;
import impactassessment.passiveprocessengine.persistance.DefinitionSerializer;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflow;
import impactassessment.passiveprocessengine.workflows.DronologyWorkflowFixed;
import impactassessment.passiveprocessengine.workflows.MultiStepSubWPWorkflow;
import impactassessment.passiveprocessengine.workflows.NestedWorkflow;
import org.junit.Test;

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
