package impactassessment.artifactconnector.demo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.IArtifactService;
import artifactapi.ResourceLink;

public abstract class DemoArtifact implements IArtifact {

	protected String id;
	protected ArtifactIdentifier ai;
	protected boolean removedAtOrigin = false;
	protected Map<String, String> properties = new HashMap<String,String>();
	protected Set<String> links = new LinkedHashSet<String>();
	protected DemoService ds;
	
	public DemoArtifact(DemoService ds, String id) {
		this.ds = ds;
		this.id = id;
	}
	
	@Override
	public ResourceLink convertToResourceLink() {
		return new ResourceLink("", "http://localhost/"+ai.getType()+"/"+ai.getId(), "", ai.getType(), "self", id);
	}

	@Override
	public void injectArtifactService(IArtifactService service) {
		if (service instanceof DemoService)
			this.ds = (DemoService) service;
	}

	@Override
	public IArtifact getParentArtifact() {
		return null;
	}

	@Override
	public void setRemovedAtOriginFlag() {
		removedAtOrigin = true;
	}

	@Override
	public boolean isRemovedAtOrigin() {
		return removedAtOrigin;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}
	
	public Map<String,String> getPropertyMap() { // for testing purposes
		return properties;
	}
	
	public void addLinkedArtifact(DemoArtifact art) {
		links.add(art.getArtifactIdentifier().getId());
	}
	
	public void removeLinkedArtifact(DemoArtifact art) {
		links.remove(art.getArtifactIdentifier().getId());		
	}
	
	public Set<DemoArtifact> getLinkedArtifacts() { //for testing purposes, this is editable/writable
		return links.stream().map(str -> ds.get(str)).filter(Objects::nonNull).collect(Collectors.toSet());
	}
}
