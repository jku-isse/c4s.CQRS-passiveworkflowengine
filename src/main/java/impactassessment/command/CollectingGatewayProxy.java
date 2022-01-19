package impactassessment.command;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.axonframework.commandhandling.gateway.CommandGateway;

import impactassessment.api.Commands.CompositeCmd;
import impactassessment.api.Commands.TrackableCmd;
import passiveprocessengine.instance.WorkflowChangeEvent;

public class CollectingGatewayProxy implements IGatewayProxy {
	
	private CommandGateway gw;
	private WorkflowChangeEvent rootCause;
	
	List<TrackableCmd> cmdList = new LinkedList<>();
	
	public CollectingGatewayProxy(CommandGateway gw) {
		super();
		this.gw = gw;
	}

	@Override
	public void send(TrackableCmd cmd) {
		if (rootCause != null && rootCause.getId() != null)
			cmd.setParentCauseRef(rootCause.getId());
		cmdList.add(cmd);
	}

	@Override
	public void setRootCause(WorkflowChangeEvent rootCause) {
		this.rootCause = rootCause;
	}
	
	public List<TrackableCmd> sendAllAsCompositeCommand() {
		Map<String, List<TrackableCmd>> perAggr = cmdList.stream().collect(Collectors.groupingBy(cmd -> cmd.getId()));
		List<TrackableCmd> dispatchList = perAggr.entrySet().stream().map(entry -> 
		{ 
			CompositeCmd cc = new CompositeCmd(entry.getKey(), entry.getValue());
			if (rootCause != null && rootCause.getId() != null)
				cc.setParentCauseRef(rootCause.getId());
			return cc;
		} ).collect(Collectors.toList());
		cmdList.clear();
		dispatchList.forEach(cmd -> gw.send(cmd));
		return dispatchList;
	}
}
