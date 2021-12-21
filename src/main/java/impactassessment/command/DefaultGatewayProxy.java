package impactassessment.command;

import org.axonframework.commandhandling.gateway.CommandGateway;

import impactassessment.api.Commands.TrackableCmd;
import passiveprocessengine.instance.WorkflowChangeEvent;

public class DefaultGatewayProxy implements IGatewayProxy {
	
	private CommandGateway gw;
	private WorkflowChangeEvent rootCause;
	
	public DefaultGatewayProxy(CommandGateway gw) {
		super();
		this.gw = gw;
	}

	@Override
	public void send(TrackableCmd cmd) {
		if (rootCause != null && rootCause.getId() != null)
			cmd.setParentCauseRef(rootCause.getId());
		gw.send(cmd);
	}

	@Override
	public void setRootCause(WorkflowChangeEvent rootCause) {
		this.rootCause = rootCause;
	}
}
