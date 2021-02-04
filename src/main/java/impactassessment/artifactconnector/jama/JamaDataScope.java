package impactassessment.artifactconnector.jama;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Optional;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JamaDataScope implements IJamaService {


	private String scopeId;
	private IJamaService origin;
	private HashMap<String, WeakReference<Object>> cache = new HashMap<String, WeakReference<Object>>();
	
	public JamaDataScope(String scopeId, IJamaService origin) {
		this.scopeId = scopeId;
		this.origin = origin;
	}
	
	public String getScopeId() {
		return scopeId;
	}


	public IJamaService getOrigin() {
		return origin;
	}

	public void setOrigin(IJamaService origin) {
		this.origin = origin;
	}

	
	public IJamaArtifact replaceWithUpdate(JamaItem item) {
		IJamaArtifact art = new JamaArtifact(item, this); //to have ref to this scope
		//((JamaArtifact) art).setArtifactRegistry(this); //ugly cast - no longer necessary
		cache.put("JamaItem"+item.getId(), new WeakReference<Object>(art));
		return art;
	}
	
	public IJamaArtifact convert(JamaItem item) {
		WeakReference<Object> ref = cache.getOrDefault("JamaItem"+item.getId(), new WeakReference<Object>(null));
		if (ref.get() == null) {
			IJamaArtifact opt = new JamaArtifact(item, this); //to have ref to this scope
			((JamaArtifact) opt).setJamaService(this);
			cache.put("JamaItem"+item.getId(), new WeakReference<Object>(opt));
			return opt;
		} else {
			return (IJamaArtifact)ref.get();
		}
	}

	public IJamaProjectArtifact convertProject(JamaProject proj) {
		WeakReference<Object> ref = cache.getOrDefault("JamaProject"+proj.getId(), new WeakReference<Object>(null));
		if (ref.get() == null) {
			IJamaProjectArtifact opt = origin.convertProject(proj);
			cache.put("JamaProject"+proj.getId(), new WeakReference<Object>(opt));
			return opt;
		} else {
			return (IJamaProjectArtifact)ref.get();
		}
	}

	public IJamaUserArtifact convertUser(JamaUser user) {
		WeakReference<Object> ref = cache.getOrDefault("JamaUser"+user.getId(), new WeakReference<Object>(null));
		if (ref.get() == null) {
			IJamaUserArtifact opt = origin.convertUser(user);
			cache.put("UserProject"+user.getId(), new WeakReference<Object>(opt));
			return opt;
			
		} else {
			return (IJamaUserArtifact)ref.get();
		}
	}

	@Override
	public String getJamaServerUrl(JamaItem jamaItem) {
		return origin.getJamaServerUrl(jamaItem);
	}

	@Override
	public Optional<IJamaArtifact> get(Integer id, String workflow) {
		WeakReference<Object> ref = cache.getOrDefault("JamaItem"+id, new WeakReference<Object>(null));
		if (ref.get() == null) {
			Optional<IJamaArtifact> opt = origin.get(id, workflow);
			if (opt.isPresent()) {
				cache.put("JamaItem"+id, new WeakReference<Object>(opt.get()));
				return opt;
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.ofNullable((IJamaArtifact)ref.get());
		}
	}

	@Override
	public Optional<IJamaArtifact> get(Integer id) {
		return get(id, scopeId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scopeId == null) ? 0 : scopeId.hashCode());
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
		JamaDataScope other = (JamaDataScope) obj;
		if (scopeId == null) {
			if (other.scopeId != null)
				return false;
		} else if (!scopeId.equals(other.scopeId))
			return false;
		return true;
	}


	@Override
	public boolean provides(String s) {
		return origin.provides(s);
	}

	@Override
	public Optional<IArtifact> get(ArtifactIdentifier artifactIdentifier, String s) {
		return origin.get(artifactIdentifier, s);
	}

	@Override
	public void injectArtifactService(IArtifact iArtifact, String s) {
		if (scopeId.equals(s)) {
			iArtifact.injectArtifactService(this);
		} else {
			log.warn("Coudn't inject this DataScope because scope id ({}) doesn't match workflow id ({})", scopeId, s);
		}
	}
}
