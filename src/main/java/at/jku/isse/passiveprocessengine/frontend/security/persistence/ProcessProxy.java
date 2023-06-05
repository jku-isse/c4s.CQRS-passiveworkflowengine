package at.jku.isse.passiveprocessengine.frontend.security.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/*
 * Used to track which logged in user may see a particular process instance
 */

@Entity
@Table(name="processproxy")
public class ProcessProxy {
	@Id
	@Column
	private Integer id;
	
	@Column
	private String name;

	public Integer getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
}
