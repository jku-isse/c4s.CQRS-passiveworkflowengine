package at.jku.isse.passiveprocessengine.frontend.ui.monitoring;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.monitoring.ProgressEntry;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.frontend.ui.AppView;
import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@Route(value="progress", layout = AppView.class)
@PageTitle("Connector Progress Overview")
@UIScope
//@SpringComponent
public class GlobalProgressView extends VerticalLayout {
        

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
        
    public GlobalProgressView(RequestDelegate reqDel, ProgressPusher pusher) {
    	this.commandGateway = reqDel;
    	this.pusher = pusher;
//        setSizeFull();
        setMargin(false);
       // setPadding(false);
        Details loadArtifact = new Details("Fetch Artifact From Tool", fetchArtifact(reqDel.getArtifactResolver()));
        loadArtifact.setOpened(false);
        loadArtifact.addThemeVariants(DetailsVariant.FILLED);
        this.setAlignSelf(Alignment.STRETCH, loadArtifact);
        
        searchField = createSearchField();
        grid = createProgressGrid();
        add(loadArtifact, searchField, grid);
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
    	grid.setHeightByRows(true);
    	return grid;
    }
    
   
    private boolean matchesTerm(String value, String searchTerm) {
    	 if (searchTerm != null && !searchTerm.isEmpty() && value != null)
    		 return value.toLowerCase().contains(searchTerm.toLowerCase());
    	 else return false;
    }
    
    private Component fetchArtifact(ArtifactResolver artRes) {
    	VerticalLayout layout = new VerticalLayout();        
        TextField artIdField = new TextField();
    	ComboBox<InstanceType> artTypeBox = new ComboBox<>("Instance Type");
    	Set<InstanceType> instTypes = commandGateway.getArtifactResolver().getAvailableInstanceTypes();
    	//List<String> instTypes = List.of("git_issue", "azure_workitem", "jira_core_artifact", "jama_item");
    	artTypeBox.setItems(instTypes);
    	artTypeBox.setItemLabelGenerator(new ItemLabelGenerator<InstanceType>() {
			@Override
			public String apply(InstanceType item) {
				return item.name();
			}});
    	ComboBox<String> idTypeBox = new ComboBox<>("Identifier Type");    	  
    	
		
    	
    	artTypeBox.addValueChangeListener(e-> {
    		InstanceType artT = artTypeBox.getOptionalValue().get();
    		List<String> idTypes = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(artT);
    		idTypeBox.setItems(idTypes);
    		idTypeBox.setValue(idTypes.get(0));
    	});
    	
        Icon icon = new Icon(VaadinIcon.CLOUD_DOWNLOAD);
        icon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
    	
    	Button importArtifactButton = new Button("Fetch", icon, evt -> {
            
                try {
                	if (artTypeBox.getOptionalValue().isEmpty())
            			Notification.show("Make sure to select an Artifact Type!");
                	else if (artIdField.getValue().length() < 1)
                		Notification.show("Make sure to provide an identifier!");
                	else {
                		String idValue = artIdField.getValue().trim();
                		String artType = artTypeBox.getOptionalValue().get().name();
                		String idType = idTypeBox.getOptionalValue().get();
                		
                		ArtifactIdentifier ai = new ArtifactIdentifier(idValue, artType, idType);
                		boolean forceRefetch = true;
                		Instance inst = artRes.get(ai, forceRefetch);
                		if (inst != null) {
                			// redirect to new page:
                			UI.getCurrent().getPage().open("instance/"+inst.id(), "_blank");
                		//	UI.getCurrent().navigate("instance/show", new QueryParameters(Map.of("id", List.of(inst.id().toString()))));
                		}
                	}
                } catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
                    log.error("Artifact Fetching Exception: " + e.getMessage());
                   // e.printStackTrace();
                    Notification.show("Fetching failed! \r\n"+e.getMessage());
                }
        });
        importArtifactButton.addClickShortcut(Key.ENTER).listenOn(layout);
        artTypeBox.setWidth("500px");
        artTypeBox.setMinWidth("200px");
        idTypeBox.setWidth("500px");
        idTypeBox.setMinWidth("200px");
        artIdField.setWidth("500px");
        artIdField.setMinWidth("200px");
        importArtifactButton.setWidth("500px");
        importArtifactButton.setMinWidth("200px");
        layout.add(artTypeBox, idTypeBox, artIdField, importArtifactButton);
        return layout;
    
    }
   
}
