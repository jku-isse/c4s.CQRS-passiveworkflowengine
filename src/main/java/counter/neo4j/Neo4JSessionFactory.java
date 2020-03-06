package counter.neo4j;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class Neo4JSessionFactory {
	    
    private static SessionFactory sessionFactory;
    //private static Neo4JSessionFactory factory = new Neo4JSessionFactory();

//    public static Neo4JSessionFactory getInstance() { // INSTANCE PROVIDED VIA DEPENDENCY INJECTION 
//        return factory;
//    }

    // prevent external instantiation
    protected Neo4JSessionFactory(Configuration config) {
    	if (sessionFactory == null) {
    		
    	}
    }

    public Session getNeo4jSession() {
        return sessionFactory.openSession();
    }
}