package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Route("definitions")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Process Definitions")
@UIScope
//@SpringComponent
public class DefinitionView extends VerticalLayout  {

    
    protected RequestDelegate commandGateway;
    
    private Set<ProcessDefinitionScopedElement> pdefs = new HashSet<>();
    private TreeGrid<ProcessDefinitionScopedElement> grid = null;
    //private ListDataProvider<ProcessDefinitionScopedElement> dataProvider = new ListDataProvider<>(pdefs);
    
    public DefinitionView(RequestDelegate commandGateway, SecurityService securityService) {
    	this.commandGateway = commandGateway;
        setSizeFull();
        setMargin(false);
        setPadding(false);
        AppHeader header = new AppHeader("Process Definitions", securityService, commandGateway.getUIConfig());
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
        if (commandGateway != null ) {

        	ComboBox<ProcessDefinition> comboBox = new ComboBox<>("Process Definitions");
        	List<ProcessDefinition> defs = commandGateway.getRegistry().getAllDefinitions(true).stream()
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
            			return ((SequenceSubscopeDecisionNodeDefinition)o).getScope().stream();
            		} else if (o instanceof DecisionNodeDefinition) {
            			return getChildElementsFromDecisionNode((DecisionNodeDefinition)o);
            		} else {
                        log.error("ProcessDefinitionTree got unexpected artifact: " + o.getClass().getSimpleName());
                        return Stream.empty();
                    }
            	});
        		grid.getDataProvider().refreshAll();
        		grid.expandRecursively(Collections.singletonList(null), 10);
        	});
        	comboBox.setMinWidth("800px");
        	layout.add(comboBox);        	      	
        	
        	grid = new TreeGrid<>();
        	grid.addComponentHierarchyColumn(o -> {
        		if (o instanceof ProcessDefinition || o instanceof StepDefinition) {
        			Span span = new Span(o.getName());
        			return span;        		
        		} else if (o instanceof SequenceSubscopeDecisionNodeDefinition) { 
        			return getDndIcon((SequenceSubscopeDecisionNodeDefinition)o);
        		} else if (o instanceof DecisionNodeDefinition) { 
        			DecisionNodeDefinition scopeClosingDN = ((DecisionNodeDefinition) o).getScopeClosingDecisionNodeOrNull(); // wont be null as ending dnd will have been filtered out before 
        			return getDndIcon(scopeClosingDN);
        		} else {
                    return new Paragraph(o.getClass().getSimpleName() +": " + o.getName());
                }
        	}).setHeader("Process Definition Structure");//.setWidth("60%");
//        	grid.addComponentColumn(o -> {
//        		if (o instanceof StepDefinition) {
//        			return paramsToList( ((StepDefinition) o).getExpectedInput() );
//        		} else
//        			return new Paragraph("");
//        	}).setHeader("Expected Input").setWidth("20%").setFlexGrow(0);
//        	grid.addComponentColumn(o -> {
//        		if (o instanceof StepDefinition) {
//        			return paramsToList( ((StepDefinition) o).getExpectedOutput() );
//        		} else
//        			return new Paragraph("");
//        	}).setHeader("Expected Output").setWidth("20%").setFlexGrow(0);
        	
        	layout.add(grid);        	
        	
        }
        return layout;
    }
    
    private Component paramsToList(Map<String, InstanceType> stepParams) {
    	VerticalLayout l = new VerticalLayout();
    	l.setMargin(false);
        l.setPadding(false);
    	stepParams.forEach((param,type) -> {
    		l.add(new Paragraph(String.format("%s <%s>", param, type.name())));
    	});
    	return l;
    }
    
    private Component getDndIcon(DecisionNodeDefinition dnd) {
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
    
    private Icon createIcon(VaadinIcon vaadinIcon) {
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
    			.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
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
    	return children.stream().sorted(new PDSEComparator());    	    				  
    }
    
    private SequenceSubscopeDecisionNodeDefinition createSubscope(DecisionNodeDefinition parentDND, StepDefinition starter, int scopeClosingIndex, List<DecisionNodeDefinition> childDNDs, List<StepDefinition> childSteps) {
    	int startIndex = starter.getProcOrderIndex();
    	int depthIndex = starter.getDepthIndex();
    	List<StepDefinition> steps = starter.getProcess().getStepDefinitions().stream()    			
    			.filter(step -> step.getDepthIndex() == depthIndex) // only at the same level
    			.filter(step -> step.getProcOrderIndex() >= startIndex) // including starter
    			.filter(step -> step.getProcOrderIndex() <= scopeClosingIndex) // and closing scope (which might be the end DND)
    			.filter(step -> !childSteps.contains(step)) // but is also not a regular child step (in case the end is defined by closingDND
    			.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) // and no NoOpStep
    			.collect(Collectors.toList());
    	
    	List<DecisionNodeDefinition> subscopeDnds = childDNDs.stream()    			
    			.filter(dnd -> dnd.getOutSteps().size() > 1)
    			.filter(dnd -> dnd.getProcOrderIndex() >= startIndex && dnd.getProcOrderIndex() <= scopeClosingIndex)
    			.collect(Collectors.toList()); 
    	childDNDs.removeAll(subscopeDnds);
    	
    	List<ProcessDefinitionScopedElement> scopeMembers = new LinkedList<>();
    	scopeMembers.addAll(steps);
    	scopeMembers.addAll(subscopeDnds);    	
    	scopeMembers.stream().sorted(new PDSEComparator());
    	
    	SequenceSubscopeDecisionNodeDefinition subDND = new SequenceSubscopeDecisionNodeDefinition(parentDND.getInstance(), scopeMembers);
    	return subDND;
    }
    
    private Stream<ProcessDefinitionScopedElement> getChildElementsFromProcess(ProcessDefinition pdef) {
    	int indexToMatch = pdef.getDepthIndex() + 1; // only one below current step
    	List<StepDefinition> steps = pdef.getStepDefinitions().stream()    			
    			.filter(step -> step.getDepthIndex() == indexToMatch) 
    			.filter(step -> !step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX))
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
    	return children.stream().sorted(new PDSEComparator());
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
		
		public List<ProcessDefinitionScopedElement> getScope() {
			return scope;
		}
		
    	@Override
    	public InFlowType getInFlowType() {
    		return InFlowType.SEQ;
    	}
    }
}
