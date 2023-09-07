package at.jku.isse.passiveprocessengine.frontend.ui.monitoring;

import at.jku.isse.designspace.artifactconnector.core.monitoring.ProgressEntry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.MainView;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RefreshableComponent;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;

import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@Route("progress")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Connector Progress Overview")
@UIScope
//@SpringComponent
public class GlobalProgressView extends VerticalLayout implements RefreshableComponent {
        

    protected RequestDelegate commandGateway;
	protected ProgressPusher pusher;
    //private Grid<ProgressEntry> grid = new Grid<ProgressEntry>();
    Grid<ProgressEntry> grid;
    TextField searchField;
    private ListDataProvider<ProgressEntry> dataProvider;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        dataProvider = pusher.add(attachEvent.getUI());
        addDataProviderToGrid();
        addDataProviderToSearchField();
        dataProvider.refreshAll();
    }
    
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        pusher.remove(detachEvent.getUI().getUIId());
    }
        
    public GlobalProgressView(RequestDelegate reqDel, ProgressPusher pusher, SecurityService securityService) {
    	this.commandGateway = reqDel;
    	this.pusher = pusher;
        setSizeFull();
        setMargin(false);
        setPadding(false);

//        HorizontalLayout header = new HorizontalLayout();
//        header.setClassName("header-theme");
//        header.setMargin(false);
//        header.setPadding(true);
//        header.setSizeFull();
//        header.setHeight("6%");
//        HorizontalLayout firstPart = new HorizontalLayout();
//        firstPart.setClassName("header-theme");
//        firstPart.setMargin(false);
//        firstPart.setPadding(true);
//        firstPart.setSizeFull();
//        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("Connector Progress Overview"));
//       
//        ToggleButton toggle = new ToggleButton("Refresher ");
//        toggle.setClassName("med");
//        toggle.addValueChangeListener(evt -> {
////            if (devMode) {
////                Notification.show("Development mode enabled! Additional features activated.");
////            }
//            content();
//        });
//        
//        header.add(firstPart, toggle);
//        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        AppHeader header = new AppHeader("Connector Progress Overview", securityService, commandGateway.getUIConfig());        
        AppFooter footer = new AppFooter(commandGateway.getUIConfig()); 

        add(
                header,
                main(),
                footer
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
    	refreshContent();
    	return pageContent;
    }
  
	@Override
	public void refreshContent() {		
        VerticalLayout cur = statePanel();
        cur.setHeight("100%");

        Div pages = new Div(cur); //, snap, split
        pages.setHeight("97%");
        pages.setWidthFull();

        pageContent.removeAll();
        pageContent.setClassName("layout-style");
        pageContent.add(/*tabs,*/ pages);
	}
  


    private VerticalLayout statePanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("big-text");
        layout.setMargin(false);
        layout.setHeight("50%");
        layout.setWidthFull();
        layout.setFlexGrow(0);
        searchField = createSearchField();
        grid = createProgressGrid();
       // if (pusher != null) {        	
        	layout.add(searchField);
        	layout.add(grid);        
       // }
        return layout;
    }
    
    private void addDataProviderToSearchField() {
    	searchField.addValueChangeListener(e -> dataProvider.refreshAll());

    	dataProvider.addFilter(pe -> {
    	    String searchTerm = searchField.getValue().trim();

    	    if (searchTerm.isEmpty())
    	        return true;

    	    boolean matchesSource = matchesTerm(pe.getSource(),         searchTerm);
    	    boolean matchesActivity = matchesTerm(pe.getActivity(), searchTerm);
    	   // boolean matchesStatus = matchesTerm(pe.getStatus().toString(), 	searchTerm);
    	    boolean matchesComment = matchesTerm(pe.getStatusComment(), 	searchTerm);

    	    return matchesSource || matchesActivity || matchesComment;
    	});    	    	
    }
    
    private TextField createSearchField() {    
    	searchField = new TextField();
    	searchField.setWidth("50%");
    	searchField.setPlaceholder("Search");
    	searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    	searchField.setValueChangeMode(ValueChangeMode.EAGER);
    	
    	return searchField;
    }
    
    private void addDataProviderToGrid() {
    	grid.setDataProvider(dataProvider);  
    }
    
    private  Grid<ProgressEntry>  createProgressGrid() {    	
    	grid = new Grid<ProgressEntry>();
    	grid.setColumnReorderingAllowed(false);
    	Grid.Column<ProgressEntry> tsColumn = grid.addColumn(p -> formatter.format(p.getTimestamp())).setHeader("Timestamp").setResizable(true).setSortable(true);
    	Grid.Column<ProgressEntry> sourceColumn = grid.addColumn(p -> p.getSource()).setHeader("Source").setResizable(true);
    	Grid.Column<ProgressEntry> activityColumn = grid.addColumn(p -> p.getActivity()).setHeader("Activity").setResizable(true);    	
    	Grid.Column<ProgressEntry> statusColumn = grid.addColumn(p -> p.getStatus()).setHeader("Status").setResizable(true);
    	Grid.Column<ProgressEntry> commentColumn = grid.addColumn(p -> p.getStatusComment()).setHeader("Comment").setResizable(true);    	    	    
    	  	
    	return grid;
    }
    
   
    private boolean matchesTerm(String value, String searchTerm) {
    	 if (searchTerm != null && !searchTerm.isEmpty() && value != null)
    		 return value.toLowerCase().contains(searchTerm.toLowerCase());
    	 else return false;
    }
    

   
}
