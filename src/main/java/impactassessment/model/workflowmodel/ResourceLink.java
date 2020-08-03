package impactassessment.model.workflowmodel;

import impactassessment.jiraartifact.IJiraArtifact;

public class ResourceLink extends AbstractArtifact implements Artifact{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7982907435567637199L;
	public String context;	
	public String href;
	public String rel;
	public String as;
	public String linkType;
	public String title;
	public transient Artifact parent;
	
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}
	public String getHref() {
		return this.href;
	}
	public void setHref(String href) {
		this.href = href;		
	}
	public String getRel() {
		return rel;
	}
	public void setRel(String rel) {
		this.rel = rel;
	}
	public String getAs() {
		return as;
	}
	public void setAs(String as) {
		this.as = as;
	}
	public String getLinkType() {
		return linkType;
	}
	public void setLinkType(String linkType) {
		this.linkType = linkType;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Deprecated
	public ResourceLink(){
		super();
	}
	
	public ResourceLink(String context, String href, String rel, String as, String linkType, String title) {
		super(href, new ArtifactType(context), null);
		//super.id = href;
		this.context = context;		
		this.href = href;
		this.rel = rel;
		this.as = as;
		this.linkType = linkType;
		this.title = title;
	}

	public static ResourceLink of(IJiraArtifact a) {
		return new ResourceLink(a.getSummary(), a.getBrowserLink().toString(), "self", a.getIssueType().getName(), "html", a.getKey());
	}

//	@Override
//	public ArtifactType getType() {
//		return new ArtifactType(this.context);
//	}
	@Override
	public Artifact getParentArtifact() {
		return parent;
	}
	
	public void setParentArtifact(Artifact parent) {
		this.parent = parent;
	}
	@Override
	public String getId() {
		return href;
	}
	@Override
	public String toString() {
		return "ResourceLink [" + title + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((href == null) ? 0 : href.hashCode());
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
		ResourceLink other = (ResourceLink) obj;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		return true;
	}
	
	
}
