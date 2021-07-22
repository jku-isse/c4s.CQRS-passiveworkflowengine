package c4s.impactassessment.tools;

import java.util.LinkedHashMap;

public class NamedHashMap<K, V> extends LinkedHashMap<K, V>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String name;

	public NamedHashMap(String name) {
		super();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "NamedHashMap [name=" + name + "]" + super.toString();
	}
	
	
}
