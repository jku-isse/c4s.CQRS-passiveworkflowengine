package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.method.P;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory.PropertySchemaDTO;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition.CoreProperties;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="definitions", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Definitions")
@UIScope
//@SpringComponent
public class DefinitionView extends VerticalLayout implements HasUrlParameter<String> {

    
    protected RequestDelegate commandGateway;
    private IFrontendPusher pusher;
    private ProcessConfigBaseElementFactory configFactory;

    private Set<ProcessDefinitionScopedElement> pdefs = new HashSet<>();
    private TreeGrid<ProcessDefinitionScopedElement> grid = null;
    
    private VerticalLayout detailsContent = new VerticalLayout();
    private SplitLayout splitLayout = null;
    
    //private String selectedProcess = "";
    private ProcessDefinition selectedP = null;
    private List<ProcessDefinition> defs = Collections.emptyList();
    private ComboBox<ProcessDefinition> comboBox;
    
    public DefinitionView(RequestDelegate commandGateway, SecurityService securityService, IFrontendPusher pusher, ProcessConfigBaseElementFactory configFactory) {    	
    	this.commandGateway = commandGateway;
    	this.configFactory = configFactory;
    	this.pusher = pusher;
    	setMargin(false);
    	setPadding(false);
    }
    
    private Icon getFalseIcon() {
    	Icon falseIcon = new Icon(VaadinIcon.CLOSE);
        falseIcon.setColor("red");
        falseIcon.getStyle()
        .set("box-sizing", "border-box")
        .set("margin-inline-end", "var(--lumo-space-m)")
        .set("margin-inline-start", "var(--lumo-space-xs)")
        .set("padding", "var(--lumo-space-xs)");
        return falseIcon;
    }
        
    private Icon getTrueIcon() {
       Icon trueIcon = new Icon(VaadinIcon.CHECK);
        trueIcon.setColor("green");
        trueIcon.getStyle()
        .set("box-sizing", "border-box")
        .set("margin-inline-end", "var(--lumo-space-m)")
        .set("margin-inline-start", "var(--lumo-space-xs)")
        .set("padding", "var(--lumo-space-xs)");
        return trueIcon;
	}


	@Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String param) {       
    	Location location = beforeEvent.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();
    	Map<String, List<String>> parametersMap = queryParameters.getParameters();
        String processName = parametersMap.getOrDefault("processName", List.of("")).get(0);
    	if (processName == null)
        	this.add(processTreeView(""));
        else {
        	this.add(processTreeView(processName));        	        
        }
    }
    
    private VerticalLayout processTreeView(String selectedProcess) {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidthFull();
        
        if (commandGateway != null ) {
        	comboBox = new ComboBox<>("Process Definitions");        	
        	defs = commandGateway.getRegistry().getAllDefinitions(true).stream()
        			.filter(pdef -> pdef.getProcess() == null) // only top level processes shown
        			.peek(pdef -> {
        				if (pdef.getName().equals(selectedProcess)) {
        					selectedP = pdef;
        				}
        			})
        			.sorted(new DefinitionComparator())
        				.collect(Collectors.toList());
        	comboBox.setItems(defs);        	
        	comboBox.setItemLabelGenerator(pdef -> { return pdef.getName() + " (DSid: "+pdef.getInstance().id()+")"; } );
        	comboBox.addValueChangeListener( e -> {
        		pdefs.clear();
        		pdefs.add(e.getValue());
        		grid.setItems(this.pdefs.stream(), o -> {
            		if (o instanceof ProcessDefinition) {
            			return getChildElementsFromProcess((ProcessDefinition)o);
            		} else if (o instanceof StepDefinition){
            			return Stream.empty();
            		} else if (o instanceof SequenceSubscopeDecisionNodeDefinition) { 
            			return ((SequenceSubscopeDecisionNodeDefinition)o).getScope();
            		} else if (o instanceof DecisionNodeDefinition) {
            			return getChildElementsFromDecisionNode((DecisionNodeDefinition)o);
            		} else {
                        log.error("ProcessDefinitionTree got unexpected artifact: " + o.getClass().getSimpleName());
                        return Stream.empty();
                    }
            	});
        		grid.getDataProvider().refreshAll();
        		grid.expandRecursively(Collections.singletonList(null), 10);
        		   		        		        	
        		splitLayout.setSplitterPosition(70);
        	});
        	comboBox.setMinWidth("600px");        	        	       
        	layout.add(comboBox);        	      	
        	
        	grid = new TreeGrid<>();
        	grid.setMinWidth("100px");
        	grid.setAllRowsVisible(true);
        	grid.addComponentHierarchyColumn(o -> {
        		if (o instanceof ProcessDefinition || o instanceof StepDefinition) {
        			Icon icon = o instanceof ProcessDefinition ? createIcon(VaadinIcon.ARROW_CIRCLE_RIGHT) : createIcon(VaadinIcon.CLIPBOARD);
        			Span span = new Span(icon, new Span(o.getName()));
        			return span;        		
        		} else if (o instanceof SequenceSubscopeDecisionNodeDefinition) { 
        			return getDndIcon((SequenceSubscopeDecisionNodeDefinition)o);
        		} else if (o instanceof DecisionNodeDefinition) { 
        			DecisionNodeDefinition scopeClosingDN = ((DecisionNodeDefinition) o).getScopeClosingDecisionNodeOrNull(); // wont be null as ending dnd will have been filtered out before 
        			return getDndIcon(scopeClosingDN);
        		} else {
                    return new Span(o.getClass().getSimpleName() +": " + o.getName());
                }
        	}).setHeader("Process Definition Structure");
        	
        	grid.addSelectionListener( o -> {
        		o.getFirstSelectedItem().ifPresentOrElse(el -> fillDetailsView(el), () -> fillDetailsView(null));
        	});
        	grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        	
        	detailsContent.setMinWidth("100px");
        	//detailsContent.setSpacing(false);
        	detailsContent.setMargin(false);
        	resetDetailsContent();
        	splitLayout = new SplitLayout(grid, detailsContent);
        	splitLayout.setWidthFull();
        	layout.add(splitLayout);    
        	if (selectedP != null)
        		comboBox.setValue(selectedP); 
        }
        return layout;
    }  
    
    private void resetDetailsContent() {
    	detailsContent.removeAll();
    	detailsContent.setVisible(false);
    }
    
    private void fillDetailsView(ProcessDefinitionScopedElement el) {    	
    	if (el != null && !(el instanceof DecisionNodeDefinition)) {
    		detailsContent.removeAll();
    		detailsContent.setVisible(true);    		
    		StepDefinition step = (StepDefinition)el;
    		addInfoHeader(detailsContent, step);
    		addConstraintTable(detailsContent, "PreCondition", step.getPreconditions());
    		addConstraintTable(detailsContent, "PostCondition", step.getPostconditions());
    		addConstraintTable(detailsContent, "CancelCondition", step.getCancelconditions());
    		addConstraintTable(detailsContent, "ActivationCondition", step.getActivationconditions());
    		addConstraintTable(detailsContent, "QA", step.getQAConstraints());    			
    		addParams(detailsContent, step.getExpectedInput(), "Input Parameters");
    		addParams(detailsContent, step.getExpectedOutput(), "Output Parameters");    		    		
    		addConfigViewIfTopLevelProcess(detailsContent, step);
    		addDeleteButtonIfTopLevelProcess(detailsContent, step);
    	} else {    		
    		resetDetailsContent();
    		//grid.setWidthFull();
    		//splitLayout.setSplitterPosition(70);    		    		
    	}
    }        

	private void addInfoHeader(VerticalLayout l,StepDefinition step) {		
        H5 header= new H5(step.getName());
        header.setClassName("info-header");
        l.add(header);
        if(step.getHtml_url()!=null)
        {
        	Anchor a =new Anchor(step.getHtml_url(),step.getHtml_url());
        	a.setClassName("info-header");
        	a.setTarget("_blank");
        	l.add(a);
        }
        if(step.getDescription()!=null)
        {
        	Html h=new Html("<span>"+step.getDescription()+"</span>");
        	l.add(h);
        }		
	}
	
//	private void addConditionsTable(VerticalLayout l, StepDefinition step) {
//		
//		Grid<AbstractMap.SimpleEntry<Conditions,String>> grid = new Grid<AbstractMap.SimpleEntry<Conditions,String>>();
//		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
//    	grid.setColumnReorderingAllowed(false);
//    	Grid.Column<AbstractMap.SimpleEntry<Conditions,String>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader("Constraint Type").setResizable(true).setWidth("150px").setFlexGrow(0);
//    	Grid.Column<AbstractMap.SimpleEntry<Conditions,String>> valueColumn = grid.addComponentColumn(p -> createValueRenderer(p.getValue())).setHeader("Constraint").setResizable(true);
//    	Map<String, Object> prop = (Map<String, Object>) step.getInstance().getProperty(CoreProperties.conditions.toString()).getValue();
//    	List<AbstractMap.SimpleEntry<Conditions,String>> entries = prop.entrySet().stream()
//    			.map(entry -> new AbstractMap.SimpleEntry<Conditions,String>(Conditions.valueOf(entry.getKey()), Objects.toString(entry.getValue())) )
//    			.sorted(new Comparator<AbstractMap.SimpleEntry<Conditions, String>>(){
//    				@Override
//    				public int compare(AbstractMap.SimpleEntry<Conditions,String> o1, AbstractMap.SimpleEntry<Conditions,String> o2) {
//    					return Integer.compare(o1.getKey().ordinal(), o2.getKey().ordinal());
//					}})
//    			.collect(Collectors.toList()); 
//    	grid.setItems(entries);
//    	grid.setAllRowsVisible(true);    
//    	grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
//    	l.add(grid);
//	}
    
	private void addConstraintTable(VerticalLayout l, String prefix, Set<ConstraintSpec> constraints) {
		
		Grid<ConstraintSpec> grid = new Grid<ConstraintSpec>();		
    	grid.setColumnReorderingAllowed(false);
    	Grid.Column<ConstraintSpec> idColumn = grid.addColumn(p -> p.getConstraintId())
    			.setHeader(prefix+" ID")
    			.setResizable(true)
    			.setWidth("150px")
    			.setFlexGrow(0);
    	Grid.Column<ConstraintSpec> nameColumn = grid.addComponentColumn(p -> createValueRenderer(p.getHumanReadableDescription()))
    			.setHeader("Description")
    			.setResizable(true);
    	Grid.Column<ConstraintSpec> valueColumn = grid.addComponentColumn(p -> createValueRenderer(p.getConstraintSpec()))
    			.setHeader("Constraint")
    			.setResizable(true);
    	Grid.Column<ConstraintSpec> overrideColumn = grid.addComponentColumn(p -> createTrueFalseIconRenderer(p.isOverridable()))
    			.setHeader("Overridable?")
    			.setWidth("100px")
    			.setFlexGrow(0)
    			.setResizable(false);
    	
    	if (constraints.isEmpty()) {    	
    		grid.setItems(List.of(new DummySpec()));
    	} else { grid.setItems(constraints.stream().sorted(new Comparator<ConstraintSpec>() {
			@Override
			public int compare(ConstraintSpec o1, ConstraintSpec o2) {
				return Integer.compare(o1.getOrderIndex(), o2.getOrderIndex());
			}    		
    	}));
    	}
    	grid.setAllRowsVisible(true);
    	grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    	l.add(grid);
	}
	
	protected Icon createTrueFalseIconRenderer(boolean value) {		
		if (value) 
			return getTrueIcon(); 
		else 
			return getFalseIcon();
	}
	
	protected static Component createValueRenderer(String arl) {
		Paragraph p = new Paragraph(arl);
		//Span p = new Span(arl);
		p.getStyle().set("white-space", "pre");
		p.setTitle(arl);
		return p;	
    }
        
    private void addParams(VerticalLayout l, Map<String, InstanceType> stepParams, String title) {
    	Grid<Map.Entry<String, InstanceType>> grid = new Grid<Map.Entry<String, InstanceType>>();
    	grid.setColumnReorderingAllowed(false);
    	Grid.Column<Map.Entry<String, InstanceType>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader(title).setResizable(true);
    	Grid.Column<Map.Entry<String, InstanceType>> valueColumn = grid.addColumn(createTypeRenderer()).setHeader("Type").setResizable(true);    	
    	if (stepParams.isEmpty()) {
    		grid.setItems(List.of(new AbstractMap.SimpleEntry<String, InstanceType>("None", null)));
    	} else {
    		grid.setItems(stepParams.entrySet().stream()
    				.sorted(new Comparator<Map.Entry<String, InstanceType>>() {
						@Override
						public int compare(Entry<String, InstanceType> o1, Entry<String, InstanceType> o2) {
							return o1.getKey().compareTo(o2.getKey());							
						}    					
    				}) 
    			.collect(Collectors.toList())
    			);
    	}
    	grid.setAllRowsVisible(true);
    	grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    	l.add(grid);
    }
    
    private void addDeleteButtonIfTopLevelProcess(VerticalLayout l, StepDefinition step) {
    	if (step instanceof ProcessDefinition && step.getProcess() == null && !commandGateway.getUIConfig().isExperimentModeEnabled()) { // only a toplevel process has no ProcessDefinitio returned via getProcess    		
    		Icon delIcon = new Icon(VaadinIcon.TRASH);
            delIcon.setColor("red");
            delIcon.getStyle()
            .set("box-sizing", "border-box")
            .set("margin-inline-end", "var(--lumo-space-m)")
            .set("margin-inline-start", "var(--lumo-space-xs)")
            .set("padding", "var(--lumo-space-xs)");

            Button deleteBtn = new Button("Delete Process Definition and process instances thereof", delIcon, e -> {            	
            	log.info("Deleting process definiton: "+step.getName());
            	Map<String, Map<String, Set<Instance>>> formerInputs = commandGateway.getRegistry().removeAllProcessInstancesOfProcessDefinition((ProcessDefinition)step);
            	log.info(String.format("Deleted %s running process instance(s)", formerInputs.size()));
            	formerInputs.keySet().forEach(id -> pusher.remove(id));
            	commandGateway.getRegistry().removeProcessDefinition(step.getName());
            	log.info("Deleted process definition: "+step.getName());
            	this.getUI().get().access(()-> UI.getCurrent().getPage().reload());
            });
            //delIcon.getElement().setProperty("title", "Delete this process definition");
            l.add(deleteBtn);
    	}
    }
    
	private void addConfigViewIfTopLevelProcess(VerticalLayout detailsContent2, StepDefinition step) {		
		if (step instanceof ProcessDefinition && step.getProcess() == null && !commandGateway.getUIConfig().isExperimentModeEnabled()) { // only a toplevel process has no ProcessDefinitio returned via getProcess 
			// see if a config is foreseen
			step.getExpectedInput().entrySet().stream()
			.filter(entry -> entry.getValue().isKindOf(configFactory.getBaseType()))
			.forEach(configEntry -> {
				InstanceType procConfig = configEntry.getValue();//configFactory.getOrCreateProcessSpecificSubtype(configEntry.getKey(), (ProcessDefinition) step);		
				
				Set<PropertyType> pTypes = procConfig.getPropertyTypes(false, true);
				ListDataProvider<PropertyType> dataProvider = new ListDataProvider<>(pTypes);
				detailsContent2.add(new Label(String.format("'%s' configuration properties:",configEntry.getKey())));
				detailsContent2.add(propertyTypesAsList(dataProvider));
				
				Details addPropDetails = new Details("Add new '"+configEntry.getKey()+"' Property", propertyAddControls(dataProvider, procConfig));
				addPropDetails.setOpened(false);
				addPropDetails.addThemeVariants(DetailsVariant.FILLED);
				detailsContent2.add(addPropDetails);
			});					
		}
	}
	
	private Component propertyAddControls(ListDataProvider<PropertyType> dataProvider, InstanceType configType) {				
		TextField nameField = new TextField();		
		nameField.setLabel("Property Name");
		nameField.setPattern("^[a-zA-Z0-9_]+$");
		/*
		 * ^ : start of string
			[ : beginning of character group
			a-z : any lowercase letter
			A-Z : any uppercase letter
			0-9 : any digit
			_ : underscore
			] : end of character group
		 	+ : one or more of the given characters
			$ : end of string
		 * */
		nameField.setPreventInvalidInput(true);
		ComboBox<InstanceType> types = new ComboBox<>("Property Type");
		types.setItems(Workspace.STRING, Workspace.BOOLEAN, Workspace.DATE, Workspace.INTEGER, Workspace.REAL);
		types.setValue(Workspace.BOOLEAN);
		ComboBox<Cardinality> cardinalities = new ComboBox<>("Cardinality");
		cardinalities.setItems(Cardinality.values());
		cardinalities.setValue(Cardinality.SINGLE);
		Icon addIcon = new Icon(VaadinIcon.PLUS);
        addIcon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
		Button createButton = new Button("Create Property", addIcon, evt -> {
			if (!nameField.isInvalid()) {
				String name = nameField.getValue().trim();				
				PropertySchemaDTO dto = new PropertySchemaDTO(name, types.getValue().name(), cardinalities.getValue().toString());
				Map<PropertySchemaDTO, Boolean> result = configFactory.augmentConfig(Set.of(dto), configType);
				if (result.get(dto) == true) {
					Notification.show("Successfully added property "+name);
					configType.workspace.concludeTransaction();
					Set<PropertyType> pTypes = configType.getPropertyTypes(false, true);
					dataProvider.getItems().clear();
					dataProvider.getItems().addAll(pTypes);
					dataProvider.refreshAll();
				} else {
					Notification.show("Unable to add property "+name);
				}
			} else {
				Notification.show("Please choose a valid property name");
			}
		});
		
		VerticalLayout l = new VerticalLayout();
		l.setPadding(false);
		l.add(nameField, types, cardinalities, createButton);
		return l;
	}
	
	private Component propertyTypesAsList(ListDataProvider<PropertyType> dataProvider) {		
		Grid<PropertyType> grid = new Grid<PropertyType>();
		grid.setColumnReorderingAllowed(false);
		Grid.Column<PropertyType> nameColumn = grid.addColumn(p -> p.name()).setHeader("Property").setWidth("300px").setResizable(true).setSortable(true).setFlexGrow(0);
		Grid.Column<PropertyType> typeColumn = grid.addColumn(p -> p.nativeType()).setHeader("Type").setResizable(true);
		Grid.Column<PropertyType> cardinalityColumn = grid.addColumn(p -> p.cardinality().toString() ).setHeader("Cardinality").setResizable(true);
		grid.setDataProvider(dataProvider);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		grid.setAllRowsVisible(true);		
		return grid;
	}
    
	private static ComponentRenderer<Span, Map.Entry<String, InstanceType>> createTypeRenderer() {
    	return new ComponentRenderer<>(Span::new, type2Component);
    }
    
    private static final SerializableBiConsumer<Span, Map.Entry<String, InstanceType>> type2Component = (span, obj) -> {
    	if (obj.getValue() != null) {
    		span.add(ComponentUtils.convertToResourceLinkWithBlankTarget(obj.getValue()));
    		//Paragraph p =  new Paragraph(ComponentUtils.convertToResourceLinkWithBlankTarget(obj.getValue()));		
    		//span.add(p);
    	}
    };
    

    
    public static Component getDndIcon(DecisionNodeDefinition dnd) {
    	Icon icon;
    	switch(dnd.getInFlowType()) {
		case AND:
			icon = createIcon(VaadinIcon.PLUS_CIRCLE_O);
			icon.getElement().setProperty("title", "Execute all in any order");
			break;
		case OR:
			icon = createIcon(VaadinIcon.CIRCLE_THIN );
			icon.getElement().setProperty("title", "Execute at least one in any order");
			break;
		case XOR:
			icon = createIcon(VaadinIcon.CLOSE_CIRCLE_O);
			icon.getElement().setProperty("title", "Execute only exactly one");
			break;
		case SEQ:
			icon = createIcon(VaadinIcon.ARROW_CIRCLE_RIGHT_O);
			icon.getElement().setProperty("title", "Execute all in sequence");
			break;			
		default:
			icon = createIcon(VaadinIcon.QUESTION);
			break;    	 
    	}    	    	
    	icon.setColor("grey");
    	Span typeSpan = new Span(icon, new Span(dnd.getInFlowType().toString()));
    	typeSpan.getElement().getThemeList().add("badge");
    	return typeSpan;
    }
    
    public static Icon createIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle()
                .set("padding", "var(--lumo-space-xs")
                .set("box-sizing", "border-box");
        return icon;
    }
    
    private Stream<ProcessDefinitionScopedElement> getChildElementsFromDecisionNode(DecisionNodeDefinition sdef) {
    	// get list of decision nodes at that hierarchy level and for each the set of the step children thereafter
    	int indexToMatch = sdef.getDepthIndex()+1;    	
    	ProcessDefinition pdef = sdef.getProcess();    	
    	// get the complete subscope from this dnd to the closing scope
    	DecisionNodeDefinition endDND = sdef.getScopeClosingDecisionNodeOrNull(); 
    	List<DecisionNodeDefinition> dnds = pdef.getDecisionNodeDefinitions().stream()
    			.filter(dnd -> dnd.getDepthIndex() == indexToMatch)
    			.filter(dnd -> dnd.getOutSteps().size() > 1)
    			.filter(dnd -> dnd.getProcOrderIndex() > sdef.getProcOrderIndex() && dnd.getProcOrderIndex() < endDND.getProcOrderIndex())
    			.collect(Collectors.toList()); 	
    	
    	List<StepDefinition> steps = pdef.getStepDefinitions().stream()    			
    			.filter(step -> step.getDepthIndex() == indexToMatch)
    			.filter(step -> sdef.getOutSteps().contains(step))
    	//		.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
    			.sorted(new StepComparator())
    			.collect(Collectors.toList());    	
   	
    	// if for any step, the subsequent dnd is not the closing scope, then we have a subscope that needs a dummy decision node for the tree visualization    	
    	List<StepDefinition> subscopeStarters = steps.stream()
    		.filter(step -> !endDND.getInSteps().contains(step) )
    		.collect(Collectors.toList());
    	steps.removeAll(subscopeStarters); // subscopeStarters are replaced by the pseudo seqeunce decision node
    	
    	// each entry is a different subscope starter, we replace each of these steps by a subscope dummy decision node, we maintain the order of the subscope starter steps
    	// the scope is defined by the specIndex starting from the starter, to the next subscope starter, or the closingDN
    	List<SequenceSubscopeDecisionNodeDefinition> scopes = new LinkedList<>();
    	while (!subscopeStarters.isEmpty()) {
    		StepDefinition scopeStarter = subscopeStarters.remove(0);
    		int scopeClosingIndex = subscopeStarters.isEmpty() ? endDND.getProcOrderIndex() : subscopeStarters.get(0).getProcOrderIndex()-1;
//    		assert (scopeClosingIndex > scopeStarter.getProcOrderIndex());
    		scopes.add(createSubscope(sdef, scopeStarter, scopeClosingIndex, dnds, steps));    		
    	}    	    
	
    	 // and no NoOpStep
    	
    	List<ProcessDefinitionScopedElement> children = new LinkedList<>();
    	children.addAll(steps);
    	children.addAll(dnds);
    	children.addAll(scopes);
    	// now sort them
    	return children.stream()
    			.filter(el -> !el.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
    			.sorted(new PDSEComparator());    	    				  
    }
    
    private SequenceSubscopeDecisionNodeDefinition createSubscope(DecisionNodeDefinition parentDND, StepDefinition starter, int scopeClosingIndex, List<DecisionNodeDefinition> childDNDs, List<StepDefinition> childSteps) {
    	int startIndex = starter.getProcOrderIndex();
    	int depthIndex = starter.getDepthIndex();
    	List<StepDefinition> steps = starter.getProcess().getStepDefinitions().stream()    			
    			.filter(step -> step.getDepthIndex() == depthIndex) // only at the same level
    			.filter(step -> step.getProcOrderIndex() >= startIndex) // including starter
    			.filter(step -> step.getProcOrderIndex() <= scopeClosingIndex) // and closing scope (which might be the end DND)
    			.filter(step -> !childSteps.contains(step)) // but is also not a regular child step (in case the end is defined by closingDND
    			//.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) // and no NoOpStep
    			.collect(Collectors.toList());
    	
    	List<DecisionNodeDefinition> subscopeDnds = childDNDs.stream()    			
    			.filter(dnd -> dnd.getOutSteps().size() > 1)
    			.filter(dnd -> dnd.getProcOrderIndex() >= startIndex && dnd.getProcOrderIndex() <= scopeClosingIndex)
    			.collect(Collectors.toList()); 
    	childDNDs.removeAll(subscopeDnds);
    	
    	List<ProcessDefinitionScopedElement> scopeMembers = new LinkedList<>();
    	scopeMembers.addAll(steps);
    	scopeMembers.addAll(subscopeDnds);    	
    	scopeMembers.sort(new PDSEComparator());
    	
    	SequenceSubscopeDecisionNodeDefinition subDND = new SequenceSubscopeDecisionNodeDefinition(parentDND.getInstance(), scopeMembers);
    	return subDND;
    }
    
    private Stream<ProcessDefinitionScopedElement> getChildElementsFromProcess(ProcessDefinition pdef) {
    	int indexToMatch = pdef.getDepthIndex() + 1; // only one below current step
    	List<StepDefinition> steps = pdef.getStepDefinitions().stream()    			
    			.filter(step -> step.getDepthIndex() == indexToMatch) 
    		//	.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
    			.collect(Collectors.toList());		
    	List<DecisionNodeDefinition> dnds = pdef.getDecisionNodeDefinitions().stream()
    			.filter(dnd -> dnd.getDepthIndex() == indexToMatch)
    			.filter(dnd -> dnd.getOutSteps().size() > 1)
    			.collect(Collectors.toList()); 
    	// now we need to create an interleaving list of decision nodes and steps
    	List<ProcessDefinitionScopedElement> children = new LinkedList<>();
    	children.addAll(steps);
    	children.addAll(dnds);
    	// now sort them
    	return children.stream()
    			.filter(el -> !el.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
    			.sorted(new PDSEComparator());
    }
    
    private static class PDSEComparator implements Comparator<ProcessDefinitionScopedElement> {
    	@Override
    	public int compare(ProcessDefinitionScopedElement o1, ProcessDefinitionScopedElement o2) {
    		int result = Integer.compare(o1.getProcOrderIndex(), o2.getProcOrderIndex()); 
    		if (result == 0) { // can only happen when one is a step and one is a decision node
    			if (o1 instanceof StepDefinition) // return the step as being ranked higher (i.e., lower proc order
    				return -1;
    			else
    				return +1;   						
    		} else
    			return result; 
    	}
    }
    
    public static class DefinitionComparator implements Comparator<ProcessDefinition> {

		@Override
		public int compare(ProcessDefinition o1, ProcessDefinition o2) {
			return o1.getName().compareTo(o2.getName());
		}
    	
    }
    
    private static class StepComparator implements Comparator<StepDefinition> {
		@Override
		public int compare(StepDefinition o1, StepDefinition o2) {
			return o1.getSpecOrderIndex().compareTo(o2.getSpecOrderIndex());
		}
    }
    
    private static class SequenceSubscopeDecisionNodeDefinition extends DecisionNodeDefinition {

    	private List<ProcessDefinitionScopedElement> scope;
    	
		public SequenceSubscopeDecisionNodeDefinition(Instance instance, List<ProcessDefinitionScopedElement> scope) {
			super(instance);
			this.scope = scope;			
		}
		
		public Stream<ProcessDefinitionScopedElement> getScope() {
			return scope.stream().filter(el -> !el.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX));
		}
		
    	@Override
    	public InFlowType getInFlowType() {
    		return InFlowType.SEQ;
    	}
    }
    
    private static class DummySpec extends ConstraintSpec {
    	
    	protected DummySpec() {
    		super(null);
    	}

		@Override
		public String getConstraintId() {
			return "None";
		}

		@Override
		public String getConstraintSpec() {
			return "";
		}

		@Override
		public String getHumanReadableDescription() {
			return "";
		}

		@Override
		public Integer getOrderIndex() {
			return -1;
		}

		@Override
		public String getId() {
			return "-1";
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public boolean isOverridable() {
			return false;
		}

		@Override
		public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
			;
		}    	
		
		
    }
}
