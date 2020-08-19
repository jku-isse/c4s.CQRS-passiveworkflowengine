package impactassessment.neo4j;

import javax.inject.Inject;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import impactassessment.passiveprocessengine.workflowmodel.IdentifiableObject;

public abstract class Persistable<T extends IdentifiableObject> implements IPersistable<T> {

    protected int DEPTH_LIST = 0;
    protected int DEPTH_ENTITY = 1;
    
    @Inject 
    private SessionFactory neo4jFactory;
    private Session session;

    @Override
    public void invalidateSession() {    	
    	session.clear();    	
    	session = null;
    }
    
    protected Session getSession() {
    	if (session == null) {
    		session = neo4jFactory.openSession();
    	}
    	return session;
    }
    
    @Override
	public Iterable<T> findAll() {
        return getSession().loadAll(getEntityType(), DEPTH_LIST);
    }

    @Override
    public T find(String id) {
        return getSession().load(getEntityType(), id, DEPTH_ENTITY);
    }

    @Override
    public void delete(String id) {
    	T entity = getSession().load(getEntityType(), id);
    	if (entity != null)
    		getSession().delete(entity);
    }
    
    @Override
    public void delete(T entity) {
    	getSession().delete(entity);
    }

    @Override
    public void push(T entity) {
    	getSession().save(entity, DEPTH_ENTITY);
        //return find(entity.getId());
    	//return null;
    }
    
    @Override
    public T createOrUpdate(T entity) {
    	getSession().save(entity, DEPTH_ENTITY);
        return find(entity.getId());    	
    }

    abstract Class<T> getEntityType();
}