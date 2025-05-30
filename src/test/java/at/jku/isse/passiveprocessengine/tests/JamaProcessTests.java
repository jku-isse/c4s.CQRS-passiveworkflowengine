package at.jku.isse.passiveprocessengine.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class JamaProcessTests {


	
	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ProcessRegistry procReg;
	
	@Autowired
	ArtifactResolver artRes;
	
	
	@Autowired
	ProcessQAStatsMonitor qastats;
	
	@BeforeEach
	void setUp() throws Exception {
	}

//	@Test
//	void testLazyLoadingWithJama() throws ProcessException {
//		// self.in_subwp.any().asType( <root/types/jama/jama_item> ).downstream->select( srs_2 : <root/types/jama/jama_item> | srs_2.typeKey = 'SRS').asSet()->forAll( srs : <root/types/jama/SRS> | srs.status = 'Released')
//		ArtifactIdentifier jamaAI = new ArtifactIdentifier("10269113", "jama_item", JamaIdentifiers.JamaItemId.toString());
//		//ArtifactIdentifier jamaAI = new ArtifactIdentifier("15079383", "jama_item", JamaIdentifiers.JamaItemId.toString());
//		
//		ProcessInstance proc = reqDelegate.instantiateProcess("TestProc", Map.of("subwp" , jamaAI), "SubWP-frq");
//		
//		//ProcessInstance proc = reqDelegate.getProcess("SubWP-frq_[[FEAT] [SUB-WP] RedBlack - Radio Domain Switching & R/B Radio Handling - OP Part / RBC Domain Switchover (Draft)]");
//		TestUtils.assertAllConstraintsAreValid(proc);
//		TestUtils.printFullProcessToLog(proc);
//	}


	
}
