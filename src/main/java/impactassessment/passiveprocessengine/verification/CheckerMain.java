package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflows.DronologyWorkflowFixed;

import java.util.Scanner;

public class CheckerMain {

    public static void main(String args[]) {
        System.setProperty( "org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer" );

        DronologyWorkflowFixed workflow = new DronologyWorkflowFixed();
        Checker checker = new Checker();
        Report report = checker.check(workflow);
        System.out.println("---------------WARNINGS---------------");
        report.getWarnings().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifacts()));
        System.out.println("---------------PATCHES----------------");
        report.getPatches().forEach(w -> System.out.println(w.getDescription() + " ID: " + w.getAffectedArtifact()));
        System.out.println("--------------------------------------");
        Scanner in = new Scanner(System.in);
        in.nextLine();
    }

}
