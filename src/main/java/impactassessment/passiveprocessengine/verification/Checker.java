package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflowmodel.*;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.DND;
import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.TD;

public class Checker {

    private Map<String, WorkflowNode> nodes;

    public Report check(AbstractWorkflowDefinition workflow) {
        return evaluate(workflow, false);
    }

    public Report checkAndPatch(AbstractWorkflowDefinition workflow) {
        return evaluate(workflow, true);
    }

    private Report evaluate(AbstractWorkflowDefinition workflow, boolean patchingEnabled) {
        Report report = new Report();
        workflow.createInstance("dummy"); // instance is not used, but must be instantiated to build definition
        // create graph data structure for evaluation
        buildGraph(workflow);
        display();
        if (patchingEnabled) {
            // patch repairable flaws of the workflow
            for (Report.Warning warning : checkPlaceholderNeeded()) {
                report.addPatches(createPlaceholder(workflow, warning.getAffectedArtifact()));
            }
            buildGraph(workflow); // graph has to be rebuilt for checks after patching workflow
            display();
        }
        report.addWarnings(checkKickoff());
        report.addWarnings(checkLoops());
        report.addWarnings(checkDecisionNodeOutBranch());
        report.addWarnings(checkPlaceholderNeeded());
        // TODO add more aspects to check..
        return report;
    }

    // ------------------------------------- individual checks -----------------------------------------------

    private Report.Warning[] checkKickoff() {
        // TODO implement
        return null;
    }

    private Report.Warning[] checkLoops() {
        // TODO implement
        return null;
    }

    private Report.Warning[] checkDecisionNodeOutBranch() {
        return nodes.values().stream()
                .filter(n -> n.getType().equals(DND))
                .filter(n -> n.getSuccessors().size() == 0)
                .map(n -> new Report.Warning("DecisionNodeDefinition should have at least one out-branch!", n.getId()))
                .toArray(Report.Warning[]::new);
    }

    private Report.Warning[] checkPlaceholderNeeded() {
        return nodes.values().stream()
                .filter(n -> n.getType().equals(TD))
                .filter(n -> n.getPredecessors().size() > 1)
                .map(n -> new Report.Warning("TaskDefinition has two incoming connections!", n.getId()))
                .toArray(Report.Warning[]::new);
    }

    // --------------------- repair utilities --------------------------------------

    private Report.Patch[] createPlaceholder(AbstractWorkflowDefinition workflow, String taskDefinitionID) {
        WorkflowNode td = nodes.get(taskDefinitionID);
        if (td.getPredecessors().size() == 2) { // only capable of fixing two incoming branches, not more
            WorkflowNode[] nodes = new WorkflowNode[2];
            td.getPredecessors().values().toArray(nodes);
            WorkflowNode first = nodes[0];
            WorkflowNode second = nodes[1];
            boolean firstBeforeSecond = search(first, second.getId());
            boolean secondBeforeFirst = search(second, first.getId());
            if (firstBeforeSecond == secondBeforeFirst) { // both true is not possible, both false is not fixable
                return null;
            }
            // make patch
            DecisionNodeDefinition dnd1;
            DecisionNodeDefinition dnd2;
            if (firstBeforeSecond) {
                dnd1 = workflow.getDNDbyID(first.getId());
                dnd2 = workflow.getDNDbyID(second.getId());
            } else {
                dnd1 = workflow.getDNDbyID(second.getId());
                dnd2 = workflow.getDNDbyID(first.getId());
            }
            IBranchDefinition invalidBranch = dnd1.getOutBranches().stream()
                    .filter(b -> b.getTask().getId().equals(taskDefinitionID))
                    .findAny().get();
            dnd1.getOutBranches().remove(invalidBranch);
            TaskDefinition placeholder = new TaskDefinition("AUTO_CREATED_PLACEHOLDER", workflow);
            workflow.getWorkflowTaskDefinitions().add(placeholder);
            dnd1.addOutBranchDefinition(new DefaultBranchDefinition("placeholderIn", placeholder, true, true, dnd1));
            dnd2.addInBranchDefinition(new DefaultBranchDefinition("placeholderOut", placeholder, true, true, dnd2));
            return new Report.Patch[]{new Report.Patch("Introduced placeholder task between "+dnd1.getId()+" and "+dnd2.getId(), placeholder.getId())};
        }
        return null;
    }

    private boolean search(WorkflowNode n, String id) {
        for (WorkflowNode m : n.getSuccessors().values()) {
            if (search(m, id) || m.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // --------------------- build graph data structure for easier checking --------------------------------------

    private void buildGraph(AbstractWorkflowDefinition workflow) {
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
