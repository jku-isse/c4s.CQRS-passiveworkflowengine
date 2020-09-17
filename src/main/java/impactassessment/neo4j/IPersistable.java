package impactassessment.neo4j;

import impactassessment.passiveprocessengine.definition.AbstractIdentifiableObject;

public interface IPersistable<T extends AbstractIdentifiableObject> {

	Iterable<T> findAll();

	T find(String id);

	void delete(String id);

	//T createOrUpdate(T object);

	void push(T entity);
	
	void delete(T entity);

	T createOrUpdate(T entity);

	void invalidateSession();
}

