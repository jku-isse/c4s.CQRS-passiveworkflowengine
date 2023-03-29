package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.rule.ARLPlaygroundEvaluator;
import at.jku.isse.passiveprocessengine.frontend.rule.ARLPlaygroundEvaluator.ResultEntry;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RefreshableComponent;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Route("arl")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("ARL Playground")
@UIScope
//@SpringComponent
public class ARLPlaygroundView extends VerticalLayout implements RefreshableComponent  /*implements PageConfigurator*/ {

    protected AtomicInteger counter = new AtomicInteger(0);
    
    @Autowired
    protected RequestDelegate commandGateway;
    
    public ARLPlaygroundView(RequestDelegate commandGateway) {
    	this.commandGateway = commandGateway;
        setSizeFull();
        setMargin(false);
        setPadding(false);

//        HorizontalLayout header = new HorizontalLayout();
//        header.setClassName("header-theme");
//        header.setMargin(false);
//        header.setPadding(true);
//        header.setSizeFull();
//        header.setHeight("6%");
//        HorizontalLayout firstPart = new HorizontalLayout();
//        firstPart.setClassName("header-theme");
//        firstPart.setMargin(false);
//        firstPart.setPadding(true);
//        firstPart.setSizeFull();
//        firstPart.add(new Icon(VaadinIcon.CLUSTER), new Label(""), new Text("ARL Playground"));
//
//        ToggleButton toggle = new ToggleButton("Refresher ");
//        toggle.setClassName("med");
//        toggle.addValueChangeListener(evt -> {
////            if (devMode) {
////                Notification.show("Development mode enabled! Additional features activated.");
////            }
//            content();
//        });
//
//
//        header.add(firstPart, toggle/*, shutdown*/);
//        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        AppHeader header = new AppHeader("Constraint Editor - OCL/ARL Playground", this);
        AppFooter footer = new AppFooter(commandGateway.getUIConfig()); 
//        HorizontalLayout footer = new HorizontalLayout();
//        footer.setClassName("footer-theme");
//        if (MainView.anonymMode)        
//        	footer.add(new Text("(C) 2022 - Anonymized "));
//        else 
//        	footer.add(new Text("JKU - Institute for Software Systems Engineering"));

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
  
	@Override
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
        	// Text field to display ARL rule
        	TextArea arlArea = new TextArea();
            arlArea.setWidthFull();
            arlArea.setMinHeight("100px");
            arlArea.setLabel("Constraint Definition");
            arlArea.setPlaceholder("OCL/ARL constraint here starting with 'self.' ");
        	layout.add(arlArea);
        	
        	// test field to provide context: instance type
        	ComboBox<InstanceType> comboBox = new ComboBox<>("Instance Type");
        	List<InstanceType> instTypes = commandGateway.getWorkspace().debugInstanceTypes().stream()
        				.filter(iType -> !iType.isDeleted)
        				.sorted(new InstanceTypeComparator())
        				.collect(Collectors.toList());
        	comboBox.setItems(instTypes);
        	comboBox.setItemLabelGenerator(InstanceType::name);
        	comboBox.setMinWidth("800px");
        	layout.add(comboBox);
        	
        	
        	TextArea errorResultArea = new TextArea();
        	errorResultArea.setWidthFull();
        	errorResultArea.setMinHeight("100px");
        	errorResultArea.setLabel("Constraint Evaluation Feedback");
        	errorResultArea.setVisible(false);
        	
        	Grid<ResultEntry> grid = new Grid<>();
        	Grid.Column<ResultEntry> instColumn = grid.addComponentColumn(rge -> resultEntryToInstanceLink(rge)).setHeader("Instance").setResizable(true).setSortable(true);
        	Grid.Column<ResultEntry> resColumn = grid.addColumn(rge -> rge.getResult().toString()).setHeader("Result").setResizable(true).setSortable(true);
        	Grid.Column<ResultEntry> repairColumn = grid.addComponentColumn(rge -> resultEntryToButton(rge)).setHeader("RepairTree").setResizable(true).setSortable(true);
        	
        	
        	HorizontalLayout fetchPart = new HorizontalLayout();        	
        	Checkbox checkbox = new Checkbox(false);
        	checkbox.setLabel("Fetch lazy loaded artifacts? (may significantly increase evaluation duration!)");      
        	checkbox.setClassName("medtext");
        	// Button to send
        	Button button = new Button("Evaluate");
        	button.addClickListener(clickEvent -> {
        		if (arlArea.getValue().length() < 5)
        			Notification.show("Make sure to enter a non-empty OCL/ARL constraint!");
        		else if (comboBox.getOptionalValue().isEmpty())
        			Notification.show("Make sure to select an Instance Type!");
        		else {     
        			errorResultArea.setVisible(false);
        			new Thread(() -> { 
        				try {
        					Set<ResultEntry> evalResult = null;
        					if (checkbox.getValue()) {
        						do {
        							if (commandGateway.getProcessChangeListenerWrapper().foundLazyLoaded()) {
        								this.getUI().get().access(() ->Notification.show("Constraint encountered lazy loaded artifact, fetching artifact and reevaluating constraint ..."));
        								commandGateway.getProcessChangeListenerWrapper().batchFetchLazyLoaded();
        							}
        							evalResult = ARLPlaygroundEvaluator.evaluateRule(commandGateway.getWorkspace(), 
        									comboBox.getValue(),
        									"constraintplaygroundrule_"+counter.getAndIncrement(), 
        									arlArea.getValue(), false);
        						} while (commandGateway.getProcessChangeListenerWrapper().foundLazyLoaded());

        					} else {
        						evalResult = ARLPlaygroundEvaluator.evaluateRule(commandGateway.getWorkspace(), 
        								comboBox.getValue(),
        								"constraintplaygroundrule_"+counter.getAndIncrement(), 
        								arlArea.getValue(), false);
        					}
        					
        					this.getUI().get().access(() ->Notification.show("Constraint evaluated successfully."));
        					//						errorResultArea.setValue(evalResult.entrySet().stream()
        					//								.map(entry -> entry.getKey().name() + " evaluated to " +entry.getValue())
        					//								.collect(Collectors.joining("\r\n")));
        					final Set<ResultEntry> finalResult = evalResult;
        					if (evalResult.isEmpty()) {
        						//errorResultArea.setVisible(true);
        						this.getUI().get().access(() ->Notification.show("No instances available to evaluate OCL/ARL constraint on."));
        					} else {
        						this.getUI().get().access(() -> grid.setItems(finalResult));
        					}
        				} catch (ProcessException e) {
        					this.getUI().get().access(() -> {
        						errorResultArea.setVisible(true);
        						errorResultArea.setValue(e.getMessage());
        					});
        				}
        			}).start();
        		}
        	    
        	});
        	fetchPart.add(button);
        	fetchPart.add(checkbox);
        	layout.add(fetchPart);
        	// text field to show error message or result
        	layout.add(errorResultArea);
        	layout.add(grid);
        	
        }
        return layout;
    }
    
    private Component resultEntryToInstanceLink(ResultEntry rge) {
    	if (InstanceView.isFullyFetched(rge.getInstance())) {
    		return new Paragraph(new Anchor("/instance/show?id="+rge.getInstance().id(), rge.getInstance().name()));
    	} else {
    		return new Paragraph(new Anchor("/instance/show?id="+rge.getInstance().id(), rge.getInstance().name()+" (lazy loaded only)"));
    	}
    }
    
    private Component resultEntryToButton(ResultEntry entry) {
    	
    	Button button = null;
    	if (entry.getError() == null && entry.getResult() == false) {
    		button = new Button("Show Repairs", evt -> {
        		Dialog dialog = new Dialog();
    			dialog.setWidth("80%");
    			dialog.setMaxHeight("80%");
    			RepairNode repairTree = RuleService.repairTree(entry.getRuleInstance());
    			RepairTreeGrid rtg = new RepairTreeGrid(null, rtf, commandGateway);
    			rtg.initTreeGrid();
    			rtg.updateConditionTreeGrid(repairTree);
    			rtg.expandRecursively(repairTree.getChildren(), 3);
    			rtg.setHeightByRows(true);
    			dialog.add(rtg);
    			dialog.open();
    		});
    	} else {
    		button = new Button("Show Repairs");
    		button.setEnabled(false);
    	}
    	return button;
    }
    
    public static class InstanceTypeComparator implements Comparator<InstanceType> {

		@Override
		public int compare(InstanceType o1, InstanceType o2) {
			return o1.name().compareTo(o2.name());
		}
    	
    }
    
    private static RepairTreeFilter rtf = new NoOpRepairTreeFilter();	
	
	private static class NoOpRepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction ra) {
			return true;		
		}
		
	}


   
}
