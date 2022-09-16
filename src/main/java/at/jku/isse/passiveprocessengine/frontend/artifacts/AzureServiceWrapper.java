package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.designspace.artifactconnector.core.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.designspace.azure.model.AzureBaseElementType;
import at.jku.isse.designspace.azure.service.IAzureService;



@Component
public class AzureServiceWrapper implements IArtifactProvider{

	@Autowired
	public IAzureService azureService;
	protected Set<String> supportedTypes = new HashSet<>();
	
	@Override
	public ServiceResponse getServiceResponse(String id, String service) {
		return azureService.getServiceResponse(id, service); // just a delegate call
	}

	@Override
	public boolean isProviding(String artifactType) {
		return supportedTypes.contains(artifactType);
	}

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        for (AzureBaseElementType gbet : AzureBaseElementType.values()) {
        	gbet.getType(); //init this type so its known in designspace immediately upon starting
        	supportedTypes.add(gbet.toString().toLowerCase());
        }
    }
	
}
