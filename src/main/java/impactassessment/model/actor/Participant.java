package impactassessment.model.actor;

public class Participant implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1900754356077247865L;
	public String username;
//	public String firstname;
//	public String lastname;
//	public String primaryEmail;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
//	public String getFirstname() {
//		return firstname;
//	}
//	public void setFirstname(String firstname) {
//		this.firstname = firstname;
//	}
//	public String getLastname() {
//		return lastname;
//	}
//	public void setLastname(String lastname) {
//		this.lastname = lastname;
//	}
//	public String getPrimaryEmail() {
//		return primaryEmail;
//	}
//	public void setPrimaryEmail(String primaryEmail) {
//		this.primaryEmail = primaryEmail;
//	}
	
	public Participant(String username) {
		super();
		this.username = username;
	}
	
//	public Participant(String username, String primaryEmail) {
//		super();
//		this.username = username;
//		this.primaryEmail = primaryEmail;
//	}
//	
//	public Participant(String username, String firstname, String lastname, String primaryEmail) {
//		super();
//		this.username = username;
//		this.firstname = firstname;
//		this.lastname = lastname;
//		this.primaryEmail = primaryEmail;
//	}
	@Override
	public String toString() {
		return "[Participant: " + username + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Participant other = (Participant) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	
}
