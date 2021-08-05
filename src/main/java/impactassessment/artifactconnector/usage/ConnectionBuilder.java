package impactassessment.artifactconnector.usage;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

public class ConnectionBuilder {
	
	public static SessionFactory createConnection(String user, String password, String url) {
		Properties properties = new Properties();
		properties.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
		properties.setProperty(Environment.HBM2DDL_AUTO,"update");
		properties.setProperty(Environment.DRIVER, "com.mysql.jdbc.Driver");
		properties.setProperty(Environment.USER, user);
		properties.setProperty(Environment.PASS, password);
		properties.setProperty(Environment.URL, url);
		
		Configuration cfg = new Configuration();
		cfg.setProperties(properties);
		cfg.addAnnotatedClass(UsageDTO.class);
		
		SessionFactory factory = cfg.buildSessionFactory();
		return factory;
	}
}
