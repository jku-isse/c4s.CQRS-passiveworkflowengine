package at.jku.isse.passiveprocessengine.frontend.ui;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

@Route(value="processeditor", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Editor")
@UIScope
//@SpringComponent
public class BlocklyEditorView extends VerticalLayout  {

	
	public BlocklyEditorView() {
		
		setSizeFull();
		setMargin(false);
		setPadding(false);
		EditorPane editor = new EditorPane("/editor/index.html");
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
