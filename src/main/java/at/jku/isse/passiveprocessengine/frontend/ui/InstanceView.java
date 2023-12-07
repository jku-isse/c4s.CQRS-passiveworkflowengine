package at.jku.isse.passiveprocessengine.frontend.ui;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.CollectionProperty;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import ch.qos.logback.core.Layout;
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
	private ProcessConfigBaseElementFactory configFactory;

	private Id id = null;
	private ListDataProvider<Property> dataProvider = null;
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


	public InstanceView(RequestDelegate commandGateway, SecurityService securityService, ProcessConfigBaseElementFactory configFactory) {
		this.commandGateway = commandGateway;
		this.configFactory = configFactory;
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
				hl.add(elName);
				
				Component grid = null;
				Dialog dialog = new Dialog();				 				
				if (el instanceof Instance) {        		
					Instance inst = (Instance) el;
					if (!inst.getInstanceType().isKindOf(configFactory.getBaseType())) { // only allow editing of process config instances
						editmode = EDITMODE.readonly;
					} else {
						editmode = EDITMODE.write;
					}
					if (!isFullyFetched(inst))
						hl.add(addButtonToFullyFetchLazyLoadedInstance(inst));
					else if (inst.hasProperty("fullyFetched")) {
						hl.add(getReloadIcon(inst));
					}
					grid = instanceAsList(inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()), dialog, inst.workspace);
				} else if (el instanceof InstanceType) {
					grid = instanceAsList(((InstanceType) el).getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()), dialog, el.workspace);
				}		
				if (el.isDeleted) { 
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
			boolean matchesValue = matchesTerm(PropertyConversionUtil.valueToString(pe.getValue()), searchTerm);

			return matchesProperty || matchesValue;
		});    	    	
		return searchField;
	}



	private boolean matchesTerm(String value, String searchTerm) {
		if (searchTerm != null && !searchTerm.isEmpty() && value != null)
			return value.toLowerCase().contains(searchTerm.toLowerCase());
		else return false;
	}

	private Component instanceAsList(List<Property> content, Dialog dialog, Workspace ws) {
		//List<Property> content = inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()) ; //.filter(prop -> !prop.name.startsWith("@"))	
		dataProvider = new ListDataProvider<>(content);
		Grid<Property> grid = new Grid<Property>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<Property> nameColumn = grid.addColumn(p -> p.name).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
		Grid.Column<Property> valueColumn = grid.addColumn(createValueRenderer()).setHeader("Value").setResizable(true);
		grid.setDataProvider(dataProvider);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setAllRowsVisible(true);
		if (this.editmode.equals(EDITMODE.write) && dialog!= null) {
			dialog.getElement().setAttribute("aria-label", "Update Process Configuration Property");
			grid.addItemClickListener(item -> {
				Property prop = item.getItem();
				if (prop.propertyType().cardinality().equals(Cardinality.SINGLE) && prop.propertyType().isPrimitive()) {
					VerticalLayout dialogLayout = createEditDialogLayout(dialog, prop, ws);
					dialog.removeAll();
					dialog.add(dialogLayout);
					dialog.open();
				}
			});
		}
		return grid;
	}

	
	
	private static String KEY = "key";
	
	private VerticalLayout createEditDialogLayout(Dialog dialog, Property property, Workspace ws) {
		H2 headline = new H2("Update Property: "+property.name);
        headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0")
                .set("font-size", "1.5em").set("font-weight", "bold");
        InstanceType type =  property.propertyType().referencedInstanceType();
        Map<String,Object> newValue = new HashMap<>();
        Component input = null;
        if (type.equals(Workspace.STRING)) {
        	TextField stringField = new TextField(Objects.toString(property.getValue()));
        	stringField.setLabel("Enter new 'String' value: ");
        	input = stringField;
        	stringField.addValueChangeListener(change -> {
        		newValue.put(KEY, change.getValue());
        	});
        } else if (type.equals(Workspace.BOOLEAN)) {
        	ComboBox<Boolean> boolBox = new ComboBox<>();
        	boolBox.setItems(Boolean.TRUE, Boolean.FALSE);
        	boolBox.setLabel("Choose new 'Boolean' value: ");
        	input = boolBox;
        	Optional<Boolean> currentValue = Optional.ofNullable((Boolean)property.getValue());
        	currentValue.ifPresentOrElse(curValue -> boolBox.setValue(curValue), () -> { 
        		boolBox.setValue(true); 
        		newValue.put(KEY, true); });       // otherwise we need to set it to false and then true again when no value is present and we want to choose true 	
        	boolBox.addValueChangeListener(change -> {
        		newValue.put(KEY, change.getValue());
        	});
        } else if (type.equals(Workspace.DATE)) {
        	DatePicker.DatePickerI18n singleFormatI18n = new DatePicker.DatePickerI18n();
        	singleFormatI18n.setDateFormat("yyyy-MM-dd");
        	DatePicker datePicker = new DatePicker("Date");
        	datePicker.setLabel("Enter new 'Date' value: ");
        	datePicker.setI18n(singleFormatI18n);
        	Optional<Date> currentValue = Optional.ofNullable((Date)property.getValue());
        	currentValue.ifPresent(curValue -> datePicker.setValue(curValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));        	
        	input = datePicker;
        	datePicker.addValueChangeListener(change -> {
        		ZoneId defaultZoneId = ZoneId.systemDefault();
        		Date date = Date.from(change.getValue().atStartOfDay(defaultZoneId).toInstant());
        		newValue.put(KEY,  date);
        	});
        } else if (type.equals(Workspace.INTEGER)) {
        	NumberField numberField = new NumberField();     
        	numberField.setMinWidth("150px");
        	numberField.setLabel("Enter new 'Integer' number: ");
        	input = numberField;
        	Optional<Integer> currentValue = Optional.ofNullable((Integer)property.getValue());
        	currentValue.ifPresent(curValue -> numberField.setValue(curValue.doubleValue()));
        	numberField.addValueChangeListener(change -> {
        		newValue.put(KEY, change.getValue().intValue());
        	});
        } else if (type.equals(Workspace.REAL)) {
        	NumberField numberField2 = new NumberField();       
        	numberField2.setMinWidth("150px");
        	numberField2.setLabel("Enter new 'Double' number");
        	input = numberField2;
        	Optional<Double> currentValue = Optional.ofNullable((Double)property.getValue());
        	currentValue.ifPresent(curValue -> numberField2.setValue(curValue));
        	numberField2.addValueChangeListener(change -> {
        		newValue.put(KEY, change.getValue());
        	});
        } else {
        	String msg = String.format("Edit Dialog encountered unsupported type %s for property %s", type.name(), property.name);
        	log.warn(msg);
        	dialog.close();
        	Notification.show(String.format("Edit Dialog encountered unsupported type %s for property %s", type.name(), property.name));
        }
       
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Save", e -> { 
        	if (newValue.containsKey(KEY)) {
        		Object newObj = newValue.get(KEY);
        		property.set(newObj);        		
            	// conclude transaction in the background/tread
        		new Thread(() -> { 
        			ws.concludeTransaction();
        			this.getUI().get().access(() -> { 
        				dataProvider.refreshAll();
        			});
				} ).start();        		        		
        		dialog.close();
        	} else {
        		Notification.show("No input found, please provide a new value before saving");
        	}
        	 
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton,
                saveButton);
        buttonLayout
                .setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(headline, input,
                buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");

        return dialogLayout;
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
		if (inst == null && commandGateway.getUIConfig().isGenerateRefetchButtonsPerArtifactEnabled()) return new Paragraph("");
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
		grid.setAllRowsVisible(true);
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
