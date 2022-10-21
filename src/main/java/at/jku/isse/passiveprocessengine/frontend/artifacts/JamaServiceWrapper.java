package at.jku.isse.passiveprocessengine.frontend.artifacts;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import at.jku.isse.designspace.artifactconnector.core.endpoints.grpc.service.ServiceResponse;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.jama.model.JamaBaseElementType;
import at.jku.isse.designspace.jama.service.IJamaService;

@Component
public class JamaServiceWrapper implements IArtifactProvider {

	@Autowired
	public IJamaService jamaService;
	protected Set<String> supportedTypes = new HashSet<>();
	
	@Override
	public ServiceResponse getServiceResponse(String id, String service) {
		//return jamOptional.getJamaItem(id, service); // just a delegate call		
		Optional<Instance> jamaOpt = jamaService.getJamaItem(id, "id"); //FIXME hack for now		
		if (jamaOpt.isPresent()) {
			Instance jama = jamaOpt.get();
			return new ServiceResponse(ServiceResponse.SUCCESS, service, "", jama.id().toString());
		} else
			return new ServiceResponse(ServiceResponse.UNKNOWN, service, "", null);
	}

	@Override
	public boolean isProviding(String artifactType) {
		return supportedTypes.contains(artifactType);
	}

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        for (JamaBaseElementType gbet : JamaBaseElementType.values()) {
        	gbet.getType(); //init this type so its known in designspace immediately upon starting
        	supportedTypes.add(gbet.getDesignSpaceShortTypeName());
        }
    }

}
