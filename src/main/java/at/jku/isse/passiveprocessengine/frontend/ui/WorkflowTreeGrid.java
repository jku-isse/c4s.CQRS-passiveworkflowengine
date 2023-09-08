package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.exception.RepairException;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.StepLifecycleStateMapper;
import at.jku.isse.passiveprocessengine.frontend.ui.utils.UIConfig;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private Authentication authentication;

    public WorkflowTreeGrid(RequestDelegate f) {
        this.reqDel = f;       
        content = new HashMap<>();
        nameFilter = ""; // default filter
        idFilter = "";
//        propertiesFilter = new HashMap<>();
//        propertiesFilter.put("", ""); // default filter
        this.conf = f.getUIConfig();
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
    }
    
    protected void injectRequestDelegate(RequestDelegate f) {
    	this.reqDel = f;
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
        	//wfi.getDefinition().setDescription("<ul><li>Inform participants about scope, review criteria, etc</li><li>Send work products to be reviewed to all participants</li><li>Schedule joint review</li><li>Set up mechanism to handle review outcomes</li></ul>");
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
            	reqDel.getMonitor().processDeleted(wfi, authentication != null ? authentication.getName() : null);
            	reqDel.deleteProcessInstance(wfi.getName());
            	updateTreeGrid();
        });
        delIcon.getElement().setProperty("title", "Remove this workflow");
        l.add(delIcon);
        if (!conf.isAnonymized())        
        	l.add(new Anchor("/instance/show?id="+wfi.getInstance().id(), "Internal Details"));                
        l.add(new Anchor("/processlogs/"+wfi.getInstance().id().value(), "JSON Event Log"));
        
        Button consBtn = new Button("Ensure Constraint State Consistency");
        consBtn.setClassName("med");
        consBtn.addClickListener(evt -> {
        	if (reqDel != null)
        		reqDel.ensureConstraintStatusConsistency(wfi);
        });
        l.add(consBtn);
        
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        Icon icon = getStepIcon(wfi);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> { 
        	reqDel.getMonitor().processViewed(wfi, authentication != null ? authentication.getName() : null);
        	dialog.open(); });
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
        icon.addClickListener(e -> { 
        	reqDel.getMonitor().stepViewed(wft, authentication != null ? authentication.getName() : null);
        	dialog.open();	
        });
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
    			
    			if (reqDel.doShowRepairs(getTopMostProcess(pStep)) ) {
    				if (crOpt.isPresent() && !crOpt.get().isConsistent()) {
    					try {    									    	        			        						    		
    						RepairNode repairTree = RuleService.repairTree(crOpt.get());
    						if (this.conf.doUseIntegratedEvalRepairTree()) {
    	        				ConstraintTreeGrid ctg = new ConstraintTreeGrid(reqDel);
    	        				EvaluationNode node = RuleService.evaluationTree(crOpt.get());
    	        				ctg.updateGrid(node, getTopMostProcess(getTopMostProcess(pStep)));        			
    	        				ctg.setHeightByRows(true);
    	        				ctg.setWidth("100%");
    	        				l.add(ctg); 
    	        			} else {
    	        				RepairTreeGrid rtg = new RepairTreeGrid(reqDel.getMonitor(), rtf, reqDel);
        						rtg.initTreeGrid();
        						rtg.updateConditionTreeGrid(repairTree, getTopMostProcess(pStep));    					    					
        					//	rtg.expandRecursively(repairTree.getChildren(), 3);
        						rtg.setHeightByRows(true);
        						rtg.setWidth("100%");
        						l.add(rtg);
    	        			}
    					} catch (RepairException e) {
    						l.add(new Paragraph(e.getMessage()));
    					}
    				} 
    			}
    			
    		});
    	}
    }
    
    public static ProcessInstance getTopMostProcess(ProcessStep step) {
    	if (step.getProcess() != null)
    		return getTopMostProcess(step.getProcess());
    	else if (step instanceof ProcessInstance) {
    		return (ProcessInstance) step;
    	} else
    		return null;
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
                ListItem li = new ListItem(entry.getKey() + " (" + entry.getValue().name() + ")");
                if (!isIn) {
                	wft.getDefinition().getInputToOutputMappingRules().entrySet().stream()
                		.filter(dmentry -> dmentry.getKey().equalsIgnoreCase(entry.getKey()))
                		.findAny().ifPresent(dmapentry -> li.setTitle(dmapentry.getValue()));
                }
                line.add(li);
                Set<Instance> artifactList = isIn ? wft.getInput(entry.getKey()) : wft.getOutput(entry.getKey());
                if (artifactList.size() == 1) {
                    Instance a = artifactList.iterator().next();
                    line.add(ComponentUtils.convertToResourceLinkWithBlankTarget(a));
                    line.add(getReloadIcon(a));
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
                        nestedLine.add(getReloadIcon(a));
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

	private Component getReloadIcon(Instance inst) {
		if (inst == null || !conf.doGenerateRefetchButtonsPerArtifact()) return new Paragraph("");
        Icon icon = new Icon(VaadinIcon.REFRESH);
		icon.getStyle().set("cursor", "pointer");
        icon.getElement().setProperty("title", "Refetch Artifact");
        icon.addClickListener(e -> { 
        	ArtifactIdentifier ai = reqDel.getProcessChangeListenerWrapper().getArtifactIdentifier(inst);
        	new Thread(() -> { 
        		try {
        			this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server", inst.name())));
        			reqDel.getArtifactResolver().get(ai, true);
        			this.getUI().get().access(() ->Notification.show(String.format("Fetching succeeded", inst.name())));
        		} catch (ProcessException e1) {
        			this.getUI().get().access(() ->Notification.show(String.format("Updating/Fetching Artifact %s from backend server failed: %s", inst.name(), e1.getMainMessage())));
        		}}
        			).start();
        });
        return icon;
	}
    
//    private Component deleteInOut(ProcessStep wft, boolean isIn, Map.Entry<String, ArtifactType> entry, IArtifact a) {
//        Icon icon = new Icon(VaadinIcon.TRASH);
//        icon.setColor("red");
//        icon.setSize("15px");
//        icon.getStyle().set("cursor", "pointer");
//        icon.getStyle().set("flex-shrink", "0");
//        // NO REMOVING FROM FRONTEND ANYMORE
////        icon.addClickListener(e -> {
////            if (isIn) {
////                f.apply(new RemoveInputCmd(wft.getWorkflow() == null ? wft.getId() : wft.getWorkflow().getName(), wft.getId(), a.getArtifactIdentifier(), entry.getKey()).setParentCauseRef(wft.getId()));
////            } else {
////                f.apply(new RemoveOutputCmd(wft.getWorkflow() == null ? wft.getId() : wft.getWorkflow().getName(), wft.getId(), a.getArtifactIdentifier(), entry.getKey()).setParentCauseRef(wft.getId()));
////            }
////        });
//        return icon;
//    }

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
        
        if ( reqDel.doShowRepairs(getTopMostProcess(rebc.getProcess()))) {
        	if (rebc.getEvalResult() == false  && rebc.getCr() != null) {
        		try {
        			RepairNode repairTree = RuleService.repairTree(rebc.getCr());        			
        			if (this.conf.doUseIntegratedEvalRepairTree()) {
        				ConstraintTreeGrid ctg = new ConstraintTreeGrid(reqDel);
        				EvaluationNode node = RuleService.evaluationTree(rebc.getCr());
        				ctg.updateGrid(node, getTopMostProcess(rebc.getProcess()));        			
        				ctg.setHeightByRows(true);
        				l.add(ctg); 
        			} else {
                			RepairTreeGrid rtg = new RepairTreeGrid(reqDel.getMonitor(), rtf, reqDel);
                			rtg.initTreeGrid();
                			rtg.updateConditionTreeGrid(repairTree, getTopMostProcess(rebc.getProcess()));                			
                			//rtg.expandRecursively(repairTree.getChildren(), 3);
                			l.add(rtg); 
        			}        			        			
        		} catch(Exception e) {
        			TextArea resultArea = new TextArea();
        			resultArea.setWidthFull();
        			resultArea.setMinHeight("100px");
        			resultArea.setLabel("Error evaluating Repairtree");
        			resultArea.setValue(e.toString());
        			l.add(resultArea);
        		}
        	}
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setMaxHeight("80%");

        Icon icon= getQAIcon(rebc);
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> { 
        	reqDel.getMonitor().constraintedViewed(rebc, authentication != null ? authentication.getName() : null);
        	dialog.open();	
        });
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
    
    public void setFilters(String id, String name, String focusedElementId) {
        //propertiesFilter = filter;
    	this.focusedElementId = focusedElementId;
    	idFilter = id;
        nameFilter = name;
        updateTreeGrid();
    }

    public void updateTreeGrid(Collection<ProcessInstance> content) {
        //this.content = new HashMap<>();
    	// we should not put subprocesses into main hierarchy as then we will duplicate some entries leading to an exception, 
    	// heuristic, if preDNI or postDNI are != null, then this is a subporcess    	
        content.stream()
        	.filter(pi -> pi.getInDNI() == null && pi.getOutDNI() == null)
        	.forEach(wfi -> this.content.put(wfi.getName(), wfi));
        updateTreeGrid();
    }

    public void updateTreeGrid(ProcessInstance wfi) {
    	// we should not put subprocesses into main hierarchy as then we will duplicate some entries leading to an exception, 
    	// heuristic, if preDNI or postDNI are != null, then this is a subporcess
        if (wfi.getInDNI() == null && wfi.getOutDNI() == null)
        	this.content.put(wfi.getName(), wfi);
        // we update in any case as the subprocess has changed
        updateTreeGrid();
    }

    public void removeWorkflow(String id) {
        ProcessInstance removed = this.content.remove(id);
        this.getDataProvider().refreshAll();
        //updateTreeGrid();
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
        if (SecurityContextHolder.getContext().getAuthentication() == null) // when called via PUSH
        	SecurityContextHolder.getContext().setAuthentication(authentication);
        Predicate<ProcessInstance> accessRight = wfi -> { 
        		String inParam = wfi.getDefinition().getExpectedInput().keySet().iterator().next();
        		String artId = (String)wfi.getInput(inParam).iterator().next().getPropertyAsValue("id");
        		boolean authorized = reqDel.doAllowProcessInstantiation(artId);
        		return authorized;
        };
        
        this.setItems(this.content.values().stream()
                        .filter(predicate)
                        .filter(accessRight)
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
    
    private static RepairTreeFilter rtf = new ProcessRepairTreeFilter();	
	
	private static class ProcessRepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction ra) {
			//FIXME: lets not suggest any repairs that cannot be navigated to in an external tool. 
			if (ra.getElement() == null) return false;
			Instance artifact = (Instance) ra.getElement();
			if (!artifact.hasProperty("html_url") || artifact.getPropertyAsValue("html_url") == null) return false;
			else
			return ra.getProperty() != null 
					&& !ra.getProperty().equalsIgnoreCase("workItemType") // FIXME: just for testing purposes
					&& !ra.getProperty().startsWith("out_") // no change to input or output --> WE do suggest as an info that it needs to come from somewhere else, other step
					&& !ra.getProperty().startsWith("in_")
					&& !ra.getProperty().equalsIgnoreCase("name"); // typically used to describe key or id outside of designspace
		
		}
		
	}
}
