package at.jku.isse.passiveprocessengine.frontend.security.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/*
 * Used to track which logged in user may see a restrictions for a particular process definition
 */

@Entity
@Table(name="restrictionproxy")
public class RestrictionProxy {

	public static String RESTRICTION_SELECTOR = "_RESTRICTION";
	public static String REPAIR_SELECTOR = "_REPAIR";
	
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
