package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflowmodel.AbstractWorkflowDefinition;
import impactassessment.passiveprocessengine.workflowmodel.DecisionNodeDefinition;
import impactassessment.passiveprocessengine.workflowmodel.IBranchDefinition;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.DND;
import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.TD;

public class WorkflowGraph {

    private Map<String, WorkflowNode> nodes;

    public Map<String, WorkflowNode> getNodeMap() {
        return nodes;
    }

    public Collection<WorkflowNode> getNodes() {
        return nodes.values();
    }

    public WorkflowGraph(AbstractWorkflowDefinition workflow) {
        nodes = new HashMap<>();
        List<DecisionNodeDefinition> dnds = workflow.getDecisionNodeDefinitions();
        DecisionNodeDefinition kickoff = dnds.stream()
                .filter(dnd -> dnd.getInBranches().size() == 0).findAny().get();
        WorkflowNode one = new WorkflowNode(kickoff.getId(), DND);
        connectLayer(dnds, kickoff, one);
    }

    private void connectLayer(List<DecisionNodeDefinition> dnds, DecisionNodeDefinition dnd, WorkflowNode one) {
        for (IBranchDefinition branch : dnd.getOutBranches()) {
            String tdID = branch.getTask().getId();
            WorkflowNode two = new WorkflowNode(tdID, TD);
            connectAndPut(one, two);
            dnds.stream()
                    .filter(d -> d.getInBranches().stream()
                            .anyMatch(x -> x.getTask().getId().equals(tdID)))
                    .forEach(d -> {
                        WorkflowNode three = new WorkflowNode(d.getId(), DND);
                        connectAndPut(two, three);
                        connectLayer(dnds, d, three);
                    });
        }
    }

    private void connectAndPut(WorkflowNode predecessor, WorkflowNode successor) {
        // use already present nodes if possible
        WorkflowNode pre = nodes.getOrDefault(predecessor.getId(), predecessor);
        WorkflowNode suc = nodes.getOrDefault(successor.getId(), successor);
        // connect nodes
        pre.addSuccessor(suc);
        suc.addPredecessor(pre);
        // put nodes into map
        nodes.put(pre.getId(), pre);
        nodes.put(suc.getId(), suc);
    }

    public void display() {
        Graph graph = new SingleGraph("Workflow");
        // add nodes
        for (WorkflowNode n : nodes.values()) {
            Node node = graph.addNode(n.getId());
            if (n.getType().equals(TD)) {
                node.addAttribute("ui.class", "td");
            } else if (n.getType().equals(DND)) {
                node.addAttribute("ui.class", "dnd");
            }
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

        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.display();
    }

    private String styleSheet =
            "graph {"+
            "   padding: 60px;"+
            "}"+
            "node.td {"+
            "   shape: box;"+
            "}"+
            "node.dnd {"+
            "   shape: circle;"+
            "}";
}
