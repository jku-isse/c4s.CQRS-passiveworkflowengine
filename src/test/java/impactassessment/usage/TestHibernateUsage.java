package impactassessment.usage;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;

import artifactapi.ArtifactIdentifier;
import impactassessment.artifactconnector.usage.ConnectionBuilder;
import impactassessment.artifactconnector.usage.HibernatePerProcessArtifactUsagePersistor;

public class TestHibernateUsage {

	HibernatePerProcessArtifactUsagePersistor usage;
	
	@Before
	public void setUp() throws Exception {
		Properties props = new Properties(); 
		props.load(new FileInputStream("application.properties"));
		SessionFactory sf = ConnectionBuilder.createConnection(
				props.getProperty("mysqlDBuser"),
				props.getProperty("mysqlDBpassword"),
				props.getProperty("mysqlURL")+"test"
				);
		usage = new HibernatePerProcessArtifactUsagePersistor(sf);		
	}
	
	@Test
	public void testInsertAndRetrieve() {
		String testscope1 = "testscope1";
		String testscope2 = "testscope2";
		ArtifactIdentifier ai1 = new ArtifactIdentifier("testId1", "testtype");
		ArtifactIdentifier ai2 = new ArtifactIdentifier("testId2", "testtype");
		ArtifactIdentifier ai3 = new ArtifactIdentifier("testId3", "testtype");
		usage.addUsage(testscope1, ai1);
		assert(usage.getUsages(testscope1).contains(ai1));
		
		usage.addUsage(testscope1, ai2);
		usage.addUsage(testscope2, ai1);
		usage.addUsage(testscope2, ai3);		
		//test duplicate entry avoided
		usage.addUsage(testscope1, ai2);
		usage.addUsage(testscope2, ai1);
		usage.addUsage(testscope2, ai3);
		
		Set<ArtifactIdentifier> usage1 = usage.getUsages(testscope1); 
		Set<ArtifactIdentifier> usage2 = usage.getUsages(testscope2);
		assert(usage.getUsages(testscope1).contains(ai2));
		assert(!usage.getUsages(testscope2).contains(ai2));
		
		usage.removeScope(testscope1);
		usage.removeScope((testscope2));
		
		assert(!usage.getUsages(testscope1).contains(ai1));
		assert(!usage.getUsages(testscope1).contains(ai2));
		assert(!usage.getUsages(testscope2).contains(ai3));
	}
	
}
