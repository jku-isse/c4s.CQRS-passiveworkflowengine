package at.jku.isse.passiveprocessengine.frontend.oclx;

import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2;
import org.eclipse.xtext.nodemodel.impl.InvariantChecker;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;
import com.google.inject.Provider;

import at.jku.isse.ide.assistance.CodeActionExecuter;

import com.google.inject.Inject;


public class CodeActionExecuterProvider {

	@Inject	private Provider<XtextResourceSet> resourceSetProvider;
	@Inject private IResourceFactory resourceFactory;
	@Inject private InvariantChecker invariantChecker;
	@Inject private ICodeActionService2 repairService;
	
	
	public CodeActionExecuter buildExecuter(String constraint) {
		return new  CodeActionExecuter(constraint, resourceSetProvider, resourceFactory, invariantChecker, repairService);
	}
}
