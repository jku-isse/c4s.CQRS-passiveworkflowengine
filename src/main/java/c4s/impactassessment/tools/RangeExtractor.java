package c4s.impactassessment.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class RangeExtractor {

	Node exclFrom;
	Node exclTo;
	boolean foundFrom = false;
	boolean foundTo = false;
	
	public RangeExtractor(Node exclFrom, Node exclTo) {
		super();
		this.exclFrom = exclFrom;
		this.exclTo = exclTo;
	}
	
	
	public List<Node> extract(Node scope) {
		List<Node> nodes = new LinkedList<Node>();
		getNodesBetween(scope, nodes);
		// filter out <br> nodes
		return nodes.stream()
			.filter(node -> !isBRnodeOrWhitespace(node) )
			.collect(Collectors.toList());
		//return nodes;
	}
	
	private void getNodesBetween(Node scope, Collection<Node> currentCollection) {
		// we do a depth first search for the from Node, then adding nodes in order lowest level first, in sequence per hierachy
		for(int i = 0; i<scope.childNodes().size(); i++) {
			Node cNode = scope.childNodes().get(i);
			
			if (!foundFrom) { // continue find startnode
				if (cNode.equals(exclFrom)) { 
					foundFrom = true;
					continue;
				}
				else if (cNode instanceof Element  && cNode.childNodeSize() > 0)
					getNodesBetween((Element) cNode, currentCollection);
			} else {
				// check for end node
				if (cNode.equals(exclTo)) foundTo = true;	 
				if (foundTo) return; // no further adding of nodes

				if (foundFrom) { // perhaps found in some child node
					
					if (!containsEndNode(cNode)) // if there is no end node, we just add this node but not lower as this would sort of duplicate the node content
						currentCollection.add(cNode);
					else {
						// add all child nodes
						if (cNode instanceof Element  && cNode.childNodeSize() > 0)
							getNodesBetween((Element) cNode, currentCollection);
					}
				} // else we continue with next node
			}
		}
	}
	
	private boolean containsEndNode(Node scope) {
		if (scope.equals(exclTo))
			return true;
		else if (scope instanceof Element) {
			for(int i = 0; i<scope.childNodes().size(); i++) {
				if (containsEndNode(scope.childNodes().get(i)))
					return true;
			}
			return false;
		} else 
			return false;
	}
	
	private boolean isBRnodeOrWhitespace(Node node) {
		if (node instanceof Element) {
			Element el = (Element)node;
			if ( el.tagName().equalsIgnoreCase("br") )
				return true;
		} else 
		if (node instanceof TextNode) {
			TextNode tn = (TextNode)node;
			String result = tn.getWholeText().trim().replace("\\n", "").trim();
			if (result.length() == 0)
				return true;
		}
		return false;
	}
	
	public static String getCleanedContent(Node node) {
		if (node == null) return null;
		if (node instanceof Element)
			return clean(((Element) node).text());
		if (node instanceof TextNode) 
			return clean(((TextNode)node).getWholeText());
		else return clean(node.toString());
	}
	
	private static String clean(String s) {
		return s.replace("\\n", "").trim();
	}
	
	public static List<Node> getContentBetweenNodeAndNextNode(Node node, List<Node> anchors, Node scope) {
		int posNode = anchors.indexOf(node);
		if (posNode < 0 || posNode==(anchors.size()-1)) 
			return Collections.emptyList();
		else
			return new RangeExtractor(node, anchors.get(posNode+1)).extract(scope);
	}
	
	public static boolean doesNodeContainString(Node node, String string) {
		if (node == null) return false;
		if (node instanceof Element)
			return ((Element) node).text().contains(string);
		if (node instanceof TextNode) 
			return ((TextNode)node).getWholeText().contains(string);
		else return node.toString().contains(string);
	}
	
	public static String appendContentOfNodesBetween(Node from, List<Node> anchors, Node scope) {
		StringBuffer content = new StringBuffer();
		RangeExtractor.getContentBetweenNodeAndNextNode(from, anchors, scope).stream()
			.filter(node -> !doesNodeContainString(node, "Wenn nicht notwendig, diesen Abschnitt"))
			.forEach(node -> {
			content.append(RangeExtractor.getCleanedContent(node));
		});
		return content.toString();
	}
	
	
}
