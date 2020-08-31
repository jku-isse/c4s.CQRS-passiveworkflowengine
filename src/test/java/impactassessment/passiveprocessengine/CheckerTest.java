package impactassessment.passiveprocessengine;

import impactassessment.passiveprocessengine.verification.Checker;
import impactassessment.passiveprocessengine.verification.Report;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CheckerTest {

    @Test
    public void testCheckingOfComplexWorkflow() {
        ComplexWorkflow workflow = new ComplexWorkflow();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        assertEquals(0, report.getWarnings().size());
    }

    @Test
    public void testCheckingOfUncleanWorkflow() {
        UncleanWorkflow workflow = new UncleanWorkflow();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        report.getWarnings().forEach(w -> System.out.println(w.getDescription()));
        assertEquals(1, report.getWarnings().size());
    }
}
