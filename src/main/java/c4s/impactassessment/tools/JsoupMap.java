package c4s.impactassessment.tools;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;

public class JsoupMap extends NamedHashMap<String, Element>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JsoupMap(String name) {
		super(name);
	}

	public List<org.jsoup.nodes.Node> getValuesAsNodes() {
		return this.values().stream().map(el -> (org.jsoup.nodes.Node)el).collect(Collectors.toList());
	}
	
}
