package at.jku.isse.passiveprocessengine.frontend.ui.connectors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.model.BaseElementType;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.jama.model.JamaBaseElementType;
import at.jku.isse.designspace.jama.service.IJamaService.JamaIdentifiers;
import at.jku.isse.designspace.jama.service.JamaService;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Route("jamaconnector")
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Jama Connector")
//@SpringComponent
@UIScope
public class JamaConnectorView extends VerticalLayout  /*implements PageConfigurator*/ {


    
    protected JamaService jamaService;

    @Inject
    private UIConfig conf;
    
    @Inject
    public void setCommandGateway(JamaService jamaService) {
        this.jamaService = jamaService;
    }

    
    public JamaConnectorView(UIConfig conf) {
    	this.conf = conf;
    	setSizeFull();
        setMargin(false);
        setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setClassName("header-theme");
        header.setMargin(false);
        header.setPadding(true);
        header.setSizeFull();
        header.setHeight("6%");
        HorizontalLayout firstPart = new HorizontalLayout();
        firstPart.setClassName("header-theme");
        firstPart.setMargin(false);
        firstPart.setPadding(true);
        firstPart.setSizeFull();
        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("Jama Connector"));
       


        header.add(firstPart/*, shutdown*/);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);


        add(
                header,
                main()

        );
    }

    private Component main() {
        HorizontalLayout main = new HorizontalLayout();
        main.setClassName("layout-style");
        main.setHeight("91%");
        main.add( content());
        return main;
    }

    VerticalLayout pageContent = new VerticalLayout();
    
    private Component content() {
       // Tab tab1 = new Tab("Current State");
        VerticalLayout cur = statePanel();
        cur.setHeight("100%");

        Div pages = new Div(cur); //, snap, split
        pages.setHeight("97%");
        pages.setWidthFull();

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(/*tabs,*/ pages);
        return pageContent;
    }
  


    private VerticalLayout statePanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
        if (jamaService != null) {
        	Set<Instance> jamaInstances = jamaService.getAllKnownJamaItems()
        			.stream()
        			.filter(inst -> (Boolean)inst.getPropertyAsValue(BaseElementType.FULLY_FETCHED)==true )
        			.collect(Collectors.toSet());
        	layout.add(instancesAsList(jamaInstances));        	        	
        }
        return layout;
    }
   
//    private Component fetchArtifact(JamaService js) {
//    	VerticalLayout layout = new VerticalLayout();
//        Paragraph p1 = new Paragraph("Fetch Jama Item:");
//        TextField artIdField = new TextField();
//    	ComboBox<JamaIdentifiers> artTypeBox = new ComboBox<>("Identifier Type");    	
//    	artTypeBox.setItems(JamaIdentifiers.values());    			
//    	
//    	Button importArtifactButton = new Button("Fetch", evt -> {
//            
//                try {
//                	if (artTypeBox.getOptionalValue().isEmpty())
//            			Notification.show("Make sure to select an Identifier Type!");
//                	else if (artIdField.getValue().length() < 1)
//                		Notification.show("Make sure to provide an identifier!");
//                	else {
//                		String idValue = artIdField.getValue().trim();
//                		JamaIdentifiers artType = artTypeBox.getOptionalValue().get();                		
//                		List<Instance> insts = js.forceRefetch(idValue, artType);
//                		                		
//                		if (insts != null && insts.size() > 0) {
//                			// redirect to new page:
//                			UI.getCurrent().navigate("instance/show", new QueryParameters(Map.of("id", List.of(insts.get(0).id().toString()))));
//                		}
//                	}
//                } catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
//                    log.error("Artifact Fetching Exception: " + e.getMessage());
//                    e.printStackTrace();
//                    Notification.show("Fetching failed! \r\n"+e.getMessage());
//                }
//        });
//        importArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);
//        layout.add(p1, artTypeBox, artIdField, importArtifactButton);
//        return layout;
//    
//    }
    
    private Component instancesAsList(Set<Instance> jamaItems) {
    	
    	Grid<Instance> grid = new Grid<Instance>();
    	grid.setColumnReorderingAllowed(true);
    	Grid.Column<Instance> idColumn = grid.addColumn(inst -> inst.getPropertyAsValueOrElse("id", () -> "unknown id")).setHeader("Id").setResizable(true).setSortable(true);
    	Grid.Column<Instance> keyColumn = grid.addColumn(inst -> inst.getPropertyAsValueOrElse(BaseElementType.KEY, () -> "unknown document key")).setHeader("DocKey").setResizable(true).setSortable(true);
    	Grid.Column<Instance> nameColumn = grid.addColumn(inst -> inst.name()).setHeader("Name").setResizable(true).setSortable(true);
    	Grid.Column<Instance> typeColumn = grid.addColumn(inst -> inst.getPropertyAsValueOrElse(JamaBaseElementType.ITEM_TYPE_SHORT, () -> "unknown item type")).setHeader("Type").setResizable(true).setSortable(true);    	
    	Grid.Column<Instance> statusColumn = grid.addColumn(inst -> inst.getPropertyAsValueOrElse(JamaBaseElementType.STATUS, () -> "unknown status")).setHeader("Status").setResizable(true).setSortable(true);
        
    	ListDataProvider<Instance> dataProvider = new ListDataProvider<Instance>(jamaItems); 
    	grid.setDataProvider(dataProvider);
    	InstanceContextMenu ctxMenu = new InstanceContextMenu(grid, jamaService);
    	
    	TextField searchField = new TextField();
    	searchField.setWidth("50%");
    	searchField.setPlaceholder("Search");
    	searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    	searchField.setValueChangeMode(ValueChangeMode.EAGER);
    	searchField.addValueChangeListener(e -> dataProvider.refreshAll());
    	
    	dataProvider.addFilter(inst -> {
    		String searchTerm = searchField.getValue().trim();
    		 if (searchTerm.isEmpty())
    		        return true;
    		 
    		 if (matchesTerm(inst.name(), searchTerm))
    			 return true;
    		 if (matchesTerm((String)inst.getPropertyAsValueOrElse(BaseElementType.KEY, () -> "unknown document key"), searchTerm))
    			 return true;
    		 if (matchesTerm((String)inst.getPropertyAsValueOrElse(JamaBaseElementType.STATUS, () -> "unknown status key"), searchTerm))
    			 return true;
    		 return false;
    	});
    	
    	 VerticalLayout layout = new VerticalLayout(searchField, grid);
         layout.setPadding(false);
    	
    	return layout;
    }
    
    private boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }
    
    private class InstanceContextMenu extends GridContextMenu<Instance> {
    	public InstanceContextMenu(Grid<Instance> target, JamaService js) {
    		super(target);
    		
            addItem("Refetch", e -> e.getItem().ifPresent(instance -> {
            	js.forceRefetch((String) instance.getPropertyAsValue("id"), JamaIdentifiers.JamaItemId);
            }));
            if (!conf.isAnonymized())        
            addItem("Internal Details", e -> e.getItem().ifPresent(instance -> {
            	UI.getCurrent().navigate("instance/show", new QueryParameters(Map.of("id", List.of(instance.id().toString()))));
            }));
            	  
    	}
    }
}
