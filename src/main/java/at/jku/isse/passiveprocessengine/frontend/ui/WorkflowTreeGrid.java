package at.jku.isse.passiveprocessengine.frontend.ui;

import artifactapi.ArtifactType;
import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Map<String, String> propertiesFilter;

    public WorkflowTreeGrid(RequestDelegate f) {
        this.reqDel = f;

        content = new HashMap<>();
        nameFilter = ""; // default filter
        propertiesFilter = new HashMap<>();
        propertiesFilter.put("", ""); // default filter
    }

    public void initTreeGrid() {

        // Column "Workflow Instance"
        this.addComponentHierarchyColumn(o -> {
            if (o instanceof ProcessInstance) {
                ProcessInstance wfi = (ProcessInstance) o;
                int i = wfi.getId().indexOf("WF-");
                String id = i < 0 ? wfi.getName() : wfi.getName().substring(0, i+2).concat("...").concat(wfi.getName().substring(wfi.getName().length()-5));
                Span span;
                //if (wfi.getName() != null) {
               //     span = new Span(wfi.getName() + " (" + id + ")");
                //} else {
                    span = new Span(wfi.getDefinition().getName() + " (" + id + ")");
               // }
                span.getElement().setProperty("title", wfi.getDefinition().getName() + " (" + wfi.getId() + ")");
                return span;
            } else if (o instanceof ProcessStep) {
                ProcessStep wft = (ProcessStep) o;
//                if (wft.getName() != null) {
//                    Span span = new Span(wft.getName());
//                    span.getElement().setProperty("title", wft.getDefinition().getName());
//                    return span;
//                } else {
                    Span span = new Span(wft.getDefinition().getName());
                    span.getElement().setProperty("title", wft.getDefinition().getName());
                    return span;
                //}
            } else if (o instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o;
                Span span = new Span(rebc.getQaSpec().getHumanReadableDescription());
                span.getElement().setProperty("title", rebc.getName());
                return span;
            } else if (o instanceof RepairNode) {
            	RepairNode rn = (RepairNode) o;
            	Span span = new Span(rn.toString());
                span.getElement().setProperty("title", rn.toString());
                return span;
            } else {
                return new Paragraph(o.getClass().getSimpleName() +": " + o.getName());
            }
        }).setHeader("Process Instance").setWidth("60%");

        // Column "Last Changed"

        this.addColumn(o -> {
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
            if (o instanceof ProcessStep) {
                ProcessStep wfi = (ProcessStep) o;
                String color = (wfi.getExpectedLifecycleState().equals(wfi.getActualLifecycleState())) ? "green" : "orange";
                Icon icon;
                switch(wfi.getExpectedLifecycleState()) {
				case ACTIVE:
					icon = new Icon(VaadinIcon.SPARK_LINE);
					icon.setColor(color);
					break;
				case AVAILABLE:
					icon = new Icon(VaadinIcon.LOCK);
					icon.setColor("grey");
					break;
				case CANCELED:
					icon = new Icon(VaadinIcon.EYE_SLASH);
					icon.setColor(color);
					break;
				case COMPLETED:
					icon = new Icon(VaadinIcon.CHECK);
					icon.setColor(color);
					break;
				case ENABLED:
					icon = new Icon(VaadinIcon.EYE);
					icon.setColor(color);
					break;
				case NO_WORK_EXPECTED:
					icon = new Icon(VaadinIcon.BAN);
					icon.setColor(color);
					break;
				default:
					icon = new Icon(VaadinIcon.ASTERISK);
					break;
                }

                icon.getStyle().set("cursor", "pointer");
               // getTestDialog(wfi, icon);
                icon.getElement().setProperty("title", "Workflow Lifecycle State: "+wfi.getActualLifecycleState());
                return icon;
            } else {
//                Icon icon = new Icon(VaadinIcon.ASTERISK);
//                icon.getStyle().set("cursor", "pointer");
//                getTestDialog(o, icon);
//                return icon;
            		return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setHeader("State").setWidth("10%").setFlexGrow(0);

        // Column "Unsatisfied" or "Fulfilled"

        this.addColumn(new ComponentRenderer<Component, ProcessInstanceScopedElement>(o -> {      	
        	if (o instanceof ProcessInstance) {
                return infoDialog((ProcessInstance)o);                                
            } else if (o instanceof ProcessStep) {
                return infoDialog((ProcessStep)o);
            } else if (o instanceof ConstraintWrapper) {
                return infoDialog((ConstraintWrapper)o);
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setHeader("QA").setWidth("5%").setFlexGrow(0);
    }

    private Icon getIcon(boolean unsatisfied, boolean fulfilled) {
        Icon icon;
        if (unsatisfied && fulfilled) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
            icon.getElement().setProperty("title", "This contains unsatisfied and fulfilled constraints");
        } else if (unsatisfied) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
            icon.getElement().setProperty("title", "This contains unsatisfied constraints");
        } else if (fulfilled){
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
            icon.getElement().setProperty("title", "This contains fulfilled constraints");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            icon.getElement().setProperty("title", "Constraints not evaluated");
        }
        return icon;
    }
    


    private Component infoDialog(ProcessInstance wfi) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Instance ID:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(wfi.getName());
        h3.setClassName("info-header");
        l.add(h3);

       // infoDialogInputOutput(l, wfi.getInput(), wfi.getOutput(), wfi.getDefinition().getExpectedInput(), wfi.getDefinition().getExpectedOutput(), wfi);
        if (wfi.getActualLifecycleState() != null)
            l.add(new Paragraph(String.format("Lifecycle State: %s (Expected) :: %s (Actual) ", wfi.getExpectedLifecycleState().name() , wfi.getActualLifecycleState().name())));
       
        augmentWithConditions(wfi, l);
        infoDialogInputOutput(l, wfi);
        
        Icon delIcon = new Icon(VaadinIcon.TRASH);
        delIcon.setColor("red");
        delIcon.getStyle().set("cursor", "pointer");
        delIcon.addClickListener(e -> {
            if (reqDel != null)
            	reqDel.deleteProcessInstance(wfi.getName());
        });
        delIcon.getElement().setProperty("title", "Remove this workflow");
        l.add(delIcon);
        
        
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        boolean unsatisfied = wfi.getProcessSteps().stream()
                .anyMatch(wft -> !wft.areQAconstraintsFulfilled());
        boolean fulfilled = wfi.getProcessSteps().stream()
                .anyMatch(wft -> wft.areQAconstraintsFulfilled());
       
        Icon icon = getIcon(unsatisfied, fulfilled);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this process instance");

        dialog.add(l);

        return icon;
    }

    private Component infoDialog(ProcessStep wft) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Step ID:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(wft.getDefinition().getName());
        h3.setClassName("info-header");
        l.add(h3);

        if (wft.getActualLifecycleState() != null)
            l.add(new Paragraph(String.format("Lifecycle State: %s (Actual) - %s (Expected)", wft.getActualLifecycleState().name(), wft.getExpectedLifecycleState().name())));
        augmentWithConditions(wft, l);
        infoDialogInputOutput(l,  wft);
        
        Dialog dialog = new Dialog();
        dialog.setMaxHeight("80%");
        dialog.setWidth("80%");

        boolean unsatisfied = wft.getQAstatus().stream()
                .anyMatch(a -> a.getEvalResult() == false);
        boolean fulfilled = wft.getQAstatus().stream()
                .anyMatch(a -> a.getEvalResult() == true); 
        Icon icon = getIcon(unsatisfied, fulfilled);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this process step");

        dialog.add(l);

        return icon;
    }

    private void augmentWithConditions(ProcessStep pStep, VerticalLayout l) {
    	for (Conditions cond : Conditions.values()) {
    		pStep.getDefinition().getCondition(cond).ifPresent(arl -> {
    			
    			HorizontalLayout line = new HorizontalLayout();
    			line.add(new H5(cond.toString()+":"));
    			// now fetch rule instance and check if fulfilled, if not, show repair tree:
    			Icon icon;
    			Optional<ConsistencyRule> crOpt = pStep.getConditionStatus(cond);
    			if (crOpt.isPresent()) {
    				if (crOpt.get().isConsistent()) {
    					icon = new Icon(VaadinIcon.CHECK_CIRCLE);
    					icon.setColor("green");
    				} else {
    					icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
    					icon.setColor("red");
    				}
    			} else {
    				icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
    				icon.setColor("grey");
    			}
    			line.add(new H5(icon));	
    			l.add(line);
    			l.add(new Paragraph(arl));
    			
    			if (crOpt.isPresent() && !crOpt.get().isConsistent()) {
//    				HorizontalLayout h = new HorizontalLayout();
//    				h.setWidthFull();
//    				h.setMargin(false);
//    				h.setPadding(false);
    				RepairNode repairTree = RuleService.repairTree(crOpt.get());
    				RepairTreeGrid rtg = new RepairTreeGrid();
    				rtg.initTreeGrid();
    				rtg.updateConditionTreeGrid(repairTree);
    				rtg.setHeightByRows(true);
//    				h.setClassName("const-margin");
//    				h.setWidthFull();
//    				h.add(rtg);
    				//Details details = new Details("Repair Instructions", rtg);
    				//details.setOpened(true);
    				//rtg.setClassName("width80");

    				l.add(rtg);
    			} 
    			
    		});
    	}
    }
    
    private Component addInOut(String title, ProcessStep wft, boolean isIn, String role, String type) {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setClassName("upload-background");

        TextField id = new TextField();
        id.setPlaceholder("Artifact ID");

        if (reqDel != null) {
        	Button submit = new Button(title, evt -> {
        		try {
        			if (wft instanceof ProcessStep) {
        				if (isIn) {
        					reqDel.addInput(wft.getProcess().getName(), wft.getId(), id.getValue(), role, type);
        					Notification.show(title + "-Request of artifact " + id.getValue() + " as input to process step submitted");
        				} else {
        					reqDel.addOutput(wft.getProcess().getName(), wft.getId(), id.getValue(), role, type);
        					Notification.show(title + "-Request of artifact " + id.getValue() + " as output to process step submitted");
        				}
        				Notification.show("Success");
        			} else if (wft instanceof ProcessInstance) {
        				if (isIn) {
        					reqDel.addInput(wft.getId(), null, id.getValue(), role, type);
        					Notification.show(title + "-Request of artifact " + id.getValue() + " as input to process submitted");
        				} else {
        					reqDel.addOutput(wft.getId(), null, id.getValue(), role, type);
        					Notification.show(title + "-Request of artifact " + id.getValue() + " as output to process submitted");
        				}
        				Notification.show("Success");
        			}
        		} catch (ProcessException e) {
        			Notification.show(e.getMessage());
        		}
        	});

        	hLayout.add(id, submit);
        }
        Details details = new Details(title, hLayout);
        details.addThemeVariants(DetailsVariant.SMALL);
        return details;
    }

    private void infoDialogInputOutput(VerticalLayout l, ProcessStep wft) {
        H5 h5 = new H5("Inputs");
        inOut(l, h5, expectedInOut(wft, wft.getDefinition().getExpectedInput(), true));

        H5 h51 = new H5("Outputs");
        inOut(l, h51, expectedInOut(wft, wft.getDefinition().getExpectedOutput(), false));
    }

    private void inOut(VerticalLayout l, H5 h41, Component expectedInOut) {
        //h41.setClassName("const-margin");
        l.add(h41);
        //VerticalLayout outLayout = new VerticalLayout();
        //outLayout.setClassName("card-border");
        //outLayout.add(new H5("Expected"));
        //outLayout.add(expectedInOut);
        //l.add(outLayout);
        l.add(expectedInOut);
    }

    private Component expectedInOut(ProcessStep wft, Map<String, InstanceType> io, boolean isIn) {
        UnorderedList list = new UnorderedList();
        list.setClassName("const-margin");
        if (io.size() > 0) {
            for (Map.Entry<String, InstanceType> entry : io.entrySet()) {
                HorizontalLayout line = new HorizontalLayout();
                line.setClassName("line");
                line.add(new ListItem(entry.getKey() + " (" + entry.getValue().name() + ")"));
                Set<Instance> artifactList = isIn ? wft.getInput(entry.getKey()) : wft.getOutput(entry.getKey());
                if (artifactList.size() == 1) {
                    Instance a = artifactList.iterator().next();
                    line.add(ComponentUtils.convertToResourceLinkWithBlankTarget(a));
                    // ADDING/DELETING NOT SUPPORTED CURRENTLY
                   // line.add(deleteInOut(wft, isIn, entry, a));
                    line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
                    list.add(line);
                } else if (artifactList.size() > 1) {
                    line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
                    list.add(line);
                    UnorderedList nestedList = new UnorderedList();
                    for (Instance a : artifactList) {
                        HorizontalLayout nestedLine = new HorizontalLayout();
                        nestedLine.setClassName("line");
                        nestedLine.add(new ListItem(ComponentUtils.convertToResourceLinkWithBlankTarget(a)));
                      //  nestedLine.add(deleteInOut(wft, isIn, entry, a));
                        nestedList.add(nestedLine);
                    }
                    list.add(nestedList);
                } else { // artifactList.size() == 0
                    Paragraph p = new Paragraph("none");
                    p.setClassName("red");
                    line.add(p);
                    line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
                    list.add(line);
                }
            }
        } else {
            ListItem li = new ListItem("nothing expected");
            li.setClassName("italic");
            list.add(li);
        }
        return list;
    }

    private Component deleteInOut(ProcessStep wft, boolean isIn, Map.Entry<String, ArtifactType> entry, IArtifact a) {
        Icon icon = new Icon(VaadinIcon.TRASH);
        icon.setColor("red");
        icon.setSize("15px");
        icon.getStyle().set("cursor", "pointer");
        icon.getStyle().set("flex-shrink", "0");
        // NO REMOVING FROM FRONTEND ANYMORE
//        icon.addClickListener(e -> {
//            if (isIn) {
//                f.apply(new RemoveInputCmd(wft.getWorkflow() == null ? wft.getId() : wft.getWorkflow().getName(), wft.getId(), a.getArtifactIdentifier(), entry.getKey()).setParentCauseRef(wft.getId()));
//            } else {
//                f.apply(new RemoveOutputCmd(wft.getWorkflow() == null ? wft.getId() : wft.getWorkflow().getName(), wft.getId(), a.getArtifactIdentifier(), entry.getKey()).setParentCauseRef(wft.getId()));
//            }
//        });
        return icon;
    }

    private Component infoDialog(ConstraintWrapper rebc) {
    	VerticalLayout l = new VerticalLayout();
        //l.setWidthFull();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Quality Assurance Constraint:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(rebc.getQaSpec().getName());
        h3.setClassName("info-header");
        l.add(h3);

        l.add(new H4(rebc.getQaSpec().getHumanReadableDescription()));
        l.add(new H4(rebc.getQaSpec().getQaConstraintSpec()));
        
        if (rebc.getEvalResult() == false  && rebc.getCr() != null) {
//        	HorizontalLayout h = new HorizontalLayout();
//        	h.setWidthFull();
//        	h.setMargin(false);
//        	h.setPadding(false);
        	try {
        		RepairNode repairTree = RuleService.repairTree(rebc.getCr());
        		RepairTreeGrid rtg = new RepairTreeGrid();
        		rtg.initTreeGrid();
        		rtg.updateQAConstraintTreeGrid(repairTree);
        		rtg.setHeightByRows(true);
        		//            h.setClassName("const-margin");
        		//            h.add(rtg);

        		l.add(rtg); 
            } catch(Exception e) {
            	TextArea resultArea = new TextArea();
            	resultArea.setWidthFull();
            	resultArea.setMinHeight("100px");
            	resultArea.setLabel("Error evaluating Repairtree");
            	resultArea.setValue(e.toString());
            	l.add(resultArea);
        	}
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        Icon icon;
        if (rebc.getCr() == null) {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            return icon;
//        } else if (fulfilledLinks.size() > 0 && unsatisfiedLinks.size() > 0) {
//            icon = new Icon(VaadinIcon.WARNING);
//            icon.setColor("#E24C00");
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
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show details");
        
        dialog.add(l);
        return icon;
    }
    
//    private Component getTestDialog(ProcessInstanceScopedElement step, Icon icon) {
//    	VerticalLayout l = new VerticalLayout();
//        //l.setWidthFull();
//        l.setClassName("scrollable");
//        Paragraph p = new Paragraph("Test:");
//        p.setClassName("info-header");
//        l.add(p);
//        H3 h3 = new H3(step.getName());
//        h3.setClassName("info-header");
//        l.add(h3);
//        l.add(new H4(step.getId()));
//        l.add(new H5(step.getInstance().getInstanceType().toString()));
//        
////        if (step instanceof ConstraintWrapper) {
////        	ConstraintWrapper rebc = (ConstraintWrapper)step;
////        	 if (rebc.getEvalResult() == false  && rebc.getCr() != null) {
//////             	HorizontalLayout h = new HorizontalLayout();
//////             	h.setWidthFull();
//////             	h.setMargin(false);
//////             	h.setPadding(false);
////             	RepairNode repairTree = RuleService.repairTree(rebc.getCr());
////         		RepairTreeGrid rtg = new RepairTreeGrid();
////                 rtg.initTreeGrid();
////                 rtg.updateQAConstraintTreeGrid(repairTree);
//////                 h.setClassName("const-margin");
//////                 h.add(rtg);
////                 
////                 l.add(rtg);
////             }
////        }
//        
//        
//        Dialog dialog = new Dialog();
//        dialog.setWidth("80%");
//        dialog.setMaxHeight("80%");
//        
//        icon.addClickListener(e -> dialog.open());
//        dialog.add(l);
//
//        return icon;
//    }
    
    
    

    public void setFilters(Map<String, String> filter, String name) {
        propertiesFilter = filter;
        nameFilter = name;
        updateTreeGrid();
    }

    public void updateTreeGrid(Collection<ProcessInstance> content) {
        //this.content = new HashMap<>();
        content.forEach(wfi -> this.content.put(wfi.getName(), wfi));
        updateTreeGrid();
    }

    public void updateTreeGrid(ProcessInstance wfi) {
        this.content.put(wfi.getName(), wfi);
        updateTreeGrid();
    }

    public void removeWorkflow(String id) {
        this.content.remove(id);
        updateTreeGrid();
    }

    private void updateTreeGrid() {
        Predicate<ProcessInstance> predicate = wfi -> ( nameFilter.equals("") 
        		|| wfi.getDefinition().getName().startsWith(nameFilter) 
        		|| (wfi.getName() != null && wfi.getName().startsWith(nameFilter)) );// &&
         //       ( wfi.getPropertiesReadOnly().size() == 0 || wfi.getPropertiesReadOnly().stream()
         //               .anyMatch(propertyEntry -> propertiesFilter.entrySet().stream()
         //                       .anyMatch(filterEntry -> propertyEntry.getKey().startsWith(filterEntry.getKey()) && propertyEntry.getValue().startsWith(filterEntry.getValue()) )) );
        this.setItems(this.content.values().stream()
                        .filter(predicate)
                        .map(x->x),
                o -> {
                    if (o instanceof ProcessInstance) {
                        ProcessInstance wfi = (ProcessInstance) o;
                        return wfi.getProcessSteps().stream()
                              //  .filter(wft -> !(wft.getType() instanceof NoOpTaskDefinition))
                                .map(wft -> (ProcessInstanceScopedElement) wft);
                    } else if (o instanceof ProcessStep) {
                        ProcessStep wft = (ProcessStep) o;
                        return wft.getQAstatus().stream().map(x -> (ConstraintWrapper)x);
                    } else if (o instanceof ConstraintWrapper) { 
                    	ConstraintWrapper cw = (ConstraintWrapper) o;
                    	return Stream.empty();
                    } else {
                        log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
                        return Stream.empty();
                    }
                });
        this.getDataProvider().refreshAll();
    }

}
