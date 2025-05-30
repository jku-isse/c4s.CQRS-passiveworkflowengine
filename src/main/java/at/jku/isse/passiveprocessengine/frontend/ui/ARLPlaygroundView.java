package at.jku.isse.passiveprocessengine.frontend.ui;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService.ResultEntry;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.botsupport.HumanReadableSchemaExtractor;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotRequest;
import at.jku.isse.passiveprocessengine.frontend.botsupport.OCLBot.BotResult;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="arl", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("OCL/ARL Playground")
@UIScope
//@SpringComponent
public class ARLPlaygroundView extends VerticalLayout  implements BeforeLeaveObserver /*implements PageConfigurator*/ {

	protected AtomicInteger counter = new AtomicInteger(0);

	protected RequestDelegate commandGateway;

	private Set<ResultEntry> result = new HashSet<>();
	private ListDataProvider<ResultEntry> dataProvider = new ListDataProvider<>(result);
	final private ConstraintTreeGrid evalTree;
	final private TextArea errorResultArea = new TextArea();
	final private TextArea arlArea = new TextArea();
	final private ComboBox<PPEInstanceType> instanceTypeComboBox = new ComboBox<>("Instance Type (rule starting point/context)");
	
	private Details schemaSelection;
	private final MultiSelectListBox<PPEInstanceType> relevantSchemaList = new MultiSelectListBox<>();

	private OCLBot oclBot;
	private PPEInstanceType lastUsedContext = null;
	private Set<PPEInstanceType> ruleRelevantContext = new HashSet<>();
	private BotResult lastResult = null;
	private Checkbox checkboxUseRule = new Checkbox(false);
	private Checkbox checkboxFetchIncomplete = new Checkbox(false);
	private MessageList botUI = new MessageList();
	private MessageInput input = new MessageInput();
	private Button copyButton = new Button("Copy OCL/ARL constraint");
	private Button runButton = new Button("Execute OCL/ARL constraint");
	// adapted from: https://github.com/samie/vaadin-openai-chat/blob/main/src/main/java/com/example/application/views/helloworld/ChatView.java
	// https://vaadin.com/blog/building-a-chatbot-in-vaadin-with-openai
	
	private String user;
	
	public ARLPlaygroundView(RequestDelegate commandGateway, OCLBot oclBot, SecurityService securityService) {
		this.commandGateway = commandGateway;
		this.oclBot = oclBot;		
		//       setSizeFull();
		setMargin(false);
		//  setPadding(false);
		evalTree = new ConstraintTreeGrid(commandGateway, true /*, this.getElement() */); 
		statePanel();
		user = securityService.getAuthenticatedUser() != null ? securityService.getAuthenticatedUser().getUsername() : "user";
	}

//	private void statePanel() {
//		this.add(new Span("Currently Not available"));
//	}
	private void statePanel() {
		VerticalLayout layout = this; // new VerticalLayout();

		
		Component arl = getARLEditorComponent();
		Component bot = getBotSupportComponent();
		SplitLayout splitLayout = new SplitLayout(arl, bot);
		if (commandGateway.getUiConfig().isARLBotSupportEnabled()) {
			splitLayout.setSplitterPosition(70);	
		} else {
			splitLayout.setSplitterPosition(100);
			bot.setVisible(false);
		}
        splitLayout.setSizeFull();    
		
		errorResultArea.setWidthFull();
		errorResultArea.setMinHeight("100px");
		errorResultArea.setLabel("Constraint Evaluation Feedback");
		errorResultArea.setVisible(false);

		Grid<ResultEntry> grid = new Grid<>();
		Grid.Column<ResultEntry> instColumn = grid.addComponentColumn(rge -> resultEntryToInstanceLink(rge)).setHeader("Instance").setResizable(true).setSortable(true);
		Grid.Column<ResultEntry> resColumn = grid.addColumn(rge -> rge.getResult().toString()).setHeader("Result").setResizable(true).setSortable(true);
		Grid.Column<ResultEntry> evalColumn = grid.addComponentColumn(rge -> resultEntryToEvalButton(rge)).setHeader("EvalTree").setResizable(true).setSortable(false);        	
		Grid.Column<ResultEntry> repairColumn = grid.addComponentColumn(rge -> resultEntryToButton(rge)).setHeader("RepairTree").setResizable(true).setSortable(true);
		grid.setAllRowsVisible(true);
		grid.setDataProvider(dataProvider);

		// text field to show error message or result
		layout.add(splitLayout, errorResultArea, grid);
		this.setAlignSelf(Alignment.STRETCH, splitLayout);
	}
//
	@Override
	public void beforeLeave(BeforeLeaveEvent event) {
		if (!arlArea.isEmpty()) {
			final ContinueNavigationAction action = event.postpone();			
			final Dialog dialog = getConfirmDialog("Are you sure you want to leave? ARL Constraint field content and message content will be cleared.", action);
			//final ConfirmDialog dialog = new ConfirmDialog();			
			// dialog.setText(msg);
			//dialog.setConfirmButton("Stay", e -> dialog.close());
			//dialog.setCancelButton("Leave", e -> action.proceed());
			//dialog.setCancelable(true);
			dialog.open();
		}
	}
	
	private Dialog getConfirmDialog(String msg, ContinueNavigationAction action) {
		final Dialog dialog = new Dialog();
		H3 header = new H3("Changes to OCL/ARL rule detected");
		Span text = new Span(msg);
		Button btnStay = new Button("Stay");
		btnStay.addClickListener(event -> dialog.close());
		Button btnLeave = new Button("Leave");
		btnLeave.addClickListener(event -> { dialog.close(); action.proceed();});
		
		HorizontalLayout buttonPart = new HorizontalLayout();
		buttonPart.add(btnStay, btnLeave);		
		buttonPart.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);		
		
		VerticalLayout layout = new VerticalLayout();
		layout.add(header, text, buttonPart);		
		layout.setAlignSelf(Alignment.STRETCH, text);
		
		dialog.add(layout);
		dialog.setMaxWidth("300px");		
		return dialog;
		
	}

	private Component getARLEditorComponent() {
		VerticalLayout layout = new VerticalLayout();
		// field to provide context: instance type
		List<PPEInstanceType> instTypes = commandGateway.getProcessContext().getSchemaRegistry().getAllNonDeletedInstanceTypes().stream()
				//.filter(iType -> !iType.isDeleted)
				.filter(type -> !(type instanceof RuleDefinition))
				.sorted(new InstanceTypeComparator())
				.collect(Collectors.toList());
		instanceTypeComboBox.setItems(instTypes);
		instanceTypeComboBox.setItemLabelGenerator(iType -> String.format("%s (DSid: %s ) ",iType.getName(), iType.getId().toString()));
		instanceTypeComboBox.setWidthFull();
		//instanceTypeComboBox.setMinWidth("100px");
		layout.add(instanceTypeComboBox);

		// Text field to display ARL rule

		arlArea.setWidthFull();
		arlArea.setMinHeight("200px");
		arlArea.setLabel("Constraint Definition");
		arlArea.setPlaceholder("OCL/ARL constraint here starting with 'self.' ");
		layout.add(arlArea);


		
		checkboxFetchIncomplete.setLabel("Fetch incompletely loaded artifacts? (may significantly increase evaluation duration!)");      
		checkboxFetchIncomplete.setClassName("medtext");

		Checkbox checkboxLimitToFetched = new Checkbox(true);
		checkboxLimitToFetched.setLabel("Show results for fully fetched artifacts only");        	
		checkboxLimitToFetched.setClassName("medtext");

		checkboxLimitToFetched.addValueChangeListener(e -> 
		dataProvider.refreshAll());
		dataProvider.addFilter(pe -> {
			if (!checkboxLimitToFetched.getValue()) {
				return true;
			} else {
				return InstanceView.isFullyFetched(pe.getContextInstance());
			}
		}
				);  

		// Button to send
		Button evalButton = new Button("Evaluate");
		evalButton.addClickListener(clickEvent -> {
			if (arlArea.getValue().length() < 5)
				Notification.show("Make sure to enter a non-empty OCL/ARL constraint!");
			else if (instanceTypeComboBox.getOptionalValue().isEmpty())
				Notification.show("Make sure to select an Instance Type!");
			else {     
				triggerEvaluation(instanceTypeComboBox.getValue(), arlArea.getValue(), checkboxFetchIncomplete.getValue() );
			}

		});
		HorizontalLayout fetchPart = new HorizontalLayout();
		fetchPart.add(checkboxLimitToFetched);
		fetchPart.add(checkboxFetchIncomplete);        	
		layout.add(fetchPart, evalButton);


		return layout;
	}
	
	private void triggerEvaluation(PPEInstanceType context, String rule, boolean doFetchIncomplete) {
		errorResultArea.setVisible(false);
		//grid.setItems(Collections.emptySet());
		result.clear();
		dataProvider.refreshAll(); 
		new Thread(() -> { 
			try {
				Set<ResultEntry> evalResult = null;
				if (doFetchIncomplete) {
					do {
						if (commandGateway.getProcessChangeListenerWrapper().foundLazyLoaded()) {
							this.getUI().get().access(() ->Notification.show("Constraint encountered lazy loaded artifact, fetching artifact and reevaluating constraint ..."));
							commandGateway.getProcessChangeListenerWrapper().batchFetchLazyLoaded();
						}
						evalResult = commandGateway.getRuleEvaluationService().evaluateTransientRule( 
								context,								
								rule);
					} while (commandGateway.getProcessChangeListenerWrapper().foundLazyLoaded());

				} else {
					evalResult = commandGateway.getRuleEvaluationService().evaluateTransientRule( 
							context,								
							rule);
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
					this.getUI().get().access(() -> { 
						result.clear();
						result.addAll(finalResult); 
						dataProvider.refreshAll(); 
					});
				}
			} catch (Exception e) {
				this.getUI().get().access(() -> {
					errorResultArea.setVisible(true);
					errorResultArea.setValue(e.getMessage());        						
				});
			}
		}).start();
	}
	
	
	private Component getBotSupportComponent() {		
		
		schemaSelection = new Details("Select relevant work item types", getSchemaList());
		
		checkboxUseRule.setLabel("Use OCL/ARL rule as basis for refinement");      
		checkboxUseRule.setClassName("medtext");
		Scroller scroller = new Scroller(botUI);		
		scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
		scroller.getStyle()
		.set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
		.set("padding", "var(--lumo-space-m)");
	
		copyButton.setEnabled(false);
		copyButton.addClickListener(clickEvent -> {    
			if (lastResult != null && lastResult.getOclRule() != null) {
				arlArea.setValue(lastResult.getOclRule());
			}
		});    
		
		runButton.setEnabled(false);
		runButton.addClickListener(clickEvent -> {    
			if (lastResult != null && lastResult.getOclRule() != null && instanceTypeComboBox.getValue() != null) {
				triggerEvaluation(instanceTypeComboBox.getValue(), lastResult.getOclRule(), checkboxFetchIncomplete.getValue());
			}
		});
	        
	    Button resetButton = new Button("Reset Bot");
		resetButton.addClickListener(clickEvent -> {    
	        oclBot.resetSession();
	        relevantSchemaList.deselectAll();
	        botUI.setItems(Collections.emptyList());
		});
		HorizontalLayout controls = new HorizontalLayout();
		controls.add(copyButton, runButton, resetButton);		
		
		input.addSubmitListener(this::onBotSubmit);
		VerticalLayout botLayout = new VerticalLayout();		
		botLayout.add(schemaSelection, checkboxUseRule, scroller, input, controls);
		botLayout.setHorizontalComponentAlignment(Alignment.STRETCH, scroller, input, checkboxUseRule, controls);
		botLayout.setPadding(true); // Leave some white space
		botLayout.setMargin(false);
		botLayout.setHeightFull(); // We maximize to window
		scroller.setMaxHeight("500px");
		botUI.setSizeFull(); // Chat takes most of the space
		input.setWidthFull(); // Full width only
		botUI.setMaxWidth("800px"); // Until to certain size
		input.setMaxWidth("800px"); // Until to certain size
				
		return botLayout;
	}

	private Component getSchemaList() {
		HorizontalLayout layout = new HorizontalLayout();
		List<PPEInstanceType> instTypes = commandGateway.getProcessContext().getSchemaRegistry().getAllNonDeletedInstanceTypes().stream()				
				.filter(type -> !(type instanceof RuleDefinition))
				.filter(type -> !type.getName().startsWith("ProcessStep")) //TODO nicer  filter out process steps
				.filter(type -> !type.getName().startsWith("ProcessDefinition")) //TODO nicer filtering out process
				.filter(type -> !type.getName().startsWith("ProcessInstance")) //TOD nicer filter out process 
				.sorted(new InstanceTypeComparator())
				.collect(Collectors.toList());
		relevantSchemaList.setItems(instTypes);
		//relevantSchemaList.setRenderer(iType -> String.format("%s (DSid: %s ) ",iType.getName(), iType.getId().toString()));
		relevantSchemaList.setWidthFull();
		
		//instanceTypeComboBox.setMinWidth("100px");
		layout.add(relevantSchemaList);
		return layout;
	}
	
	private void onBotSubmit(MessageInput.SubmitEvent submitEvent) {
		// Append an item (this will be overriden later when reply comes)
		List<MessageListItem> items = new ArrayList<>(botUI.getItems());
		MessageListItem inputItem = new MessageListItem(submitEvent.getValue(), Instant.now(), user);
		items.add(inputItem);
		botUI.setItems(items);

		// Query AIbot
		oclBot.sendAsync(augmentInputPrompt(submitEvent.getValue()))
			  .whenComplete((response, t) -> {
			getUI().get().access(() -> {
				lastResult = response;
				if (response.getOclRule() != null) {
					runButton.setEnabled(true);
					copyButton.setEnabled(true);
				} else {
					runButton.setEnabled(false);
					copyButton.setEnabled(false);
				}
				items.add(convertResult(response));
				botUI.setItems(items);				
			});
		});
	}

	private BotRequest augmentInputPrompt(String userRequest) {
		BotRequest request = null;
		String existingRule = checkboxUseRule.getValue() ? arlArea.getValue().trim() : null; // whether to include rule as basis for refinement
		Optional<PPEInstanceType> ctxType = instanceTypeComboBox.getOptionalValue();
		if (ctxType.isPresent()) {			
			String schema = getChangedOrNullSchemaAndReset(ctxType.get());
			request = new BotRequest(Instant.now(), user, userRequest, ctxType.get(), schema, existingRule );
		} else {
			lastUsedContext = null;
			request = new BotRequest(Instant.now(), user, userRequest, null, OCLBot.FORGET_SCHEMA, existingRule );
		}
		return request;
	}

	private String getChangedOrNullSchemaAndReset(PPEInstanceType currentSelectedContextType) {
		if (lastUsedContext == null || !lastUsedContext.equals(currentSelectedContextType) || hasRelevantSchemaChanged()) {
			HumanReadableSchemaExtractor extractor = new HumanReadableSchemaExtractor(commandGateway.getProcessContext().getSchemaRegistry());			
			var currentSelection = new HashSet<>(relevantSchemaList.getSelectedItems());
			currentSelection.add(currentSelectedContextType);
			Map<PPEInstanceType, List<PPEInstanceType>> clusters = extractor.clusterTypes(currentSelection);
			StringBuffer schemaStringSet = new StringBuffer();							
			clusters.entrySet().forEach(entry -> {
						var props = extractor.processSubgroup(entry.getKey(), entry.getValue());
						var schema = extractor.compileSchemaList(entry.getKey(),  entry.getValue(), props.getKey(), props.getValue());
						schemaStringSet.append(schema);
					});													
			lastUsedContext = currentSelectedContextType;
			ruleRelevantContext = currentSelection;
			return schemaStringSet.toString();
		} else
			return null;
	}

	private boolean hasRelevantSchemaChanged() {
		var currentSelection = relevantSchemaList.getSelectedItems();		
		return !(currentSelection.size() == ruleRelevantContext.size() && currentSelection.containsAll(ruleRelevantContext)); 
	}
	
	private MessageListItem convertResult(BotResult msg) {
		return new MessageListItem(msg.getBotResult(), msg.getTime(), msg.getRole());
	}	
	
	private Component resultEntryToInstanceLink(ResultEntry rge) {
		if (InstanceView.isFullyFetched(rge.getContextInstance())) {
			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instance/"+rge.getContextInstance().getId(), rge.getContextInstance().getName()));
		} else {
			return new Paragraph(new Anchor(ComponentUtils.getBaseUrl()+"/instance/"+rge.getContextInstance().getId(), rge.getContextInstance().getName()+" (not fully fetched)"));
		}
	}      

	private Component resultEntryToEvalButton(ResultEntry entry) {

		Button button = new Button("Show Eval", evt -> {
			Dialog dialog = new Dialog();
			dialog.setWidth("80%");
			dialog.setMaxHeight("80%");    			
			evalTree.updateGrid((EvaluationNode) entry.getEvalTreeRootNode(), null);    			
			evalTree.setAllRowsVisible(true);
			dialog.add(evalTree);
			dialog.open();
		});
		return button;
	}

	private Component resultEntryToButton(ResultEntry entry) {

		Button button = null;
		if (entry.getError() == null && entry.getResult() == false) {
			button = new Button("Show Repairs", evt -> {
				Dialog dialog = new Dialog();
				dialog.setWidth("80%");
				dialog.setMaxHeight("80%");
				RepairNode repairTree = (RepairNode) entry.getRepairTreeRootNode();
				RepairTreeGrid rtg = new RepairTreeGrid(null, rtf, commandGateway);
				rtg.initTreeGrid();
				rtg.updateConditionTreeGrid(repairTree, null);				
				rtg.setAllRowsVisible(true);
				dialog.add(rtg);
				dialog.open();
			});
		} else {
			button = new Button("Show Repairs");
			button.setEnabled(false);
		}
		return button;
	}

	public static class InstanceTypeComparator implements Comparator<PPEInstanceType> {

		@Override
		public int compare(PPEInstanceType o1, PPEInstanceType o2) {
			return o1.getName().compareTo(o2.getName());
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
