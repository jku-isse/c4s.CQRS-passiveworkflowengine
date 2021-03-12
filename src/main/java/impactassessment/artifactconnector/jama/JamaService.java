package impactassessment.artifactconnector.jama;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import c4s.jamaconnector.cache.CouchDBJamaCache;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.core.JamaDomainObject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.*;
import impactassessment.SpringUtil;
import impactassessment.artifactconnector.jama.subtypes.JamaProjectArtifact;
import impactassessment.artifactconnector.jama.subtypes.JamaUserArtifact;

import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class JamaService implements IJamaService {

    private static final String TYPE = IJamaArtifact.class.getSimpleName();

    private JamaInstance jamaInstance;
    private JamaChangeSubscriber jamaChangeSubscriber;
    private ConcurrentMap<String, JamaDataScope> perProcessCaches = new ConcurrentHashMap<>();

    public JamaService(JamaInstance jamaInstance, JamaChangeSubscriber jamaChangeSubscriber) {
        this.jamaInstance = jamaInstance;
        this.jamaChangeSubscriber = jamaChangeSubscriber;
    }

    @Override
    public boolean provides(String type) {
        return type.equals(TYPE);
    }

    
    // redirects to a workflow scope
    @Override
    public Optional<IArtifact> get(ArtifactIdentifier id, String workflowId) {
    	if (workflowId != null) {
    		//TODO: some method to purge scope entries when workflow is removed
    		IJamaService scope = perProcessCaches.computeIfAbsent(workflowId, k -> new JamaDataScope(k, this));
    		Optional<IJamaArtifact> opt = scope.get(Integer.parseInt(id.getId())); // no need to pass the workflow, as scope has that id;
    		return  opt.map(jArt -> (IArtifact)jArt);
    	}
    	else { // 
    		Optional<IJamaArtifact> opt = get(Integer.parseInt(id.getId())); // local passthrough to backend cache without change tracking
    		return  opt.map(jArt -> (IArtifact)jArt);
    	}
    }

    @Override
    public void injectArtifactService(IArtifact artifact, String workflowId) {
        IJamaService scope = perProcessCaches.computeIfAbsent(workflowId, k -> new JamaDataScope(k, this));
        jamaChangeSubscriber.addUsage(perProcessCaches.get(workflowId), new ArtifactIdentifier(((IJamaArtifact)artifact).getId()+"", IJamaArtifact.class.getSimpleName()));
        artifact.injectArtifactService(scope);
    }

    @Override
    public void deleteDataScope(String s) {
        perProcessCaches.remove(s);
    }

    @Override
	public Optional<IJamaArtifact> get(Integer id) {
		JamaItem jamaItem;
        try {
            jamaItem = jamaInstance.getItem(id);
        } catch (Exception e) { // FIXME
            log.error("Jama Item could not be retrieved: "+e.getClass().getSimpleName());
            return Optional.empty();
        }
        if (jamaItem != null) {
            return Optional.of(new JamaArtifact(jamaItem, this));
        } else {
            return Optional.empty();
        }
	}
    
	@Override
	public Optional<IJamaArtifact> get(Integer id, String workflowId) {
        JamaItem jamaItem;
        try {
            jamaItem = jamaInstance.getItem(id);
        } catch (Exception e) {
            log.error("Jama Item could not be retrieved: "+e.getClass().getSimpleName());
            e.printStackTrace();
            return Optional.empty();
        }
        if (jamaItem != null) {
            jamaChangeSubscriber.addUsage(perProcessCaches.get(workflowId), new ArtifactIdentifier(id+"", IJamaArtifact.class.getSimpleName()));
            IJamaService scope = perProcessCaches.get(workflowId);
            if (scope == null) scope = this;
            return Optional.of(new JamaArtifact(jamaItem, scope));
        } else {
            return Optional.empty();
        }
	}
    
	@Override
	public IJamaArtifact convert(JamaItem item) {
		return new JamaArtifact(item, this);
	}

	@Override
	public IJamaProjectArtifact convertProject(JamaProject proj) {
		return new JamaProjectArtifact(proj);
	}

	@Override
    public String getJamaServerUrl(JamaItem jamaItem) {
      return jamaInstance.getOpenUrl(jamaItem);
    }

    @Override
	public IJamaUserArtifact convertUser(JamaUser user) {
		return new JamaUserArtifact(user);
	}

}
