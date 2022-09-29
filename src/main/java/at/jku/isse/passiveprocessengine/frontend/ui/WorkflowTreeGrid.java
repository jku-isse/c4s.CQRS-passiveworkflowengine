package at.jku.isse.passiveprocessengine.frontend.ui;

import artifactapi.ArtifactType;
import artifactapi.IArtifact;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.exception.RepairException;
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
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.text.html.HTML;

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
                Span span= new Span(wfi.getName());
                span.getElement().setProperty("title", wfi.getDefinition().getName() + " (" + wfi.getName() + ")");
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
        	if (o instanceof ProcessInstance) {
                return infoDialog((ProcessInstance)o);                                
            } else if (o instanceof ProcessStep) {
                return infoDialog((ProcessStep)o);
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
        		//return infoDialog((ProcessInstance)o);                                
            } else if (o instanceof ProcessStep) {
               ProcessStep wft = (ProcessStep)o;
            	boolean unsatisfied = wft.getQAstatus().stream()
                        .anyMatch(a -> a.getEvalResult() == false);
                boolean fulfilled = wft.getQAstatus().stream()
                        .anyMatch(a -> a.getEvalResult() == true); 
               Icon icon = getIcon(unsatisfied, fulfilled, wft.getDefinition().getQAConstraints().size());
           	   icon.setColor("grey");
               return icon;
            	//return infoDialog((ProcessStep)o);
            } else if (o instanceof ConstraintWrapper) {
                return infoDialog((ConstraintWrapper)o);
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setHeader("QA").setWidth("5%").setFlexGrow(0);
    }

    
    private Icon getStepIcon(ProcessStep step) {
    	boolean isPremature = (step.isInPrematureOperationModeDueTo().size() > 0);
        boolean isUnsafe = (step.isInUnsafeOperationModeDueTo().size() > 0);
        String color = (step.getExpectedLifecycleState().equals(step.getActualLifecycleState())) ? "green" : "orange";
        if (isPremature || isUnsafe)
        	color = "#E24C00"; //dark orange
    	Icon icon;
    	State state = null;
    	if ( step.getExpectedLifecycleState().equals(State.AVAILABLE) || step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || step.getExpectedLifecycleState().equals(State.CANCELED) )
    		state = step.getExpectedLifecycleState();
    	else 
    		state = step.getActualLifecycleState();
        switch(state) {
		case ACTIVE:
			icon = new Icon(VaadinIcon.SPARK_LINE);
			icon.setColor(color);
			break;
		case AVAILABLE:
			icon = new Icon(VaadinIcon.LOCK);
			icon.setColor("red");
			break;
		case CANCELED:
			icon = new Icon(VaadinIcon.FAST_FORWARD);
			icon.setColor(color);
			break;
		case COMPLETED:
			icon = new Icon(VaadinIcon.CHECK);
			icon.setColor(color);
			break;
		case ENABLED:
			icon = new Icon(VaadinIcon.UNLOCK);
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
        StringBuffer sb = new StringBuffer("Lifecycle State is ");
        if (isPremature)
        	sb.append("premature ");
        if (isPremature && isUnsafe)
        	sb.append("and ");
        if (isUnsafe)
        	sb.append("unsafe ");
        sb.append(StepLifecycleStateMapper.translateState(step.getActualLifecycleState()));
        //TODO: for now we don't inform about deviations
        //if (!step.getExpectedLifecycleState().equals(step.getActualLifecycleState()))        
        //	sb.append(" but expected "+step.getExpectedLifecycleState());
        icon.getStyle().set("cursor", "pointer");
        icon.getElement().setProperty("title", sb.toString());
        return icon;
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
        Paragraph p = new Paragraph("Process Instance ID:  "+wfi.getId());
        p.setClassName("info-header");
        l.add(p);
        H3 h3= new H3(wfi.getName());
        h3.setClassName("info-header");
        l.add(h3);
        if(wfi.getDefinition().getHtml_url()!=null)
        {
        	Anchor a =new Anchor(wfi.getDefinition().getHtml_url(),wfi.getDefinition().getHtml_url());
        	a.setClassName("info-header");
        	a.setTarget("_blank");
        	l.add(a);
        }
        if(wfi.getDefinition().getDescription()!=null)
        {
        	wfi.getDefinition().setDescription("<ul><li>Inform participants about scope, review criteria, etc</li><li>Send work products to be reviewed to all participants</li><li>Schedule joint review</li><li>Set up mechanism to handle review outcomes</li></ul>");
        	Html h=new Html("<span>"+wfi.getDefinition().getDescription()+"</span>");
        	l.add(h);
        }
       // infoDialogInputOutput(l, wfi.getInput(), wfi.getOutput(), wfi.getDefinition().getExpectedInput(), wfi.getDefinition().getExpectedOutput(), wfi);
        if (wfi.getActualLifecycleState() != null) {            
        	//TODO for now we only show actual state
        	//l.add(new Paragraph(String.format("Lifecycle State: %s (Expected) :: %s (Actual) ", wfi.getExpectedLifecycleState().name() , wfi.getActualLifecycleState().name())));
            l.add(new Paragraph("Process State: "+StepLifecycleStateMapper.translateState(wfi.getActualLifecycleState())));
        }
        augmentWithConditions(wfi, l);
        infoDialogInputOutput(l, wfi);
        augmentWithPrematureTriggerConditions(wfi, l);
        
        Icon delIcon = new Icon(VaadinIcon.TRASH);
        delIcon.setColor("red");
        delIcon.getStyle().set("cursor", "pointer");
        delIcon.addClickListener(e -> {
            if (reqDel != null)
            	reqDel.deleteProcessInstance(wfi.getName());
        });
        delIcon.getElement().setProperty("title", "Remove this workflow");
        l.add(delIcon);
        if (!MainView.anonymMode)        
        	l.add(new Anchor("/instance/show?id="+wfi.getInstance().id(), "Internal Details"));                
        l.add(new Anchor("/processlogs/"+wfi.getInstance().id().value(), "JSON Event Log"));
        
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        Icon icon = getStepIcon(wfi);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        //icon.getElement().setProperty("title", "Show more information about this process instance");
        dialog.add(l);

        return icon;
    }

    private Component infoDialog(ProcessStep wft) {
        VerticalLayout l = new VerticalLayout();
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Process Step ID:  " +wft.getId());
        p.setClassName("info-header");
        l.add(p);
        H3 h3= new H3(wft.getName());
        h3.setClassName("info-header");
        l.add(h3);
        if(wft.getDefinition().getHtml_url()!=null)
        {
        	Anchor a =new Anchor(wft.getDefinition().getHtml_url(),wft.getDefinition().getHtml_url());
        	a.setClassName("info-header");
        	a.setTarget("_blank");
        	l.add(a);
        }
        if(wft.getDefinition().getDescription()!=null)
        {
        	Html h=new Html("<span>"+wft.getDefinition().getDescription()+"</span>");
        	l.add(h);
        }
        if (wft.getActualLifecycleState() != null) {
         //TODO for now just actual state
        	//l.add(new Paragraph(String.format("Lifecycle State: %s (Actual) - %s (Expected)", wft.getActualLifecycleState().name(), wft.getExpectedLifecycleState().name())));
        	l.add(new Paragraph("Step State: "+StepLifecycleStateMapper.translateState(wft.getActualLifecycleState())));
        }
       
        
        augmentWithConditions(wft, l);
        augmentWithPrematureUnsafeMode(wft, l);
        infoDialogInputOutput(l,  wft);
        
        Dialog dialog = new Dialog();
        dialog.setMaxHeight("80%");
        dialog.setWidth("80%");

        Icon icon = getStepIcon(wft);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        //icon.getElement().setProperty("title", "Show more information about this process step");

        dialog.add(l);

        return icon;
    }

    private void augmentWithPrematureTriggerConditions(ProcessInstance pInst, VerticalLayout l) {
    	Map<String,String> premTriggers = pInst.getDefinition().getPrematureTriggers();
    	if (premTriggers.isEmpty()) return;
    	Grid<Map.Entry<String, String>> grid = new Grid<Map.Entry<String, String>>();
    	grid.setColumnReorderingAllowed(false);
    	Grid.Column<Map.Entry<String, String>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader("Step").setResizable(true).setSortable(true).setWidth("200px");
    	Grid.Column<Map.Entry<String, String>> valueColumn = grid.addColumn(p -> p.getValue()).setHeader("Premature Trigger Condition").setResizable(true);
    	grid.setItems(premTriggers.entrySet());
    	grid.setHeightByRows(true);
    	l.add(grid);
    }
    
    private void augmentWithPrematureUnsafeMode(ProcessStep pStep, VerticalLayout l) {
    	List<ProcessStep> unsafe = pStep.isInUnsafeOperationModeDueTo();
    	List<ProcessStep> prem = pStep.isInPrematureOperationModeDueTo();
    	if (!unsafe.isEmpty() || !prem.isEmpty()) {
    		HorizontalLayout list = new HorizontalLayout();
    		list.add(new H5("Upstream incomplete work"));
    		Icon icon = new Icon(VaadinIcon.EXCLAMATION);
    		icon.setColor("#E24C00");
    		list.add(new H5(icon));
    		list.add(new H5("Caution! continuing on this step could lead to rework or unnecessary work."));
    		l.add(list);
    	}
    	if (!unsafe.isEmpty()) {
    		Grid<ProcessStep> grid2 = new Grid<ProcessStep>();
    		grid2.setColumnReorderingAllowed(false);
    		Grid.Column<ProcessStep> nameColumn = grid2.addComponentColumn(p -> ComponentUtils.convertToResourceLinkWithBlankTarget(p.getInstance())).setHeader("Preceeding Steps with unfulfilled QA constraints").setResizable(true).setSortable(true).setWidth("400px");
    		//Grid.Column<ProcessStep> nameColumn = grid2.addColumn(p -> p.getDefinition().getName()).setHeader("Preceeding Steps with unfulfilled QA constraints").setResizable(true).setSortable(true).setWidth("400px");
    		//Grid.Column<ProcessStep> linkColumn = grid2.addComponentColumn(p -> ComponentUtils.convertToResourceLinkWithBlankTarget(p.getInstance())).setHeader("Step Link").setResizable(true);
    		grid2.setItems(unsafe);
    		grid2.setHeightByRows(true);
    		l.add(grid2);
    	}
    	if (!prem.isEmpty()) {
    		Grid<ProcessStep> grid = new Grid<ProcessStep>();
    		grid.setColumnReorderingAllowed(false);
    		Grid.Column<ProcessStep> nameColumn2 = grid.addComponentColumn(p -> ComponentUtils.convertToResourceLinkWithBlankTarget(p.getInstance())).setHeader("Preceeding Incomplete Steps").setResizable(true).setSortable(true).setWidth("400px");
    		//Grid.Column<ProcessStep> nameColumn2 = grid.addColumn(p -> p.getDefinition().getName()).setHeader("Preceeding Incomplete Steps").setResizable(true).setSortable(true).setWidth("400px");
    		//Grid.Column<ProcessStep> linkColumn2 = grid.addComponentColumn(p -> ComponentUtils.convertToResourceLinkWithBlankTarget(p.getInstance())).setHeader("Step Link").setResizable(true);
    		grid.setItems(prem);
    		grid.setHeightByRows(true);
    		l.add(grid);
    	}
    }
    
    private void augmentWithConditions(ProcessStep pStep, VerticalLayout l) {
    	for (Conditions cond : Conditions.values()) {
    		pStep.getDefinition().getCondition(cond).ifPresent(arl -> {
    			
    			HorizontalLayout line = new HorizontalLayout();
    			String strCond = cond.toString().substring(0,1)+cond.toString().substring(1).toLowerCase();
    			H5 h5cond = new H5(strCond+":");
    			h5cond.setTitle(arl);
    			line.add(h5cond);
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
    			//l.add(new Paragraph(arl));
    			
    			if (crOpt.isPresent() && !crOpt.get().isConsistent()) {
//    				HorizontalLayout h = new HorizontalLayout();
//    				h.setWidthFull();
//    				h.setMargin(false);
//    				h.setPadding(false);
    				try {
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
    				} catch (RepairException e) {
    					l.add(new Paragraph(e.getMessage()));
    				}
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
                  //  line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
                    list.add(line);
                } else if (artifactList.size() > 1) {
                  //  line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
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
                  //  line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
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
        l.setClassName("scrollable");
        Paragraph p = new Paragraph("Quality Assurance Constraint:");
        p.setClassName("info-header");
        l.add(p);
        H3 h3 = new H3(rebc.getQaSpec().getName());
        h3.setClassName("info-header");
        l.add(h3);
        
        HorizontalLayout line = new HorizontalLayout();
        
        H4 descr = new H4(rebc.getQaSpec().getHumanReadableDescription());
        descr.setTitle(rebc.getQaSpec().getQaConstraintSpec());
        line.add(descr);
        Icon icon1= getQAIcon(rebc);
        line.add(new H4(icon1));
        l.add(line);
        //l.add(new H4(rebc.getQaSpec().getQaConstraintSpec()));
        
        
        if (rebc.getEvalResult() == false  && rebc.getCr() != null) {
        	try {
        		RepairNode repairTree = RuleService.repairTree(rebc.getCr());
        		RepairTreeGrid rtg = new RepairTreeGrid();
        		rtg.initTreeGrid();
        		rtg.updateQAConstraintTreeGrid(repairTree);
        		rtg.setHeightByRows(true);
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

        Icon icon= getQAIcon(rebc);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show details");
        
        dialog.add(l);
        return icon;
    }

    private Icon getQAIcon(ConstraintWrapper rebc) {
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
                        		.sorted(new StepComparator())
                        		.map(wft -> (ProcessInstanceScopedElement) wft);
                    } else if (o instanceof ProcessStep) {
                        ProcessStep wft = (ProcessStep) o;
                        return wft.getQAstatus().stream().sorted(new ConstraintWrapperComparator()).map(x -> (ConstraintWrapper)x);
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
}
