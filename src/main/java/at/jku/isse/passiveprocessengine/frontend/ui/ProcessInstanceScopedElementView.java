package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.exception.RepairException;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstanceScopedElementView extends VerticalLayout{

	private RequestDelegate reqDel; 
	private UIConfig conf;
	 private Authentication authentication;
	 private String loggedInUserNameOrNull = null;
	 private ProcessConfigBaseElementFactory configFactory;
	
	public ProcessInstanceScopedElementView(RequestDelegate f, ProcessConfigBaseElementFactory configFactory) {
		 this.reqDel = f;
		 this.conf = f.getUIConfig();
		 this.configFactory = configFactory;
		 this.authentication = SecurityContextHolder.getContext().getAuthentication();
		 loggedInUserNameOrNull = authentication != null ? authentication.getName() : null;
		 this.setMargin(false);
	}
	
	private void resetDetailsContent() {
		this.removeAll();
		this.setVisible(false);
	}
	
	protected void fillDetailsView(ProcessInstanceScopedElement el) {
		if (el != null) {
			this.removeAll();
    		this.setVisible(true);    	    		
			if (el instanceof ProcessInstance) {
				ProcessInstance wfi = (ProcessInstance)el;
				infoDialog(wfi);          		
        		reqDel.getMonitor().processViewed(wfi, loggedInUserNameOrNull);
            } else if (el instanceof ProcessStep) {
            	ProcessStep wft = (ProcessStep)el; 
            	infoDialog(wft);
            	reqDel.getMonitor().stepViewed(wft, loggedInUserNameOrNull);
            } else if (el instanceof ConstraintWrapper) {
            	ConstraintWrapper rebc = (ConstraintWrapper)el;
            	infoDialog(rebc);
            	reqDel.getMonitor().constraintedViewed(rebc, loggedInUserNameOrNull);
            } else {
                this.add(new Label(""));
            }
		} else {
			resetDetailsContent();
		}
			
	}
	
	
    private Icon getDetailedStepIcon(ProcessStep step) {
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
    

   
    private Component infoDialog(ProcessInstance wfi) {
        VerticalLayout l = getInfoHeader(wfi, "Process");
        augmentWithConditions(wfi, l);
        infoDialogInputOutput(l, wfi);
        augmentWithPrematureTriggerConditions(wfi, l);
        
        Icon delIcon = new Icon(VaadinIcon.TRASH);
        delIcon.setColor("red");
        delIcon.getStyle().set("cursor", "pointer");
        delIcon.addClickListener(e -> {
            if (reqDel != null)
            	reqDel.getMonitor().processDeleted(wfi, loggedInUserNameOrNull);
            	reqDel.deleteProcessInstance(wfi.getName());
            	//updateTreeGrid();
        });
        addConfigViewIfTopLevelProcess(l, wfi);
        
        delIcon.getElement().setProperty("title", "Remove this process");
        l.add(delIcon);
        if (!conf.isAnonymized() && !conf.isExperimentModeEnabled()) {        
        	Anchor a = new Anchor("/instance/"+wfi.getInstance().id(), "Internal Details (opens in new tab)");
        	a.setTarget("_blank");
        	l.add(a);                
        } 
        if(!conf.isExperimentModeEnabled()) {
        	Anchor a = new Anchor("/processlogs/"+wfi.getInstance().id().value(), "JSON Event Log (opens in new tab)");
        	a.setTarget("_blank");
        	l.add(a);
        }
//        Dialog dialog = new Dialog();
//        dialog.setWidth("80%");
//        dialog.setMaxHeight("80%");
//
//        Icon icon = getStepIcon(wfi);
//        icon.getStyle().set("cursor", "pointer");
//        icon.addClickListener(e -> { 
//        	reqDel.getMonitor().processViewed(wfi, authentication != null ? authentication.getName() : null);
//        	dialog.open(); });
//        dialog.add(l);
//
//        return icon;
        return l;
    }

    private Component infoDialog(ProcessStep wft) {
    	VerticalLayout l = getInfoHeader(wft, "Step");
        augmentWithConditions(wft, l);
        augmentWithPrematureUnsafeMode(wft, l);
        infoDialogInputOutput(l,  wft);
        
        return l;
    }

	private VerticalLayout getInfoHeader(ProcessStep step, String type) {
		VerticalLayout l = this;
       // l.setClassName("scrollable");
        Paragraph p = new Paragraph(type+" Instance ID:  "+step.getId());
        p.setClassName("info-header");
        l.add(p);
        H4 h4= new H4(step.getName());
        h4.setClassName("info-header");
        l.add(h4);
        if(step.getDefinition().getHtml_url()!=null)
        {
        	Anchor a =new Anchor(step.getDefinition().getHtml_url(),step.getDefinition().getHtml_url());
        	a.setClassName("info-header");
        	a.setTarget("_blank");
        	l.add(a);
        }
        if(step.getDefinition().getDescription()!=null)
        {
        	Html h=new Html("<span>"+step.getDefinition().getDescription()+"</span>");
        	l.add(h);
        }
       // infoDialogInputOutput(l, wfi.getInput(), wfi.getOutput(), wfi.getDefinition().getExpectedInput(), wfi.getDefinition().getExpectedOutput(), wfi);
        if (step.getActualLifecycleState() != null) {            
        	//TODO for now we only show actual state
        	//l.add(new Paragraph(String.format("Lifecycle State: %s (Expected) :: %s (Actual) ", wfi.getExpectedLifecycleState().name() , wfi.getActualLifecycleState().name())));
            l.add(new Span(type+" State: "+StepLifecycleStateMapper.translateState(step.getActualLifecycleState())));
        }
		return l;
	}
    
    private void augmentWithPrematureTriggerConditions(ProcessInstance pInst, VerticalLayout l) {
    	Map<String,String> premTriggers = pInst.getDefinition().getPrematureTriggers();
    	if (premTriggers.isEmpty()) return;
    	Grid<Map.Entry<String, String>> grid = new Grid<Map.Entry<String, String>>();
    	grid.setColumnReorderingAllowed(false);
    	Grid.Column<Map.Entry<String, String>> nameColumn = grid.addColumn(p -> p.getKey()).setHeader("Step").setResizable(true).setSortable(true).setWidth("200px");
    	Grid.Column<Map.Entry<String, String>> valueColumn = grid.addColumn(p -> p.getValue()).setHeader("Premature Trigger Condition").setResizable(true);
    	grid.setItems(premTriggers.entrySet());
    	grid.setAllRowsVisible(true);
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
    		grid2.setItems(unsafe);
    		grid2.setAllRowsVisible(true);
    		l.add(grid2);
    	}
    	if (!prem.isEmpty()) {
    		Grid<ProcessStep> grid = new Grid<ProcessStep>();
    		grid.setColumnReorderingAllowed(false);
    		Grid.Column<ProcessStep> nameColumn2 = grid.addComponentColumn(p -> ComponentUtils.convertToResourceLinkWithBlankTarget(p.getInstance())).setHeader("Preceeding Incomplete Steps").setResizable(true).setSortable(true).setWidth("400px");
    		grid.setItems(prem);
    		grid.setAllRowsVisible(true);
    		l.add(grid);
    	}
    }
    
    private void augmentWithConditions(ProcessStep pStep, VerticalLayout l) {
    	
    	for (Conditions cond : Conditions.values()) {
    		pStep.getDefinition().getCondition(cond).ifPresent(arl -> {
    			
    			HorizontalLayout line = new HorizontalLayout();
    			String strCond = cond.toString().substring(0,1)+cond.toString().substring(1).toLowerCase();
    			H5 h5cond = new H5(strCond+": ");
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
    			
    			if (reqDel.doShowRepairs(getTopMostProcess(pStep)) ) {
    				if (crOpt.isPresent() && !crOpt.get().isConsistent()) {
    					try {    									    	        			        						    		
    						RepairNode repairTree = RuleService.repairTree(crOpt.get());
    						if (this.conf.isIntegratedEvalRepairTreeEnabled()) {
    	        				ConstraintTreeGrid ctg = new ConstraintTreeGrid(reqDel /*, this.getElement()*/);
    	        				EvaluationNode node = RuleService.evaluationTree(crOpt.get());
    	        				ctg.updateGrid(node, getTopMostProcess(getTopMostProcess(pStep)));        			
    	        				ctg.setAllRowsVisible(true);
    	        				ctg.setWidth("100%");
    	        				l.add(ctg); 
    	        			} else {
    	        				RepairTreeGrid rtg = new RepairTreeGrid(reqDel.getMonitor(), rtf, reqDel);
        						rtg.initTreeGrid();
        						rtg.updateConditionTreeGrid(repairTree, getTopMostProcess(pStep));    					    					
        					//	rtg.expandRecursively(repairTree.getChildren(), 3);
        						rtg.setAllRowsVisible(true);
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
                    // first adding another element as input, then removal of this one allowed!! otherwise authorization will fail as no artifact is present to check with.
//                    if (isIn && !reqDel.getUIConfig().doEnableExperimentMode() && wft instanceof ProcessInstance && wft.getProcess() == null) { // input to a toplevel process
//                    	line.add(getDeleteInputArtifactFromProcessButton((ProcessInstance)wft, entry.getKey(), a.name()));
//                    }
                    list.add(line);
                } else if (artifactList.size() > 1) {
                    list.add(line);
                    UnorderedList nestedList = new UnorderedList();
                    for (Instance a : artifactList) {
                        HorizontalLayout nestedLine = new HorizontalLayout();
                        nestedLine.setAlignItems(Alignment.START);
                        nestedLine.setClassName("line");
                        nestedLine.add(new ListItem(ComponentUtils.convertToResourceLinkWithBlankTarget(a)));
                        nestedLine.add(getReloadIcon(a));
                        if (isIn && !reqDel.getUIConfig().isExperimentModeEnabled() && wft instanceof ProcessInstance && wft.getProcess() == null) { // input to a toplevel process
                        	nestedLine.add(getDeleteInputArtifactFromProcessButton((ProcessInstance)wft, entry.getKey(), a.name()));
                        }
                        nestedList.add(nestedLine);
                    }
                    list.add(nestedList);
                } else { // artifactList.size() == 0
                    Span p = new Span("none");
                    p.setClassName("red");
                    line.add(p);
                  //  line.add(addInOut("Add", wft, isIn, entry.getKey(), entry.getValue().name()));
                    list.add(line);
                }
                if (isIn && !reqDel.getUIConfig().isExperimentModeEnabled() && wft instanceof ProcessInstance && wft.getProcess() == null) { // input to a toplevel process
                	list.add(getInputComponent((ProcessInstance)wft, entry.getKey(), entry.getValue()));
                }
            }
        } else {
            ListItem li = new ListItem("nothing expected");
            li.setClassName("italic");
            list.add(li);
        }
        return list;
    }
    
    private Component getInputComponent(ProcessInstance pInst, String role, InstanceType artT) {
    	HorizontalLayout inputData = new HorizontalLayout();
        inputData.setMargin(false);               
        inputData.setPadding(false);
        inputData.setAlignItems(Alignment.START);
        
        TextField tf = new TextField();
        tf.setRequiredIndicatorVisible(true);
        tf.setMinWidth("300px");
        tf.setLabel(role);                
        tf.setHelperText(artT.name());
        inputData.add(tf);
        
        ComboBox<String> idTypeBox = new ComboBox<>("Identifier Type");
        List<String> idTypes = reqDel.getArtifactResolver().getIdentifierTypesForInstanceType(artT);
        idTypeBox.setItems(idTypes);
        idTypeBox.setValue(idTypes.get(0));
        idTypeBox.setAllowCustomValue(false);
        inputData.add(idTypeBox);
        
        Icon startIcon = new Icon(VaadinIcon.FILE_ADD);
        startIcon.getStyle()
	      .set("box-sizing", "border-box")
	      .set("margin-inline-end", "var(--lumo-space-m)")
	      .set("margin-inline-start", "var(--lumo-space-xs)")
	      .set("padding", "var(--lumo-space-xs)");
        Button addInputButton = new Button("Add", startIcon, evt -> {
        	String artId = tf.getValue().trim();
        	if (artId != null && artId.length() > 0) {
        		if (!reqDel.doAllowProcessInstantiation(artId)) {
        			Notification.show("You are not authorized to access the artifact used as process input - unable to add to process.");        			
        		} else {

        			Notification.show("Adding input might take some time. UI will be updated automatically upon success.");
        			new Thread(() -> { 
        				try {
        					String idType = idTypeBox.getOptionalValue().orElse(artT.name()); 
        					reqDel.addProcessInput(pInst, role, artId, idType) ;
        					this.getUI().get().access(() -> { 
        						Notification.show("Successfully added Artifact");
        						
        					});
        				} catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
        					log.error("Adding Artifact Exception: " + e.getMessage());
        					e.printStackTrace();
        					this.getUI().get().access(() ->Notification.show("Adding Artifact failed! \r\n"+e.getMessage()));
        				}
        			} ).start();
        		}
        	} else { 
        		Notification.show("Make sure to fill out the artifact identifier!");
        	}});
        addInputButton.getElement().getStyle().set("margin-top","37px");
        inputData.add(addInputButton);
        return inputData;
    }

	private Component getReloadIcon(Instance inst) {
		if (inst == null || !conf.isGenerateRefetchButtonsPerArtifactEnabled()) return new Span("");
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
    
    private Component getDeleteInputArtifactFromProcessButton(ProcessInstance wft, String param, String artId) {
        Icon icon = new Icon(VaadinIcon.TRASH);
        icon.setColor("red");
        icon.setSize("15px");
        icon.getStyle().set("cursor", "pointer");
        icon.getStyle().set("flex-shrink", "0");
        icon.addClickListener(e -> { 
            reqDel.removeInputFromProcess(wft, param, artId);
        });
        return icon;
    }

    private void addConfigViewIfTopLevelProcess(VerticalLayout detailsContent2, ProcessInstance step) {		
		if (step.getProcess() == null && !conf.isExperimentModeEnabled()) { // only a toplevel process has no ProcessDefinitio returned via getProcess 
			// see if a config is foreseen
			step.getDefinition().getExpectedInput().entrySet().stream()
			.filter(entry -> entry.getValue().isKindOf(configFactory.getBaseType()))
			.forEach(configEntry -> {
				//InstanceType procConfig = configEntry.getValue();//configFactory.getOrCreateProcessSpecificSubtype(configEntry.getKey(), (ProcessDefinition) step);		
				step.getInput(configEntry.getKey()).stream().forEach(config -> {
				
				detailsContent2.add(new Label(String.format("'%s' configuration properties:",configEntry.getKey())));
				Anchor a = new Anchor("/instance/"+config.id(), "View and Edit in Artifact/Instance Inspector (DSId: "+config.id()+")");
	        	a.setTarget("_blank");
	        	detailsContent2.add(a);  
				});
			});					
		}
	}
    
    private Component infoDialog(ConstraintWrapper rebc) {
    	VerticalLayout l = this;/*new VerticalLayout();    	
    	l.setMargin(false);
    	this.add(l);*/
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
        Icon icon1= WorkflowTreeGrid.getQAIcon(rebc);
        line.add(new H4(icon1));
        l.add(line);
        //l.add(new H4(rebc.getQaSpec().getQaConstraintSpec()));
        
        if ( reqDel.doShowRepairs(getTopMostProcess(rebc.getProcess()))) {
        	if (rebc.getCr() != null && this.conf.isIntegratedEvalRepairTreeEnabled()) {
        		try {
        			ConstraintTreeGrid ctg = new ConstraintTreeGrid(reqDel /*, this.getElement()*/);        			
    				EvaluationNode node = RuleService.evaluationTree(rebc.getCr());
    				ctg.updateGrid(node, getTopMostProcess(rebc.getProcess()));        			
    				ctg.setAllRowsVisible(true);    				
    				l.add(ctg); 
        		} catch(Exception e) {
        			e.printStackTrace();
        			TextArea resultArea = new TextArea();
        			resultArea.setWidthFull();
        			resultArea.setMinHeight("100px");
        			resultArea.setLabel("Error evaluating Repairtree");
        			resultArea.setValue(e.toString());
        			l.add(resultArea);
        		}
        	}
        	else 	
        		if (rebc.getEvalResult() == false  && rebc.getCr() != null && ! this.conf.isIntegratedEvalRepairTreeEnabled()) {
        			try {
        				RepairNode repairTree = RuleService.repairTree(rebc.getCr());        			

        				RepairTreeGrid rtg = new RepairTreeGrid(reqDel.getMonitor(), rtf, reqDel);
        				rtg.initTreeGrid();
        				rtg.updateConditionTreeGrid(repairTree, getTopMostProcess(rebc.getProcess()));                			
        				//rtg.expandRecursively(repairTree.getChildren(), 3);                			
        				l.add(rtg); 

        			} catch(Exception e) {
        				e.printStackTrace();
        				TextArea resultArea = new TextArea();
        				resultArea.setWidthFull();
        				resultArea.setMinHeight("100px");
        				resultArea.setLabel("Error evaluating Repairtree");
        				resultArea.setValue(e.toString());
        				l.add(resultArea);
        			}
        		}
        }
        
//        Dialog dialog = new Dialog();
//        dialog.setWidth("80%");
//        dialog.setMaxHeight("80%");
//
//        Icon icon= getQAIcon(rebc);
//        icon.getStyle().set("cursor", "pointer");
//        icon.addClickListener(e -> { 
//        	reqDel.getMonitor().constraintedViewed(rebc, loggedInUserNameOrNull);
//        	dialog.open();	
//        });
//        icon.getElement().setProperty("title", "Show details");
//        
//        dialog.add(l);
//        return icon;
        return l;
    }

       
    

    
    private static RepairTreeFilter rtf = new ProcessRepairTreeFilter();	
	
	private static class ProcessRepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction ra) {
			// lets not suggest any repairs that cannot be navigated to in an external tool. 
			if (ra.getElement() == null) return false;
			Instance artifact = (Instance) ra.getElement();
			if (!artifact.hasProperty("html_url") || artifact.getPropertyAsValue("html_url") == null) return false;
			else
			return ra.getProperty() != null 
					//&& !ra.getProperty().equalsIgnoreCase("workItemType") // now done via metaproperties
					&& !ra.getProperty().startsWith("out_") // no change to input or output --> WE do suggest as an info that it needs to come from somewhere else, other step
					&& !ra.getProperty().startsWith("in_")
					&& !ra.getProperty().equalsIgnoreCase("name")
					&& !ConsistencyUtils.isPropertyRepairable(artifact.getInstanceType(), ra.getProperty())
					; // typically used to describe key or id outside of designspace
		
		}
		
	}
	
}
