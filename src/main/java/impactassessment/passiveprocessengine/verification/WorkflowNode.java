package impactassessment.passiveprocessengine.verification;

import java.util.HashMap;
import java.util.Map;

public class WorkflowNode {

    private NodeType type;
    private String id;
    private Map<String, WorkflowNode> predecessors;
    private Map<String, WorkflowNode> successors;

    public WorkflowNode(String id, NodeType type) {
        this.id = id;
        this.type = type;
        predecessors = new HashMap<>();
        successors = new HashMap<>();
    }

    public void addPredecessor(WorkflowNode n) {
        predecessors.put(n.getId(), n);
    }

    public void addSuccessor(WorkflowNode n) {
        successors.put(n.getId(), n);
    }

    public NodeType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Map<String, WorkflowNode> getPredecessors() {
        return predecessors;
    }

    public Map<String, WorkflowNode> getSuccessors() {
        return successors;
    }

    enum NodeType {DND, TD}
}
