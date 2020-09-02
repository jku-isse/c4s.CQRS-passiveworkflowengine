package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflowmodel.*;

import java.util.*;
import java.util.stream.Collectors;

import static impactassessment.passiveprocessengine.verification.Node.NodeType.DND;
import static impactassessment.passiveprocessengine.verification.Node.NodeType.TD;
import static impactassessment.passiveprocessengine.verification.Report.WarningType.*;

public class Checker {

    private Map<String, Node> nodes;

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
        if (patchingEnabled) {
            // patch repairable flaws of the workflow
            for (Report.Warning warning : checkPlaceholderNeeded()) {
                report.addPatches(createPlaceholder(workflow, warning.getAffectedArtifact()));
            }
            buildGraph(workflow); // graph has to be rebuilt for checks after patching workflow
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
                .map(n -> new Report.Warning(STRUCTURE, "DecisionNodeDefinition should have at least one out-branch!", n.getId()))
                .toArray(Report.Warning[]::new);

        /* old implementation without using graph data structure
        return workflow.getDecisionNodeDefinitions().stream()
                .filter(dnd -> dnd.getOutBranches().size() == 0)
                .map(dnd -> dnd.getId())
                .map(s -> new Report.Warning(STRUCTURE, "DecisionNodeDefinition should have at least one out-branch!", s))
                .toArray(Report.Warning[]::new);
         */
    }

    private Report.Warning[] checkPlaceholderNeeded() {
        return nodes.values().stream()
                .filter(n -> n.getType().equals(TD))
                .filter(n -> n.getPredecessors().size() > 1)
                .map(n -> new Report.Warning(PLACEHOLDER, "TaskDefinition has two incoming connections!", n.getId()))
                .toArray(Report.Warning[]::new);

        /* old implementation without using graph data structure
        List<String> wfts = workflow.getDecisionNodeDefinitions().stream()
                .map(dnd -> dnd.getOutBranches().stream()
                    .map(out -> out.getTask().getId())
                    .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return wfts.stream()
                .filter(d -> Collections.frequency(wfts, d) > 1)
                .distinct()
                .map(s -> new Report.Warning(PLACEHOLDER, "TaskDefinition has two incoming connections!", s))
                .toArray(Report.Warning[]::new);
        */
    }

    // --------------------- repair utilities --------------------------------------

    private Report.Patch[] createPlaceholder(AbstractWorkflowDefinition workflow, String taskDefinitionID) {
        Node td = nodes.get(taskDefinitionID);
        if (td.getPredecessors().size() == 2) { // only capable of fixing two incoming branches, not more
            Node[] nodes = new Node[2];
            td.getPredecessors().values().toArray(nodes);
            Node first = nodes[0];
            Node second = nodes[1];
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

    private boolean search(Node n, String id) {
        boolean isSuccessor = false;
        for (Node m : n.getSuccessors().values()) {
            if (m.getId().equals(id)) {
                return true;
            }
            isSuccessor = search(m, id);
        }
        return isSuccessor;
    }

    // --------------------- build graph data structure for easier checking --------------------------------------

    private void buildGraph(AbstractWorkflowDefinition workflow) {
        nodes = new HashMap<>();
        List<DecisionNodeDefinition> dnds = workflow.getDecisionNodeDefinitions();
        DecisionNodeDefinition kickoff = dnds.stream()
                .filter(dnd -> dnd.getInBranches().size() == 0).findAny().get();
        Node one = new Node(kickoff.getId(), DND);
        connectLayer(dnds, kickoff, one);
   }

    private void connectLayer(List<DecisionNodeDefinition> dnds, DecisionNodeDefinition dnd, Node one) {
        for (IBranchDefinition branch : dnd.getOutBranches()) {
            String tdID = branch.getTask().getId();
            Node two = new Node(tdID, TD);
            connectAndPut(one, two);
            dnds.stream()
                    .filter(d -> d.getInBranches().stream()
                            .anyMatch(x -> x.getTask().getId().equals(tdID)))
                    .forEach(d -> {
                        Node three = new Node(d.getId(), DND);
                        connectAndPut(two, three);
                        connectLayer(dnds, d, three);
                    });
        }
    }

    private void connectAndPut(Node predecessor, Node successor) {
        // use already present nodes if possible
        Node pre = nodes.getOrDefault(predecessor.getId(), predecessor);
        Node suc = nodes.getOrDefault(successor.getId(), successor);
        // connect nodes
        pre.addSuccessor(suc);
        suc.addPredecessor(pre);
        // put nodes into map
        nodes.put(pre.getId(), pre);
        nodes.put(suc.getId(), suc);
    }
}
