package impactassessment.passiveprocessengine.verification;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private NodeType type;
    private String id;
    private List<Node> predecessors;
    private List<Node> successors;

    public Node(String id, NodeType type) {
        this.id = id;
        this.type = type;
        predecessors = new ArrayList<>();
        successors = new ArrayList<>();
    }

    public void addPredecessor(Node n) {
        predecessors.add(n);
    }

    public void addSuccessor(Node n) {
        successors.add(n);
    }

    public NodeType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public List<Node> getPredecessors() {
        return predecessors;
    }

    public List<Node> getSuccessors() {
        return successors;
    }

    enum NodeType {DND, TD}
}
