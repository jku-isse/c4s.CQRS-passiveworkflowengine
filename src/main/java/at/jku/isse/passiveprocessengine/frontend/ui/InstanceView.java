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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

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

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
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
	private String id = null;
	private PPEInstance inst;
	private ListDataProvider<Entry<PPEPropertyType, Object>> dataProvider = null;
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

		if (commandGateway.getUiConfig().isAnonymized())  {
			layout.add(new Paragraph("This view is not available in double blind reviewing mode"));
		} else       		    	    {
			layout.add(createFetchField());
			if (commandGateway != null && commandGateway.getProcessContext() != null && id != null) {
				layout.add(new Hr());
				HorizontalLayout hl = new HorizontalLayout();
				Optional<PPEInstance> el = commandGateway.getProcessContext().getInstanceRepository().findInstanceById(id);
				if (el.isEmpty()) { 
					layout.add(new Paragraph("Instance with id "+id+" does not exists."));
					return layout;				
				}
				inst = el.get();
				// Authentication here:
				if (!commandGateway.getAclProvider().isAuthorizedToView(inst)) {
					layout.add(new Paragraph("You are not authorized to access the artifact."));
				}								
				
				Paragraph elName = new Paragraph("Artifact/Instance (DSid="+id+"): "+inst.getName());
				hl.add(elName);

				Component grid = null;
				Dialog dialog = new Dialog();				 				
				//				if (el instanceof PPEInstance) {        		
				PPEInstanceType configBaseType = commandGateway.getProcessContext().getSchemaRegistry().getTypeByName(ProcessConfigBaseElementType.typeId);
				if (!inst.getInstanceType().isOfTypeOrAnySubtype(configBaseType)) { // only allow editing of process config instances
					editmode = EDITMODE.readonly;
				} else {
					editmode = EDITMODE.write;
				}
				if (!isFullyFetched(inst))
					hl.add(addButtonToFullyFetchLazyLoadedInstance(inst));
				else if (inst.getInstanceType().hasPropertyType(PPEInstanceType.IS_FULLYFETCHED)) { 
					hl.add(getReloadIcon(inst));
				}
				List<Entry<PPEPropertyType, Object>> props = inst.getInstanceType().getPropertyNamesIncludingSuperClasses().stream()
						.sorted()
						.map(pName -> inst.getInstanceType().getPropertyType(pName))
						.map(prop -> new AbstractMap.SimpleEntry<PPEPropertyType, Object>(prop, inst.getTypedProperty(prop.getName(), Object.class)))
						.collect(Collectors.toList());
				grid = instanceAsList(props, dialog);
				//				} else if (el instanceof PPEInstanceType) {
				//					grid = instanceAsList(((PPEInstanceType) el).getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()), dialog, el.workspace);
				//}		
				if (inst.isMarkedAsDeleted()) { 
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
		if ( element.getInstanceType().hasPropertyType(PPEInstanceType.IS_FULLYFETCHED)) { // an instance with this property  
			return element.getTypedProperty(PPEInstanceType.IS_FULLYFETCHED, Boolean.class, true);
		} else
			return true;
	}

	private Component addButtonToFullyFetchLazyLoadedInstance(PPEInstance inst) {
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

	private ArtifactIdentifier getArtifactIdentifier(PPEInstance inst) {
		// less brittle, but requires consistent use by artifact connectors
		String artId = inst.getTypedProperty(CoreTypeFactory.EXTERNAL_DEFAULT_ID, String.class);
		PPEInstanceType instType = inst.getInstanceType();
		List<String> idOptions = commandGateway.getArtifactResolver().getIdentifierTypesForInstanceType(instType);
		if (idOptions.isEmpty()) {
			log.warn("Cannot determine identifier option for instance type: "+instType.getName());
			return new ArtifactIdentifier(artId, instType.getName());
		} 
		String idType = idOptions.get(0);				
		return new ArtifactIdentifier(artId, instType.getName(), idType);
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

			boolean matchesProperty= matchesTerm(pe.getKey().getName(), searchTerm);
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

	private Component instanceAsList(List<Entry<PPEPropertyType, Object>> content, Dialog dialog) {
		//List<Property> content = inst.getProperties().stream().sorted(new PropertyComparator()).collect(Collectors.toList()) ; //.filter(prop -> !prop.name.startsWith("@"))	
		dataProvider = new ListDataProvider<>(content);
		Grid<Entry<PPEPropertyType, Object>> grid = new Grid<Entry<PPEPropertyType, Object>>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<Entry<PPEPropertyType, Object>> nameColumn = grid.addColumn(p -> p.getKey().getName()).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
		Grid.Column<Entry<PPEPropertyType, Object>> valueColumn = grid.addColumn(createValueRenderer()).setHeader("Value").setResizable(true);
		grid.setDataProvider(dataProvider);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setAllRowsVisible(true);
		if (this.editmode.equals(EDITMODE.write) && dialog!= null) {
			dialog.getElement().setAttribute("aria-label", "Update Process Configuration Property");
			grid.addItemClickListener(item -> {
				PPEPropertyType prop = item.getItem().getKey();
				if (prop.getCardinality().equals(CARDINALITIES.SINGLE) && BuildInType.isAtomicType(prop.getInstanceType())) {
					VerticalLayout dialogLayout = createEditDialogLayout(dialog, prop);
					dialog.removeAll();
					dialog.add(dialogLayout);
					dialog.open();
				}
			});
		}
		return grid;
	}



	private static String KEY = "key";

	private VerticalLayout createEditDialogLayout(Dialog dialog, PPEPropertyType property) {
		H2 headline = new H2("Update Property: "+property.getName());
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0")
		.set("font-size", "1.5em").set("font-weight", "bold");
		PPEInstanceType type =  property.getInstanceType();
		Map<String,Object> newValue = new HashMap<>();
		Component input = null;
		if (type.equals(BuildInType.STRING)) {
			TextField stringField = new TextField(Objects.toString(inst.getTypedProperty(property.getName(), String.class)));
			stringField.setLabel("Enter new 'String' value: ");
			input = stringField;
			stringField.addValueChangeListener(change -> {
				newValue.put(KEY, change.getValue());
			});
		} else if (type.equals(BuildInType.BOOLEAN)) {
			ComboBox<Boolean> boolBox = new ComboBox<>();
			boolBox.setItems(Boolean.TRUE, Boolean.FALSE);
			boolBox.setLabel("Choose new 'Boolean' value: ");
			input = boolBox;
			Optional<Boolean> currentValue = Optional.ofNullable(inst.getTypedProperty(property.getName(), Boolean.class));
			currentValue.ifPresentOrElse(curValue -> boolBox.setValue(curValue), () -> { 
				boolBox.setValue(true); 
				newValue.put(KEY, true); });       // otherwise we need to set it to false and then true again when no value is present and we want to choose true 	
			boolBox.addValueChangeListener(change -> {
				newValue.put(KEY, change.getValue());
			});
		} else if (type.equals(BuildInType.DATE)) {
			DatePicker.DatePickerI18n singleFormatI18n = new DatePicker.DatePickerI18n();
			singleFormatI18n.setDateFormat("yyyy-MM-dd");
			DatePicker datePicker = new DatePicker("Date");
			datePicker.setLabel("Enter new 'Date' value: ");
			datePicker.setI18n(singleFormatI18n);
			Optional<Date> currentValue = Optional.ofNullable(inst.getTypedProperty(property.getName(), Date.class));
			currentValue.ifPresent(curValue -> datePicker.setValue(curValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));        	
			input = datePicker;
			datePicker.addValueChangeListener(change -> {
				ZoneId defaultZoneId = ZoneId.systemDefault();
				Date date = Date.from(change.getValue().atStartOfDay(defaultZoneId).toInstant());
				newValue.put(KEY,  date);
			});
		} else if (type.equals(BuildInType.INTEGER)) {
			NumberField numberField = new NumberField();     
			numberField.setMinWidth("150px");
			numberField.setLabel("Enter new 'Integer' number: ");
			input = numberField;
			Optional<Integer> currentValue = Optional.ofNullable(inst.getTypedProperty(property.getName(), Integer.class));
			currentValue.ifPresent(curValue -> numberField.setValue(curValue.doubleValue()));
			numberField.addValueChangeListener(change -> {
				newValue.put(KEY, change.getValue().intValue());
			});
		} else if (type.equals(BuildInType.FLOAT)) {
			NumberField numberField2 = new NumberField();       
			numberField2.setMinWidth("150px");
			numberField2.setLabel("Enter new 'Double' number");
			input = numberField2;
			Optional<Double> currentValue = Optional.ofNullable(inst.getTypedProperty(property.getName(), Double.class));
			currentValue.ifPresent(curValue -> numberField2.setValue(curValue));
			numberField2.addValueChangeListener(change -> {
				newValue.put(KEY, change.getValue());
			});
		} else {
			String msg = String.format("Edit Dialog encountered unsupported type %s for property %s", type.getName(), property.getName());
			log.warn(msg);
			dialog.close();
			Notification.show(String.format("Edit Dialog encountered unsupported type %s for property %s", type.getName(), property.getName()));
		}

		Button cancelButton = new Button("Cancel", e -> dialog.close());
		Button saveButton = new Button("Save", e -> { 
			if (newValue.containsKey(KEY)) {
				Object newObj = newValue.get(KEY);
				inst.setSingleProperty(property.getName(), newValue);
				//property.set(newObj);        		
				// conclude transaction in the background/tread
				new Thread(() -> { 
					commandGateway.getProcessContext().getInstanceRepository().concludeTransaction();
					//        			ws.concludeTransaction();
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

	private Component getReloadIcon(PPEInstance inst) {
		if (inst == null 
				|| commandGateway.getUiConfig().isGenerateRefetchButtonsPerArtifactEnabled()
				|| !inst.getInstanceType().isOfTypeOrAnySubtype(commandGateway.getProcessContext().getSchemaRegistry().getTypeByName(CoreTypeFactory.BASE_TYPE_NAME)))  { 
			return new Paragraph("");
		}
		Icon icon = new Icon(VaadinIcon.REFRESH);
		icon.getStyle().set("cursor", "pointer");
		icon.getElement().setProperty("title", "Force Refetching of Artifact");
		icon.addClickListener(e -> { 
			ArtifactIdentifier ai = commandGateway.getProcessChangeListenerWrapper().getArtifactIdentifier(inst);
			new Thread(() -> { 
				try {
					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server", inst.getName())));
					commandGateway.getArtifactResolver().get(ai, true);
					this.getUI().get().access(() ->Notification.show(String.format("Fetching succeeded", inst.getName())));
				} catch (ProcessException e1) {
					this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server failed: %s", inst.getName(), e1.getMessage())));
				}}
					).start();
		});
		return icon;
	}

	public static class PropertyComparator implements Comparator<PPEPropertyType> {

		@Override
		public int compare(PPEPropertyType o1, PPEPropertyType o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	private static ComponentRenderer<Span, Entry<PPEPropertyType, Object>> createValueRenderer() {
		return new ComponentRenderer<>(Span::new, propertyComponentUpdated);
	}

	private static final SerializableBiConsumer<Span, Entry<PPEPropertyType, Object>> propertyComponentUpdated = (span, entry) -> {

		switch(entry.getKey().getCardinality()) {
		case LIST: //fallthrough
		case SET:
			span.add(collectionValueToComponent((Collection) entry.getValue()));
			break;
		case MAP:
			span.add(mapValueToComponent((Map)entry.getValue()));
			break;
		case SINGLE:
			span.add(singleValueToComponent(entry.getValue()));
			break;
		default:
			span.setText("Unknown Property ");
			break;
		};
	}; 

	private static Component singleValueToComponent(Object value) {
		if (value instanceof PPEInstanceType) {
			PPEInstanceType inst = (PPEInstanceType)value;
			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instancetype/"+inst.getId(), inst.getName()));
		}else if (value instanceof PPEInstance) {
			PPEInstance inst = (PPEInstance)value;
			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instance/"+inst.getId(), inst.getName()));		
		} else if (value instanceof PPEPropertyType) {
			PPEPropertyType pt = (PPEPropertyType)value;
			return new Paragraph(String.format("PropertyType: %s %s of type %s", pt.getName(), pt.getCardinality(), pt.getInstanceType()));
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
