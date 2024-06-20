package at.jku.isse.passiveprocessengine.frontend.ui;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;

@Route(value="processeditor", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Editor")
@UIScope
//@SpringComponent
public class BlocklyEditorView extends VerticalLayout  {
	
	public BlocklyEditorView(RequestDelegate reqDel) {
		
	
		setSizeFull();
		setMargin(false);
		setPadding(false);
		String relUrlBase = ComponentUtils.getRelativeBaseUrl(); //FIXME running with a context-path other than '/' will cause the vaadin router to try to navigate instead of serving the static resource
		EditorPane editor = null;
		if (relUrlBase != null && relUrlBase.length() > 1) { // workaround for now
			editor = new EditorPane(reqDel.getUiConfig().getBlocklyEditorUrl());			
		} else {
			editor = new EditorPane(relUrlBase+"/editor/index.html");
		}
		editor.setSizeFull();
		this.add(editor);
	}

	
	@Tag(Tag.IFRAME)
	public static class EditorPane extends HtmlContainer {
		public EditorPane(String src) {
	        setSrc(src);
	    }
		
		public void setSrc(String src) {
	        set(srcDescriptor, src);
	    }
		
		private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
	            .attributeWithDefault("src", "");
	}
	
}
