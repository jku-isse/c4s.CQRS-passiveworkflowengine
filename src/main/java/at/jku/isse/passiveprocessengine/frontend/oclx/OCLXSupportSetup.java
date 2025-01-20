package at.jku.isse.passiveprocessengine.frontend.oclx;

import com.google.inject.Guice;
import com.google.inject.Injector;

import at.jku.isse.OCLXStandaloneSetup;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OCLXSupportSetup extends OCLXStandaloneSetup {
	
	private final DesignSpaceSchemaRegistry designspace;
	
	private Injector injector;
	
	@Override
	public Injector createInjector() {
		 if (injector == null)
			 injector = Guice.createInjector(new OCLXSupportModule(designspace));
		 return injector;
	}
	
	
	
}
