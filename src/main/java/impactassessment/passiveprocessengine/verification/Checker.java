package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflowmodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.DND;
import static impactassessment.passiveprocessengine.verification.WorkflowNode.NodeType.TD;

public class Checker {

    private WorkflowGraph graph;

    public Report check(AbstractWorkflowDefinition workflow) {
        return evaluate(workflow);
    }

//    public Report checkAndPatch(AbstractWorkflowDefinition workflow) {
//        return evaluate(workflow, true);
//    }

    private Report evaluate(AbstractWorkflowDefinition workflow/*, boolean patchingEnabled*/) {
        Report report = new Report();
        workflow.createInstance("dummy"); // instance is not used, but must be instantiated to build definition
        // create graph data structure for evaluation
        graph = new WorkflowGraph(workflow);
        graph.display();
//        if (patchingEnabled) {
//            // patch repairable flaws of the workflow
//            for (Report.Warning warning : checkPlaceholderNeeded()) {
//                report.addPatches(createPlaceholder(workflow, warning.getAffectedArtifacts()));
//            }
//            graph = new WorkflowGraph(workflow); // graph has to be rebuilt for checks after patching workflow
//            graph.display();
//        }
        report.addWarnings(checkUnconnectedDecisionNode(workflow));
        report.addWarnings(checkUnconnectedTaskDefinition(workflow));
        report.addWarnings(checkKickoff());
        report.addWarnings(checkDecisionNodeOutBranch()); // TODO end DND possible
        report.addWarnings(checkPlaceholderNeeded());
        report.addWarnings(checkTaskDefinitionConnected());
        // TODO add more aspects to check..
        return report;
    }

    // ------------------------------------- individual checks -----------------------------------------------

    private Report.Warning[] checkUnconnectedDecisionNode(AbstractWorkflowDefinition workflow) {
        List<String> unconnected = new ArrayList<>();
        for (DecisionNodeDefinition dnd : workflow.getDecisionNodeDefinitions()) {
            List<String> graphIDs = graph.getNodes().stream().filter(n -> n.getType().equals(DND)).map(n -> n.getId()).collect(Collectors.toList());
            if (!graphIDs.contains(dnd.getId())) {
                unconnected.add(dnd.getId());
            }
        }
        if (unconnected.size() > 0) {
            String[] a = new String[unconnected.size()];
            return new Report.Warning[]{new Report.Warning("There exist unconnected DecisionNodeDefinition(s)!", unconnected.toArray(a))};
        } else {
            return null;
        }
    }

    private Report.Warning[] checkUnconnectedTaskDefinition(AbstractWorkflowDefinition workflow) {
        List<String> unconnected = new ArrayList<>();
        for (TaskDefinition td : workflow.getWorkflowTaskDefinitions()) {
            List<String> graphIDs = graph.getNodes().stream().filter(n -> n.getType().equals(TD)).map(n -> n.getId()).collect(Collectors.toList());
            if (!graphIDs.contains(td.getId())) {
                unconnected.add(td.getId());
            }
        }
        if (unconnected.size() > 0) {
            String[] a = new String[unconnected.size()];
            return new Report.Warning[]{new Report.Warning("There exist unconnected TaskDefinition(s)!", unconnected.toArray(a))};
        } else {
            return null;
        }
    }

    private Report.Warning[] checkKickoff() {
        List<String> kickoffs = graph.getNodes().stream()
                .filter(n -> n.getType().equals(DND))
                .filter(n -> n.getPredecessors().size() == 0)
                .map(WorkflowNode::getId)
                .collect(Collectors.toList());
        if (kickoffs.size() == 0) {
            return new Report.Warning[]{ new Report.Warning("No DecisionNodeDefinitions with zero incoming branches detected, but there should be exactly one!", "")};
        } else if (kickoffs.size() > 1) {
            String[] a = new String[kickoffs.size()];
            return new Report.Warning[]{ new Report.Warning("Multiple DecisionNodeDefinitions with zero incoming branches detected, but there should be exactly one!", kickoffs.toArray(a))};
        } else {
            return null;
        }
    }

    private Report.Warning[] checkDecisionNodeOutBranch() {
        return graph.getNodes().stream()
                .filter(n -> n.getType().equals(DND))
                .filter(n -> n.getSuccessors().size() == 0)
                .map(n -> new Report.Warning("DecisionNodeDefinition should have at least one out-branch!", n.getId()))
                .toArray(Report.Warning[]::new);
    }

    private Report.Warning[] checkPlaceholderNeeded() {
        return graph.getNodes().stream()
                .filter(n -> n.getType().equals(TD))
                .filter(n -> n.getPredecessors().size() > 1)
                .map(n -> new Report.Warning("TaskDefinition has two incoming connections!", n.getId()))
                .toArray(Report.Warning[]::new);
    }

    private Report.Warning[] checkTaskDefinitionConnected() {
        return graph.getNodes().stream()
                .filter(n -> n.getType().equals(TD))
                .filter(n -> n.getPredecessors().size() == 0)
                .map(n -> new Report.Warning("TaskDefinition has zero incoming branches!", n.getId()))
                .toArray(Report.Warning[]::new);
    }

    // --------------------- repair utilities --------------------------------------

//    private Report.Patch[] createPlaceholder(AbstractWorkflowDefinition workflow, List<String> taskDefinitionIDs) {
//        for (String tdID : taskDefinitionIDs) {
//            WorkflowNode td = graph.getNodeMap().get(tdID);
//            if (td.getPredecessors().size() == 2) { // only capable of fixing two incoming branches, not more
//                WorkflowNode[] nodes = new WorkflowNode[2];
//                td.getPredecessors().values().toArray(nodes);
//                WorkflowNode first = nodes[0];
//                WorkflowNode second = nodes[1];
//                boolean firstBeforeSecond = search(first, second.getId());
//                boolean secondBeforeFirst = search(second, first.getId());
//                if (firstBeforeSecond == secondBeforeFirst) { // both true is not possible, both false is not fixable
//                    return null;
//                }
//                // make patch
//                DecisionNodeDefinition dnd1;
//                DecisionNodeDefinition dnd2;
//                if (firstBeforeSecond) {
//                    dnd1 = workflow.getDNDbyID(first.getId());
//                    dnd2 = workflow.getDNDbyID(second.getId());
//                } else {
//                    dnd1 = workflow.getDNDbyID(second.getId());
//                    dnd2 = workflow.getDNDbyID(first.getId());
//                }
//                IBranchDefinition invalidBranch = dnd1.getOutBranches().stream()
//                        .filter(b -> b.getTask().getId().equals(tdID))
//                        .findAny().get();
//                dnd1.getOutBranches().remove(invalidBranch);
//                TaskDefinition placeholder = new TaskDefinition("AUTO_CREATED_PLACEHOLDER", workflow);
//                workflow.getWorkflowTaskDefinitions().add(placeholder);
//                dnd1.addOutBranchDefinition(new DefaultBranchDefinition("placeholderIn", placeholder, true, true, dnd1));
//                dnd2.addInBranchDefinition(new DefaultBranchDefinition("placeholderOut", placeholder, true, true, dnd2));
//                return new Report.Patch[]{new Report.Patch("Introduced placeholder task between " + dnd1.getId() + " and " + dnd2.getId(), placeholder.getId())};
//            }
//        }
//        return null;
//    }
//
//    private boolean search(WorkflowNode n, String id) {
//        for (WorkflowNode m : n.getSuccessors().values()) {
//            if (search(m, id) || m.getId().equals(id)) {
//                return true;
//            }
//        }
//        return false;
//    }

}
