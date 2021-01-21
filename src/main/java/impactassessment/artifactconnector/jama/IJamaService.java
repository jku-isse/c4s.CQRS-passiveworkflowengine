package impactassessment.artifactconnector.jama;

import java.util.Optional;

import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaProject;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaUser;

import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;

public interface IJamaService{

	public Optional<IJamaArtifact> get(Integer id, String workflow);
	
	public Optional<IJamaArtifact> get(Integer id);
//	
//	public Optional<IJamaProjectArtifact> getProject(Integer id);
//	
//	public Optional<IJamaUserArtifact> getUser(Integer id);
	
	public IJamaArtifact convert(JamaItem item);
	
	public IJamaProjectArtifact convertProject(JamaProject proj);
	
	public IJamaUserArtifact convertUser(JamaUser user);

	public String getJamaServerUrl(JamaItem jamaItem);
	
}
