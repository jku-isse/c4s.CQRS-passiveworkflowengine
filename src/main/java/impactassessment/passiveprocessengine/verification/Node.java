package impactassessment.passiveprocessengine.verification;

import java.util.HashMap;
import java.util.Map;

public class Node {

    private NodeType type;
    private String id;
    private Map<String, Node> predecessors;
    private Map<String, Node> successors;

    public Node(String id, NodeType type) {
        this.id = id;
        this.type = type;
        predecessors = new HashMap<>();
        successors = new HashMap<>();
    }

    public void addPredecessor(Node n) {
        predecessors.put(n.getId(), n);
    }

    public void addSuccessor(Node n) {
        successors.put(n.getId(), n);
    }

    public NodeType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Map<String, Node> getPredecessors() {
        return predecessors;
    }

    public Map<String, Node> getSuccessors() {
        return successors;
    }

    enum NodeType {DND, TD}
}
