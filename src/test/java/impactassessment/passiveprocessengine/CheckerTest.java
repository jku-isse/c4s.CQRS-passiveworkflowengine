package impactassessment.passiveprocessengine;


import impactassessment.exampleworkflows.ComplexWorkflow;
import impactassessment.exampleworkflows.DronologyWorkflow;
import impactassessment.exampleworkflows.DronologyWorkflowFixed;
import impactassessment.exampleworkflows.UncleanWorkflow;
import org.junit.jupiter.api.Test;
import passiveprocessengine.verification.Checker;
import passiveprocessengine.verification.Report;

import static org.junit.Assert.assertEquals;

public class CheckerTest {

    @Test
    public void testCheckingOfComplexWorkflow() {
        ComplexWorkflow workflow = new ComplexWorkflow();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        // log result
        System.out.println("---------------WARNINGS---------------");
        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
        System.out.println("--------------------------------------");
        // assertions
        assertEquals(2, report.getWarnings().size());
    }

    @Test
    public void testCheckingOfUncleanWorkflow() {
        UncleanWorkflow workflow = new UncleanWorkflow();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        // log result
        System.out.println("---------------WARNINGS---------------");
        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
        System.out.println("--------------------------------------");
        // assertions
        assertEquals(4, report.getWarnings().size());
    }

    @Test
    public void testCheckingOfDronologyWorkflow() {
        DronologyWorkflow workflow = new DronologyWorkflow();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        // log result
        System.out.println("---------------WARNINGS---------------");
        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
        System.out.println("--------------------------------------");
        // assertions
        assertEquals(4, report.getWarnings().size());
    }

    @Test
    public void testCheckingOfDronologyWorkflowFixed() {
        DronologyWorkflowFixed workflow = new DronologyWorkflowFixed();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        // log result
        System.out.println("---------------WARNINGS---------------");
        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
        System.out.println("--------------------------------------");
        // assertions
        assertEquals(5, report.getWarnings().size());
    }

//    @Test
//    public void testCheckingAndPatchingOfComplexWorkflow() {
//        ComplexWorkflow workflow = new ComplexWorkflow();
//        Checker checker = new Checker();
//        Report report = checker.checkAndPatch(workflow);
//        // log result
//        System.out.println("---------------WARNINGS---------------");
//        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
//        System.out.println("---------------PATCHES----------------");
//        report.getPatches().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifact()));
//        System.out.println("--------------------------------------");
//        // assertions
//        assertEquals(0, report.getWarnings().size());
//    }
//
//    @Test
//    public void testCheckingAndPatchingOfUncleanWorkflow() {
//        UncleanWorkflow workflow = new UncleanWorkflow();
//        Checker checker = new Checker();
//        Report report = checker.checkAndPatch(workflow);
//        // log result
//        System.out.println("---------------WARNINGS---------------");
//        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
//        System.out.println("---------------PATCHES----------------");
//        report.getPatches().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifact()));
//        System.out.println("--------------------------------------");
//        // assertions
//        assertEquals(3, report.getWarnings().size());
//    }
//
//    @Test
//    public void testCheckingAndPatchingOfDronologyWorkflow() {
//        DronologyWorkflow workflow = new DronologyWorkflow();
//        Checker checker = new Checker();
//        Report report = checker.checkAndPatch(workflow);
//        // log result
//        System.out.println("---------------WARNINGS---------------");
//        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
//        System.out.println("---------------PATCHES----------------");
//        report.getPatches().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifact()));
//        System.out.println("--------------------------------------");
//        // assertions
//        assertEquals(0, report.getWarnings().size());
//        assertEquals(1, report.getPatches().size());
//    }

}
