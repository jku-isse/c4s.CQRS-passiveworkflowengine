package at.jku.isse.passiveprocessengine.frontend.ui;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.StepLifecycleStateMapper;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CssImport(value="./styles/grid-styles.css")
@CssImport(
        value= "./styles/dialog-overlay.css",
        themeFor = "vaadin-dialog-overlay"
)
public class WorkflowTreeGrid extends TreeGrid<ProcessInstanceScopedElement> { 	
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private RequestDelegate reqDel;    

    private Map<String, ProcessInstance> content;
    private String nameFilter;
    private String idFilter;
    private String focusedElementId = "";
    private ProcessInstanceScopedElement focusedElement = null;
//    private Map<String, String> propertiesFilter;
    private UIConfig conf;
    

    public WorkflowTreeGrid(RequestDelegate f) {
        this.reqDel = f;       
        content = new HashMap<>();
        nameFilter = ""; // default filter
        idFilter = "";
//        propertiesFilter = new HashMap<>();
//        propertiesFilter.put("", ""); // default filter
        this.conf = f.getUIConfig();
      
        //this.setHeightByRows(true);
        this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    }
    
    protected void injectRequestDelegate(RequestDelegate f) {
    	this.reqDel = f;
    }
    
    public void initTreeGrid() {

        // Column "Workflow Instance"
        this.addComponentHierarchyColumn(o -> {
            if (o instanceof ProcessInstance) {
                ProcessInstance wfi = (ProcessInstance) o;
                String name = wfi.getProcess() != null ? wfi.getDefinition().getName() : wfi.getName(); // when subprocess, then just definition name
                Span span= new Span(name);
                span.getElement().setProperty("title", wfi.getDefinition().getName() + " (" + wfi.getName() + ")");
                return span;
            } else if (o instanceof ProcessStep) {
            	ProcessStep wft = (ProcessStep) o;
            	Span span = new Span(wft.getDefinition().getName());                    
            	span.getElement().setProperty("title", wft.getDefinition().getName());
            	return span;
            } else if (o instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o;
                String title = rebc.getQaSpec().getHumanReadableDescription() != null ? rebc.getQaSpec().getHumanReadableDescription() : rebc.getQaSpec().getName();
                Span span = new Span(title);
                span.getElement().setProperty("title", rebc.getName());
                return span;
            } else if (o instanceof RepairNode) {
            	RepairNode rn = (RepairNode) o;
            	Span span = new Span(rn.toString());
                span.getElement().setProperty("title", rn.toString());
                return span;
            } else {
                return new Span(o.getClass().getSimpleName() +": " + o.getName());
            }
        }).setHeader("Process Instance").setWidth("65%");

        // Column "Last Changed"

        this.addColumn(o -> {
        	if (conf.doEnableExperimentMode()) {
        		if (o instanceof ProcessInstance) {
        			ProcessInstance pi = (ProcessInstance) o;
        			try {
        				ZonedDateTime now = ZonedDateTime.now();
        				Duration diff = Duration.between(pi.getCreatedAt(), now);
        			    String hms = String.format("%d:%02d:%02d", 
        			                                diff.toHours(), 
        			                                diff.toMinutesPart(), 
        			                                diff.toSecondsPart());
        				return "Running since: "+hms;
        			} catch (DateTimeException e) {
        				return " ";
        			}
        		} 
        	}
        	if (o instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o;
                try {
                    return formatter.format(rebc.getLastChanged());
                } catch (DateTimeException e) {
                    return "not available";
                }
            } else {
                return "";
            }
        }).setHeader("Last Changed").setWidth("15%").setFlexGrow(0);

        // Column status
        this.addColumn(new ComponentRenderer<Component, ProcessInstanceScopedElement>(o -> {
        	if (o instanceof ProcessInstance) {
        		return getStepIcon((ProcessInstance)o);
        		//return infoDialog((ProcessInstance)o);                                
            } else if (o instanceof ProcessStep) {
            	return getStepIcon((ProcessStep)o);
               // return infoDialog((ProcessStep)o);
            } else {
            		return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setHeader("State").setWidth("10%").setFlexGrow(0);

        // Column "Unsatisfied" or "Fulfilled"
        this.addColumn(new ComponentRenderer<Component, ProcessInstanceScopedElement>(o -> {      	
        	if (o instanceof ProcessInstance) {
        		ProcessInstance wfi = (ProcessInstance)o;
        		boolean unsatisfied = wfi.getProcessSteps().stream()
                        .anyMatch(wft -> !wft.areQAconstraintsFulfilled());
                boolean fulfilled = wfi.getProcessSteps().stream()
                        .anyMatch(wft -> wft.areQAconstraintsFulfilled());
               Icon icon = getIcon(unsatisfied, fulfilled, wfi.getProcessSteps().size());
               icon.setColor("grey");
               return icon;                              
            } else if (o instanceof ProcessStep) {
               ProcessStep wft = (ProcessStep)o;
            	boolean unsatisfied = wft.getQAstatus().stream()
                        .anyMatch(a -> a.getEvalResult() == false);
                boolean fulfilled = wft.getQAstatus().stream()
                        .anyMatch(a -> a.getEvalResult() == true); 
               Icon icon = getIcon(unsatisfied, fulfilled, wft.getDefinition().getQAConstraints().size());
           	   icon.setColor("grey");
               return icon;
            } else if (o instanceof ConstraintWrapper) {
            	return getQAIcon((ConstraintWrapper)o);
                //return infoDialog((ConstraintWrapper)o);
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setHeader("QA").setWidth("10%").setFlexGrow(0);
        
//        this.setItemDetailsRenderer(new ComponentRenderer<Component, ProcessInstanceScopedElement>(o -> {
//        	if (o instanceof ProcessInstance) {
//        		return infoDialog((ProcessInstance)o);                          
//            } else if (o instanceof ProcessStep) {
//            	return infoDialog((ProcessStep)o);
//            } else if (o instanceof ConstraintWrapper) {
//            	return infoDialog((ConstraintWrapper)o);
//            } else {
//                return new Label("");
//            }
//        }));
        
        
        
    }
    
    private Icon getIcon(boolean unsatisfied, boolean fulfilled, int nrConstr) {
        Icon icon;
        if (nrConstr <= 0) {
        	icon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
        	icon.setColor("grey");
        	icon.getElement().setProperty("title", "No QA constraints defined");
        } else if (unsatisfied && fulfilled) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
            icon.getElement().setProperty("title", "This contains unsatisfied and fulfilled QA constraints");
        } else if (unsatisfied) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
            icon.getElement().setProperty("title", "This contains unsatisfied QA constraints");
        } else if (fulfilled){
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
            icon.getElement().setProperty("title", "This contains fulfilled QA constraints");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            icon.getElement().setProperty("title", "QA Constraints not evaluated");
        }
        return icon;
    }
    
    private Icon getStepIcon(ProcessStep step) {
    	Icon icon = null;
    	State state = null;
    	if ( step.getExpectedLifecycleState().equals(State.AVAILABLE) || step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || step.getExpectedLifecycleState().equals(State.CANCELED) )
    		state = step.getExpectedLifecycleState();
    	else 
    		state = step.getActualLifecycleState();
        switch(state) {
        case ACTIVE:
        	icon = new Icon(VaadinIcon.UNLOCK);
			icon.setColor("green");
			break;
		case AVAILABLE:
			icon = new Icon(VaadinIcon.LOCK);
			icon.setColor("red");
			break;
		case CANCELED:
			icon = new Icon(VaadinIcon.FAST_FORWARD);
			icon.setColor("orange");
			break;
		case COMPLETED:
			icon = new Icon(VaadinIcon.CHECK);
			icon.setColor("green");
			break;
		case ENABLED:
			icon = new Icon(VaadinIcon.UNLOCK);
			icon.setColor("green");
			break;
		case NO_WORK_EXPECTED:
			icon = new Icon(VaadinIcon.BAN);
			icon.setColor("orange");
			break;
		default:
			icon = new Icon(VaadinIcon.ASTERISK);
			break;
        }
        
        StringBuffer sb = new StringBuffer("Lifecycle State is ");
        sb.append(StepLifecycleStateMapper.translateState(step.getActualLifecycleState()));
        icon.getStyle().set("cursor", "pointer");
        icon.getElement().setProperty("title", sb.toString());
        return icon;
    }
    
    public static Icon getQAIcon(ConstraintWrapper rebc) {
    	Icon icon;
        if (rebc.getCr() == null) {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("orange");
        } else if (rebc.getEvalResult()) {
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
        } else if (!rebc.getEvalResult()) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
        } else { // never reached
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            return icon;
        }
        return icon;
    } 
    
    public void setFilters(String id, String name, String focusedElementId) {
        //propertiesFilter = filter;
    	this.focusedElementId = focusedElementId;
    	idFilter = id;
        nameFilter = name;
        updateTreeGrid();
    }

    public void updateTreeGrid(Collection<ProcessInstance> content) {  	    	        
    	this.getSelectionModel().getFirstSelectedItem().ifPresent(el -> focusedElement = el); // remember last selection
    	List<ProcessInstance> changes = content.stream()
    			// we should not put subprocesses into main hierarchy as then we will duplicate some entries leading to an exception, 
    	    	// heuristic, if preDNI or postDNI are != null, then this is a subprocess
    		.filter(pi -> pi.getInDNI() == null && pi.getOutDNI() == null)
        	.filter(pi -> doHaveAccessRight(pi))
        	.peek(wfi -> this.content.put(wfi.getName(), wfi))
        	.collect(Collectors.toList());
        if (changes.size() > 0) {
        	updateTreeGrid();
        	if (focusedElement == null) // no prior selection
        		this.getSelectionModel().select(changes.get(0));
        }
    }
    
    private boolean doHaveAccessRight(ProcessInstance wfi)  { 
  		String inParam = wfi.getDefinition().getExpectedInput().keySet().iterator().next();
  		String artId = (String)wfi.getInput(inParam).iterator().next().getPropertyAsValue("id");
  		boolean authorized = reqDel.doAllowProcessInstantiation(artId);
  		return authorized;
 	 };
    

    public void removeWorkflow(String id) {
        ProcessInstance removed = this.content.remove(id);
        if (removed != null) {
        	//this.getDataProvider().refreshAll();
        	updateTreeGrid();
        }
        
    }

    private void updateTreeGrid() {

    	
        Predicate<ProcessInstance> predicate = wfi -> ( 
        		( nameFilter.equals("") 
        				|| wfi.getDefinition().getName().startsWith(nameFilter) 
        			|| (wfi.getName() != null && wfi.getName().startsWith(nameFilter))) 
        		&& 
        		( idFilter.equals("") 
        				|| wfi.getInstance().id().toString().equals(idFilter)		
        		)
        	);// &&
         //       ( wfi.getPropertiesReadOnly().size() == 0 || wfi.getPropertiesReadOnly().stream()
         //               .anyMatch(propertyEntry -> propertiesFilter.entrySet().stream()
         //                       .anyMatch(filterEntry -> propertyEntry.getKey().startsWith(filterEntry.getKey()) && propertyEntry.getValue().startsWith(filterEntry.getValue()) )) );
    //    if (SecurityContextHolder.getContext().getAuthentication() == null ) { // when called via PUSH        	
    //    	SecurityContextHolder.getContext().setAuthentication(authentication);
    //    }
       
        
        this.setItems(this.content.values().stream()
                        .filter(predicate)
                        .map(x->x),
                o -> {
                    if (o instanceof ProcessInstance) {
                        ProcessInstance wfi = (ProcessInstance) o;
                        if (wfi.getInstance().id().toString().equals(focusedElementId))
                        	this.focusedElement = wfi;
                        return wfi.getProcessSteps().stream()
                              //  .filter(wft -> !(wft.getType() instanceof NoOpTaskDefinition))
                        		.sorted(new StepComparator())
                        		.map(wft -> (ProcessInstanceScopedElement) wft);
                    } else if (o instanceof ProcessStep) {
                        ProcessStep wft = (ProcessStep) o;
                        if (wft.getInstance().id().toString().equals(focusedElementId))
                        	this.focusedElement = wft;
                        return wft.getQAstatus().stream().sorted(new ConstraintWrapperComparator()).map(x -> (ConstraintWrapper)x);
                    } else if (o instanceof ConstraintWrapper) { 
                    	ConstraintWrapper cw = (ConstraintWrapper) o;
                    	if (cw.getInstance().id().toString().equals(focusedElementId)) {
                        	this.focusedElement = cw;                        
                    	}
                    	return Stream.empty();
                    } else {
                        log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
                        return Stream.empty();
                    }
                });
        this.getDataProvider().refreshAll();        
        if (focusedElement != null) {
        	List<ProcessInstanceScopedElement> expandStack = expandElementStack(focusedElement);
        	Lists.reverse(expandStack).stream().forEach(element -> {
        		this.expand(element);            	
        	});
        	this.select(focusedElement);        	
        }
        Notification.show("Process list has been updated!");
    }
    
    private static class ConstraintWrapperComparator implements Comparator<ConstraintWrapper> {
		@Override
		public int compare(ConstraintWrapper o1, ConstraintWrapper o2) {
			return o1.getQaSpec().getOrderIndex().compareTo(o2.getQaSpec().getOrderIndex());
		}
    }
    
    private static class StepComparator implements Comparator<ProcessStep> {
		@Override
		public int compare(ProcessStep o1, ProcessStep o2) {
			return o1.getDefinition().getSpecOrderIndex().compareTo(o2.getDefinition().getSpecOrderIndex());
		}
    }

    private List<ProcessInstanceScopedElement> expandElementStack(ProcessInstanceScopedElement focusedElement) {
    	if (focusedElement == null)
    		return Collections.emptyList();
    	List<ProcessInstanceScopedElement> stack = new ArrayList<>();
    	stack.add(focusedElement);
    	if (focusedElement instanceof ProcessStep)
    		stack.addAll(expandElementStack(focusedElement.getProcess()));
    	else if (focusedElement instanceof ConstraintWrapper)
    		stack.addAll(expandElementStack(((ConstraintWrapper) focusedElement).getParentStep()));
    	return stack;
    }
    
    
   
}
