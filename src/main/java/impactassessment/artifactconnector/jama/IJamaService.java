package impactassessment.artifactconnector.jama;

import java.util.Collection;
import java.util.Optional;

import artifactapi.IArtifactService;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.*;

import artifactapi.jama.IJamaArtifact;
import artifactapi.jama.subtypes.IJamaProjectArtifact;
import artifactapi.jama.subtypes.IJamaUserArtifact;

public interface IJamaService extends IArtifactService {

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
