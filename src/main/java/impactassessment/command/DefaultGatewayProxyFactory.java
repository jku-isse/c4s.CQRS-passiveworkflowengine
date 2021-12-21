package impactassessment.command;

import org.axonframework.commandhandling.gateway.CommandGateway;

public class DefaultGatewayProxyFactory implements IGatewayProxyFactory{

	private CommandGateway gw;
	
	public DefaultGatewayProxyFactory(CommandGateway gw) {
		this.gw = gw;
	}
	
	@Override
	public IGatewayProxy instantiateNewProxy() {
		return new DefaultGatewayProxy(gw);
	}

}
