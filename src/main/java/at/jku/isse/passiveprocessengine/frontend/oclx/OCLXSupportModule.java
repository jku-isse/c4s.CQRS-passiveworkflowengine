package at.jku.isse.passiveprocessengine.frontend.oclx;

import at.jku.isse.AbstractOCLXRuntimeModule;
import com.google.inject.Binder;

import at.jku.isse.validation.MethodRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;

public class OCLXSupportModule extends AbstractOCLXRuntimeModule {

	DesignSpaceSchemaRegistry designspace;
	
	public OCLXSupportModule(DesignSpaceSchemaRegistry designspace) {
		this.designspace = designspace;
	}
	
	public Class<? extends MethodRegistry> bindMethodRegistry() {
		return MethodRegistry.class;
	}
	
	@Override
	public void configure(Binder binder) {
		super.configure(binder);
		binder.bind(SchemaRegistry.class).toInstance(designspace);
	}
}
