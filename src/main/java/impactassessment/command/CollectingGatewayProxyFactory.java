package impactassessment.command;

import org.axonframework.commandhandling.gateway.CommandGateway;

public class CollectingGatewayProxyFactory implements IGatewayProxyFactory{

	private CommandGateway gw;
	
	public CollectingGatewayProxyFactory(CommandGateway gw) {
		this.gw = gw;
	}
	
	@Override
	public IGatewayProxy instantiateNewProxy() {
		return new CollectingGatewayProxy(gw);
	}

}
