package impactassessment.command;

import impactassessment.api.Commands.TrackableCmd;
import passiveprocessengine.instance.WorkflowChangeEvent;

public interface IGatewayProxy {

	void send(TrackableCmd cmd);

	void setRootCause(WorkflowChangeEvent rootCause);

	Object send(Object x);

}