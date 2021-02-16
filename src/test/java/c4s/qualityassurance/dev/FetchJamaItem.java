package c4s.qualityassurance.dev;

import com.google.inject.Injector;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItem;

import c4s.jamaconnector.JamaUtils;
import impactassessment.DevelopmentConfig;

public class FetchJamaItem {
	
	public static void main(String[] args) {
		Injector injector = DevelopmentConfig.getInjector();
		JamaInstance ji = injector.getInstance(JamaInstance.class);
		try {
			JamaItem item = ji.getItem(9710969);
			System.out.println(JamaUtils.getJamaItemDetails(item));
		} catch (RestClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
