package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.definition.DronologyWorkflow;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Map;
import java.util.Scanner;

public class GraphDisplay {

    public static void main(String args[]) {
        DronologyWorkflow workflow = new DronologyWorkflow();
        Checker checker = new Checker();
        Report report = checker.checkAndPatch(workflow);

        Scanner in = new Scanner(System.in);
        in.nextLine();
    }

    public static void display(Map<String, WorkflowNode> nodes) {
        Graph graph = new SingleGraph("Workflow");
        // add nodes
        for (WorkflowNode n : nodes.values()) {
            graph.addNode(n.getId());
        }
        // add edges
        for (WorkflowNode n : nodes.values()) {
            for (WorkflowNode suc : n.getSuccessors().values()) {
                graph.addEdge(n.getId()+"-"+suc.getId(), n.getId(), suc.getId(), true);
            }
        }
        // set attribute
        for (Node node : graph) {
            node.addAttribute("ui.label", node.getId());
        }
        // display
        graph.display();
    }

}
