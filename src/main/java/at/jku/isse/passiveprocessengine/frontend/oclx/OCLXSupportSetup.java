package at.jku.isse.passiveprocessengine.frontend.oclx;

import org.eclipse.xtext.util.Modules2;

import com.google.inject.Guice;
import com.google.inject.Injector;

import at.jku.isse.OCLXStandaloneSetup;
import at.jku.isse.ide.OCLXIdeModule;
import at.jku.isse.passiveprocessengine.core.NodeToDomainResolver;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OCLXSupportSetup extends OCLXStandaloneSetup {
	
	private final NodeToDomainResolver designspace;
	
	//private Injector injector;
	
	@Override
	public Injector createInjector() {		
		 return Guice.createInjector(Modules2.mixin(new OCLXSupportModule(designspace),  new OCLXIdeModule()));		 
	}
	
	
	
}
