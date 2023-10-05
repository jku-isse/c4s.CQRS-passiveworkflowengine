package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.CollectionProperty;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="instance", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Artifact/Instance Inspector")
@UIScope
//@SpringComponent
public class InstanceView extends VerticalLayout implements HasUrlParameter<String> {

	private RequestDelegate commandGateway;

	private Id id = null;
	private ListDataProvider<Property> dataProvider = null;

	@Override
	public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
		// example link: http://localhost:8080/home/?key=DEMO-9&value=Task
		Location location = beforeEvent.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();

		//Map<String, List<String>> parametersMap = queryParameters.getParameters();
		//String strid = parametersMap.getOrDefault("id", List.of("")).get(0);
		String strid = s;
		if (commandGateway.getUIConfig().isAnonymized()) {  
			id = null;
		}
		if (strid != null) {
			try {
				long lId = Long.parseLong(strid);
				id = Id.of(lId);			
			} catch(Exception e) {
				log.warn("Parameter id cannot be parsed to long");
			}
		}
		statePanel();

	}


	public InstanceView(RequestDelegate commandGateway, SecurityService securityService) {
		this.commandGateway = commandGateway;
		setSizeFull();
		setMargin(false);
		//setPadding(false);

	}

	private VerticalLayout statePanel() {
		VerticalLayout layout = this; // new VerticalLayout();
		layout.setMargin(false);
		layout.setWidthFull();

		if (commandGateway.getUIConfig().isAnonymized())  {
			layout.add(new Paragraph("This view is not available in double blind reviewing mode"));
		} else       		    	    {
			layout.add(createFetchField());
			if (commandGateway != null && commandGateway.getWorkspace() != null && id != null) {
				layout.add(new Hr());
				HorizontalLayout hl = new HorizontalLayout();
				Element el = commandGateway.getWorkspace().findElement(id);
				if (el == null) { 
					layout.add(new Paragraph("Element with id "+id+" does not exists."));
					return layout;				
				}
				Paragraph elName = new Paragraph("Artifact/Instance (DSid="+id+"): "+el.name());
				if (el.isDeleted) 
					elName.getStyle().set("background-color", "#ffbf00");
				hl.add(elName);
				Component grid = null;
				if (el instanceof Instance) {        		
					Instance inst = (Instance) el;

					if (!isFullyFetched(inst))
						hl.add(addButtonToFullyFetchLazyLoadedInstance(inst));
					else if (inst.hasProperty("fullyFetched"))
						hl.add(getReloadIcon(inst));
					grid = instanceAsList(inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()));
				} else if (el instanceof InstanceType) {
					grid = instanceAsList(((InstanceType) el).getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()));
				}								
				layout.add(hl);
				layout.add(createSearchField());
				layout.add(grid);				
			}
		}
		return layout;
	}

	private static Supplier<Boolean> falseSupplier = () -> Boolean.FALSE;

	public static boolean isFullyFetched(Instance element) {
		if ( element.hasProperty("fullyFetched") 
				&& (      element.getPropertyAsValueOrElse("fullyFetched", falseSupplier) == null 
				|| ((Boolean)element.getPropertyAsValueOrElse("fullyFetched", falseSupplier)) == false     ))
		{
			return false;
		} else
			return true;
	}

	private Component addButtonToFullyFetchLazyLoadedInstance(Instance inst) {
		Button fetchButton = new Button("Fetch Properties", evt -> {
			Notification.show("Fetching properties via connector, this might take some time.");
			new Thread(() -> { 
				try {
					commandGateway.getArtifactResolver().get(getArtifactIdentifier(inst), true);
					this.getUI().get().access(() ->Notification.show("Fetching Success"));
					this.getUI().get().access(()-> UI.getCurrent().getPage().reload());
				} catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
					log.error("Artifact fetching failed: " + e.getMessage());
					e.printStackTrace();
					this.getUI().get().access(() ->Notification.show("Fetching failed! \r\n"+e.getMessage()));
				}
			} ).start();
		});
		return fetchButton;
	}

	private ArtifactIdentifier getArtifactIdentifier(Instance inst) {
		// less brittle, but requires consistent use by artifact connectors
		String artId = (String) inst.getPropertyAsValue("id");
		InstanceType instType = inst.getInstanceType();
		List<String> idOptions = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(instType);
		if (idOptions.isEmpty()) {
			log.warn("Cannot determine identifier option for instance type: "+instType.name());
			return new ArtifactIdentifier(artId, instType.name());
		} 
		String idType = idOptions.get(0);				
		return new ArtifactIdentifier(artId, instType.name(), idType);
	}

	private Component createFetchField() {
		TextField fetchField = new TextField();
		fetchField.setWidth("50%");
		fetchField.setPlaceholder("Find Instance by Id");
		fetchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		fetchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		fetchField.addValueChangeListener(e -> {
			String deepLinkingUrl = RouteConfiguration.forSessionScope()
					.getUrl(getClass(), fetchField.getValue());
			getUI().get().getPage().getHistory()
			.replaceState(null, deepLinkingUrl);
			this.getUI().get().access(()-> UI.getCurrent().getPage().reload());
		});
		return fetchField;
	}

	private Component createSearchField() {    
		TextField searchField = new TextField();
		searchField.setWidth("50%");
		searchField.setPlaceholder("Search in Properties");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> dataProvider.refreshAll());

		dataProvider.addFilter(pe -> {
			String searchTerm = searchField.getValue().trim();

			if (searchTerm.isEmpty())
				return true;

			boolean matchesProperty= matchesTerm(pe.name, searchTerm);
			boolean matchesValue = matchesTerm(valueToString(pe.getValue()), searchTerm);

			return matchesProperty || matchesValue;
		});    	    	
		return searchField;
	}

	private String valueToString(Object value) {
		if (value == null) return "null";
		if (value instanceof Instance ) {
			return ((Instance) value).className();
		}
		else if (value instanceof InstanceType) {
			return ((InstanceType) value).name();
		} else if (value instanceof Map) {
			return (String) ((Map) value).entrySet().stream()
					.map(entry-> ((Map.Entry<String, Object>)entry).getKey()+valueToString(((Map.Entry<String, Object>)entry).getValue()) )
					.collect(Collectors.joining());
		} else if (value instanceof Collection) {
			return (String) ((Collection) value).stream().map(v->valueToString(v)).collect(Collectors.joining());
		} else if (value instanceof PropertyType) {
			return ((PropertyType) value).name();
		}
		else
			return value.toString();

	}

	private boolean matchesTerm(String value, String searchTerm) {
		if (searchTerm != null && !searchTerm.isEmpty() && value != null)
			return value.toLowerCase().contains(searchTerm.toLowerCase());
		else return false;
	}

	private Component instanceAsList(List<Property> content) {
		//List<Property> content = inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()) ; //.filter(prop -> !prop.name.startsWith("@"))	
		dataProvider = new ListDataProvider<>(content);
		Grid<Property> grid = new Grid<Property>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<Property> nameColumn = grid.addColumn(p -> p.name).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
		Grid.Column<Property> valueColumn = grid.addColumn(createValueRenderer()).setHeader("Value").setResizable(true);
		grid.setDataProvider(dataProvider);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setHeightByRows(true);
		return grid;
	}

//	private Component instanceAsList(InstanceType inst) {
//		List<Property> content = inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()) ; //.filter(prop -> !prop.name.startsWith("@"))
//		dataProvider = new ListDataProvider<>(content);
//		Grid<Property> grid = new Grid<Property>();
//		grid.setColumnReorderingAllowed(false);
//		Grid.Column<Property> nameColumn = grid.addColumn(p -> p.name).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
//		Grid.Column<Property> valueColumn = grid.addColumn(createValueRenderer()).setHeader("Value").setResizable(true);
//		grid.setDataProvider(dataProvider);
//		grid.setHeightByRows(true);
//		return grid;
//	}

	private Component getReloadIcon(Instance inst) {
		if (inst == null && commandGateway.getUIConfig().doGenerateRefetchButtonsPerArtifact()) return new Paragraph("");
		Icon icon = new Icon(VaadinIcon.REFRESH);
		icon.getStyle().set("cursor", "pointer");
		icon.getElement().setProperty("title", "Force Refetching of Artifact");
		icon.addClickListener(e -> { 
			ArtifactIdentifier ai = commandGateway.getProcessChangeListenerWrapper().getArtifactIdentifier(inst);
			new Thread(() -> { 
				try {
					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server", inst.name())));
					commandGateway.getArtifactResolver().get(ai, true);
					this.getUI().get().access(() ->Notification.show(String.format("Fetching succeeded", inst.name())));
				} catch (ProcessException e1) {
					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server failed: %s", inst.name(), e1.getMainMessage())));
				}}
					).start();
		});
		return icon;
	}

	public static class PropertyComparator implements Comparator<Property> {

		@Override
		public int compare(Property o1, Property o2) {
			return o1.name.compareTo(o2.name);
		}

	}

	private static ComponentRenderer<Span, Property> createValueRenderer() {
		return new ComponentRenderer<>(Span::new, propertyComponentUpdated);
	}

	private static final SerializableBiConsumer<Span, Property> propertyComponentUpdated = (span, prop) -> {
		if (prop instanceof SingleProperty) {
			span.add(singleValueToComponent(prop.get()));
		} else     	
			if (prop instanceof CollectionProperty) {
				span.add(collectionValueToComponent((Collection) prop.get()));
			} else
				if (prop instanceof MapProperty) {
					// not supported yet
					span.add(mapValueToComponent(((MapProperty)prop).get()));
				}
				else span.setText("Unknown Property ");
	}; 

	private static Component singleValueToComponent(Object value) {
		if (value instanceof Instance) {
			Instance inst = (Instance)value;
			return new Paragraph(new Anchor("/instance/"+inst.id(), inst.name()));
		} else if (value instanceof InstanceType) {
			InstanceType inst = (InstanceType)value;
			return new Paragraph(new Anchor("/instance/"+inst.id(), inst.name()));
		} else if (value instanceof PropertyType) {
			PropertyType pt = (PropertyType)value;
			return new Paragraph(String.format("PropertyType: %s %s of type %s", pt.name(), pt.cardinality(), pt.referencedInstanceType()));
		} else
			return new Paragraph(Objects.toString(value));
	}

	private static Component collectionValueToComponent(Collection value) {
		if (value == null || value.size() == 0)	
			return new Paragraph( "[ ]");
		else if (value.size() == 1)
			return singleValueToComponent(value.iterator().next());
		else {
			UnorderedList list = new UnorderedList();
			list.setClassName("no-padding");
			value.stream().forEach(val -> list.add(singleValueToComponent(val)));
			return list;
		}
	}


	protected static Component mapValueToComponent(Map value) {
		Grid<Map.Entry<String, Object>> grid = new Grid<Map.Entry<String, Object>>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<Map.Entry<String, Object>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader("Key").setResizable(true).setSortable(true);
		Grid.Column<Map.Entry<String, Object>> valueColumn = grid.addColumn(createMapValueRenderer()).setHeader("Value").setResizable(true);
		grid.setItems(value.entrySet());
		grid.setHeightByRows(true);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		return grid;
	}

	private static ComponentRenderer<Span, Map.Entry<String, Object>> createMapValueRenderer() {
		return new ComponentRenderer<>(Span::new, propertyMapUpdated);
	}

	private static final SerializableBiConsumer<Span, Map.Entry<String, Object>> propertyMapUpdated = (span, obj) -> {
		if (obj.getValue() instanceof Collection) {
			span.add(collectionValueToComponent((Collection)obj.getValue()));
		} else     	
			if (obj.getValue() instanceof Map) {
				span.add(mapValueToComponent((Map)obj.getValue()));
			} else {
				span.add(singleValueToComponent(obj.getValue()));
			}

	};
}
