package impactassessment.artifactconnector.usage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import artifactapi.ArtifactIdentifier;
import c4s.jiralightconnector.hibernate.JiraIssueDTO;
import impactassessment.artifactconnector.jira.JiraChangeSubscriber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HibernatePerProcessArtifactUsagePersistor implements PerProcessArtifactUsagePersistor{
	
	private Session session;
	private SessionFactory sessionFactory;
	
	public HibernatePerProcessArtifactUsagePersistor(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private Session getSession() {
		if (this.session == null || !this.session.isOpen()) {
			this.session = sessionFactory.openSession();
		}
		return this.session;
	}
	
	
	@Override
	
	public synchronized void addUsage(String projectScopeId, ArtifactIdentifier ai) {
		Transaction tx = getSession().beginTransaction();
		UsageDTO dto = null;
		dto = getSession().get(UsageDTO.class, projectScopeId);
		if (dto != null) {
			if (!dto.getUsages().contains(ai)) {
				dto.getUsages().add(ai);
				getSession().save(dto);
				log.debug("Workflow: {} has following usages: {}", projectScopeId, dto.getUsages().stream().map(id -> id.getId()).collect(Collectors.joining( ", " )));
			}
		} else {				
				dto = new UsageDTO(projectScopeId);
				dto.getUsages().add(ai);
				getSession().persist(dto);
		}
		getSession().flush();
		tx.commit();
	}

	@Override
	public Set<ArtifactIdentifier> getUsages(String projectScopeId) {
		try {
			UsageDTO dto = getSession().get(UsageDTO.class, projectScopeId);
			if (dto == null) return Collections.emptySet();
			else {
				return dto.getUsages();
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
			return Collections.emptySet();
		}
		
	}

	@Override
	public synchronized void removeScope(String projectScopeId) {
		Transaction tx = getSession().beginTransaction();		
		try {
			UsageDTO dto =  getSession().load(UsageDTO.class, projectScopeId);
			if (dto != null) {
				getSession().delete(dto);
				log.info("Removed scope "+projectScopeId );
			}
		} catch (ObjectNotFoundException e) {
			//then nothing to remove
		}
		tx.commit();
	}

	@Override
	public Set<String> getAllScopeIdentifier() {
		Query<String> query  = getSession().createQuery("select e.projectScopeId from ArtUsage e");
		List<String> list = query.list();
		return new HashSet<String>(list);
	}

}
