package impactassessment.passiveprocessengine.actor;

import java.util.Properties;

public class UnresolvableParticipant extends Participant {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7985439019405241819L;
	public UnresolvableParticipant(String username) {
		super(username);
	}

	public UnresolvableParticipant(String username, String resolvingErrorMsg) {
		super(username);
		this.resolvingErrorMsg = resolvingErrorMsg;
	}
	
	private String resolvingErrorMsg;	
	private Properties resolvingStatus = new Properties();
	public String getResolvingErrorMsg() {
		return resolvingErrorMsg;
	}

	public void setResolvingErrorMsg(String resolvingErrorMsg) {
		this.resolvingErrorMsg = resolvingErrorMsg;
	}

	public Properties getResolvingStatus() {
		return resolvingStatus;
	}

	public void setResolvingStatus(Properties resolvingStatus) {
		this.resolvingStatus = resolvingStatus;
	}

	@Override
	public String toString() {
		return "UnresolvableParticipant [resolvingErrorMsg=" + resolvingErrorMsg + ", resolvingStatus="
				+ resolvingStatus + "]";
	}
	
	
	
}
