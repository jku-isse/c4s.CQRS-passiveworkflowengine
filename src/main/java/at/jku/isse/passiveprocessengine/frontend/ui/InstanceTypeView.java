package at.jku.isse.passiveprocessengine.frontend.ui;

import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.Cardinalities;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="instancetype", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Artifact/Instance Type Inspector")
@UIScope
//@SpringComponent
public class InstanceTypeView extends VerticalLayout implements HasUrlParameter<String> {

	private RequestDelegate commandGateway;
	private String id = null;
	private PPEInstanceType instType;
	private ListDataProvider<PPEPropertyType> dataProvider = null;
	private EDITMODE editmode = EDITMODE.readonly;

	private enum EDITMODE { readonly, write };
	
	@Override
	public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
		// example link: http://localhost:8080/home/?key=DEMO-9&value=Task
//		Location location = beforeEvent.getLocation();
//		QueryParameters queryParameters = location.getQueryParameters();
//		Map<String, List<String>> parametersMap = queryParameters.getParameters();
//		String mode = parametersMap.getOrDefault("editmode", List.of("readonly")).get(0);
//		try {
//			editmode = EDITMODE.valueOf(mode);
//		} catch (Exception e) {
//			editmode = EDITMODE.readonly;
//		}
		String strid = s;
		if (commandGateway.getUiConfig().isAnonymized()) {  
			id = null;
		}
		if (strid != null) {
//			try {
//				long lId = Long.parseLong(strid);
//				id = Id.of(lId);	
				id = strid;
//			} catch(Exception e) {
//				log.warn("Parameter id cannot be parsed to long");
//			}
		}
		statePanel();
	}


	public InstanceTypeView(RequestDelegate commandGateway, SecurityService securityService) {
		this.commandGateway = commandGateway;
		setSizeFull();
		setMargin(false);
		//setPadding(false);
	}

	private VerticalLayout statePanel() {
		VerticalLayout layout = this; // new VerticalLayout();
		layout.setMargin(false);
		layout.setWidthFull();

		if (commandGateway.getUiConfig().isAnonymized())  {
			layout.add(new Paragraph("This view is not available in double blind reviewing mode"));
		} else       		    	    {
			layout.add(createFetchField());
			if (commandGateway != null && commandGateway.getProcessContext() != null && id != null) {
				layout.add(new Hr());
				HorizontalLayout hl = new HorizontalLayout();
				Optional<PPEInstanceType> el = commandGateway.getProcessContext().getSchemaRegistry().findNonDeletedInstanceTypeById(id);
				if (el.isEmpty()) { 
					layout.add(new Paragraph("InstanceType with id "+id+" does not exists."));
					return layout;				
				}
				instType = el.get();
				Paragraph elName = new Paragraph("Artifact/Instance Type (DSid="+id+"): "+instType.getName());
				hl.add(elName);
				
				Component grid = null;
				Dialog dialog = new Dialog();				 				

					List<PPEPropertyType> props = instType.getPropertyNamesIncludingSuperClasses().stream()
							.sorted()
							.map(pName -> instType.getPropertyType(pName))
							//.map(prop -> new AbstractMap.SimpleEntry<PPEPropertyType, Object>(prop, instType.getTypedProperty(prop.getName(), Object.class)))
							.collect(Collectors.toList());
					grid = instanceAsList(props, dialog);
	
				if (instType.isMarkedAsDeleted()) { 
					elName.getStyle().set("background-color", "#ffbf00");
					editmode = EDITMODE.readonly;
				}
				layout.add(hl);
				layout.add(createSearchField());
				layout.add(grid);	
				layout.add(dialog);
			}
		}
		return layout;
	}

	public static boolean isFullyFetched(PPEInstance element) {
		if ( element.getInstanceType().hasPropertyType(PPEInstanceType.IS_FULLYFETCHED) 
				&&       element.getTypedProperty(PPEInstanceType.IS_FULLYFETCHED, Boolean.class, true) )
		{
			return false;
		} else
			return true;
	}

	private Component createFetchField() {
		TextField fetchField = new TextField();
		fetchField.setWidth("50%");
		fetchField.setPlaceholder("Find Instance Type by Id");
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

			boolean matchesProperty= matchesTerm(pe.getName(), searchTerm);
			boolean matchesValue = matchesTerm(PropertyConversionUtil.valueToString(pe), searchTerm);

			return matchesProperty || matchesValue;
		});    	    	
		return searchField;
	}



	private boolean matchesTerm(String value, String searchTerm) {
		if (searchTerm != null && !searchTerm.isEmpty() && value != null)
			return value.toLowerCase().contains(searchTerm.toLowerCase());
		else return false;
	}

	private Component instanceAsList(List<PPEPropertyType> content, Dialog dialog) {
		//List<Property> content = inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()) ; //.filter(prop -> !prop.name.startsWith("@"))	
		dataProvider = new ListDataProvider<>(content);
		Grid<PPEPropertyType> grid = new Grid<PPEPropertyType>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<PPEPropertyType> nameColumn = grid.addColumn(p -> p.getName()).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
		Grid.Column<PPEPropertyType> typeColumn = grid.addColumn(createValueRenderer()).setHeader("Value Type").setResizable(true);
		Grid.Column<PPEPropertyType> cardinalityColumn = grid.addColumn(p -> p.getCardinality()).setHeader("Cardinality").setResizable(true);
		grid.setDataProvider(dataProvider);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setAllRowsVisible(true);
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

//	private Component getReloadIcon(PPEInstance inst) {
//		if (inst == null && commandGateway.getUiConfig().isGenerateRefetchButtonsPerArtifactEnabled()) return new Paragraph("");
//		Icon icon = new Icon(VaadinIcon.REFRESH);
//		icon.getStyle().set("cursor", "pointer");
//		icon.getElement().setProperty("title", "Force Refetching of Artifact");
//		icon.addClickListener(e -> { 
//			ArtifactIdentifier ai = commandGateway.getProcessChangeListenerWrapper().getArtifactIdentifier(inst);
//			new Thread(() -> { 
//				try {
//					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server", inst.getName())));
//					commandGateway.getArtifactResolver().get(ai, true);
//					this.getUI().get().access(() ->Notification.show(String.format("Fetching succeeded", inst.getName())));
//				} catch (ProcessException e1) {
//					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server failed: %s", inst.getName(), e1.getMessage())));
//				}}
//					).start();
//		});
//		return icon;
//	}

	public static class PropertyComparator implements Comparator<PPEPropertyType> {

		@Override
		public int compare(PPEPropertyType o1, PPEPropertyType o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	private static ComponentRenderer<Span, PPEPropertyType> createValueRenderer() {
		return new ComponentRenderer<>(Span::new, propertyComponentUpdated);
	}

	private static final SerializableBiConsumer<Span, PPEPropertyType> propertyComponentUpdated = (span, propertyType) -> {		
		span.add(ComponentUtils.convertToResourceLinkWithBlankTarget(propertyType.getInstanceType()));
	}; 
//
//	private static Component singleValueToComponent(Object value) {
//		if (value instanceof PPEInstance) {
//			PPEInstance inst = (PPEInstance)value;
//			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instance/"+inst.getId(), inst.getName()));
//		} else if (value instanceof PPEInstanceType) {
//			PPEInstanceType inst = (PPEInstanceType)value;
//			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instance/"+inst.getId(), inst.getName()));
//		} else if (value instanceof PPEPropertyType) {
//			PPEPropertyType pt = (PPEPropertyType)value;
//			return new Paragraph(String.format("PropertyType: %s %s of type %s", pt.getName(), pt.getCardinality(), pt.getInstanceType()));
//		} else
//			return new Paragraph(Objects.toString(value));
//	}
//
//	private static Component collectionValueToComponent(Collection value) {
//		if (value == null || value.size() == 0)	
//			return new Paragraph( "[ ]");
//		else if (value.size() == 1)
//			return singleValueToComponent(value.iterator().next());
//		else {
//			UnorderedList list = new UnorderedList();
//			list.setClassName("no-padding");
//			value.stream().forEach(val -> list.add(singleValueToComponent(val)));
//			return list;
//		}
//	}
//
//
//	protected static Component mapValueToComponent(Map value) {
//		Grid<Map.Entry<String, Object>> grid = new Grid<Map.Entry<String, Object>>();
//		grid.setColumnReorderingAllowed(false);
//		Grid.Column<Map.Entry<String, Object>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader("Key").setResizable(true).setSortable(true);
//		Grid.Column<Map.Entry<String, Object>> valueColumn = grid.addColumn(createMapValueRenderer()).setHeader("Value").setResizable(true);
//		grid.setItems(value.entrySet());
//		grid.setAllRowsVisible(true);
//		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
//		return grid;
//	}
//
//	private static ComponentRenderer<Span, Map.Entry<String, Object>> createMapValueRenderer() {
//		return new ComponentRenderer<>(Span::new, propertyMapUpdated);
//	}
//
//	private static final SerializableBiConsumer<Span, Map.Entry<String, Object>> propertyMapUpdated = (span, obj) -> {
//		if (obj.getValue() instanceof Collection) {
//			span.add(collectionValueToComponent((Collection)obj.getValue()));
//		} else     	
//			if (obj.getValue() instanceof Map) {
//				span.add(mapValueToComponent((Map)obj.getValue()));
//			} else {
//				span.add(singleValueToComponent(obj.getValue()));
//			}
//
//	};
}
