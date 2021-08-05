package impactassessment.artifactconnector.demo;

public class Basic1Artifacts {

	public static DemoRequirement req1;
	public static DemoRequirement req2;
	public static DemoRequirement req3;
	public static DemoRequirement req4;
	
	private static void initArt(DemoService ds) {
		ds.deleteDataScope("");
		req1 = new DemoRequirement(ds, "Req1");
		req2 = new DemoRequirement(ds, "Req2");
		req3 = new DemoRequirement(ds, "Req3");
		req4 = new DemoRequirement(ds, "Req4");
		ds.addArtifact(req1);
		ds.addArtifact(req2);
		ds.addArtifact(req3);
		ds.addArtifact(req4);
	}
	
	public static void initServiceWithReq(DemoService ds) {
		initArt(ds);
		req1.addLinkedArtifact(req4);
		req1.addLinkedArtifact(req2);
		req2.addLinkedArtifact(req3);
		req4.addLinkedArtifact(req1);
	}
	
}
