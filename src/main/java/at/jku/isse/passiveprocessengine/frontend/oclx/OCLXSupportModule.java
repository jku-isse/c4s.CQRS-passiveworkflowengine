package at.jku.isse.passiveprocessengine.frontend.oclx;

import at.jku.isse.ide.AbstractOCLXIdeModule;
import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2;

import com.google.inject.Binder;

import at.jku.isse.ide.assistance.MethodRegistry;
import at.jku.isse.ide.assistance.QuickFixCodeActionService;
import at.jku.isse.ide.assistance.TypeExtractor;
import at.jku.isse.ide.assistance.TypeHoverService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;

public class OCLXSupportModule extends AbstractOCLXIdeModule {

	DesignSpaceSchemaRegistry designspace;
	
	public OCLXSupportModule(DesignSpaceSchemaRegistry designspace) {
		this.designspace = designspace;
	}
	
	public Class<? extends ICodeActionService2> bindCodeActionService2() {
		return QuickFixCodeActionService.class;
	}
	
	
//	public Class<? extends IdeContentProposalProvider> bindIdeContentProposalProvider() {
//		return OclxContentProposalProvider.class;
//	}
	
	public Class<TypeExtractor> bindTypeExtractor() {
		return TypeExtractor.class;
	}
	
	public Class<? extends TypeHoverService> bindHoverService() {
		return TypeHoverService.class;
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
