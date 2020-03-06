package counter.workflowmodel.actor;

import java.io.Serializable;

public class Role implements Serializable{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private java.net.URI URI;
	private String name;
	
	public java.net.URI getURI() {
		return URI;
	}
	public void setURI(java.net.URI uri) {
		URI = uri;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Role(java.net.URI uri, String name) {
		super();
		URI = uri;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Role [name=" + name + "]";
	}		
}


