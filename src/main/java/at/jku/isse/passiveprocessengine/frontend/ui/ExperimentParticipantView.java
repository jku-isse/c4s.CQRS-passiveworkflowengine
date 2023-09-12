package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence.TaskInfo;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequenceProvider;
import at.jku.isse.passiveprocessengine.frontend.rule.ARLPlaygroundEvaluator;
import at.jku.isse.passiveprocessengine.frontend.rule.ARLPlaygroundEvaluator.ResultEntry;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RefreshableComponent;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

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
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Route("exp")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Experiment Overview")
//@ConditionalOnExpression(value = "${enableExperimentMode:false}")
@UIScope
//@SpringComponent
public class ExperimentParticipantView extends VerticalLayout implements RefreshableComponent  /*implements PageConfigurator*/ {

	protected AtomicInteger counter = new AtomicInteger(0);

	protected RequestDelegate commandGateway;
	protected ExperimentSequenceProvider sequenceProvider;
	protected ExperimentSequence seq;

	private ListDataProvider<ExperimentSequence.TaskInfo> dataProvider;

	public ExperimentParticipantView(RequestDelegate commandGateway, ExperimentSequenceProvider sequenceProvider, SecurityService securityService) {
		this.commandGateway = commandGateway;
		setSizeFull();
		setMargin(false);
		setPadding(false);

		String pId = securityService.getAuthenticatedUser().getUsername();
		seq = sequenceProvider.getSequenceForParticipant(pId);
		if (seq == null) {
			seq = new ExperimentSequence(pId+" - no sequence configured for this user");
		}
		dataProvider = new ListDataProvider<>(seq.getSequence());

		AppHeader header = new AppHeader("Experiment Overview", securityService, commandGateway.getUIConfig());
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

	@Override
	public void refreshContent() {		
		VerticalLayout cur = statePanel();
		cur.setHeight("100%");

		Div pages = new Div(cur); //, snap, split
		pages.setHeight("97%");
		pages.setWidthFull();

		Image image = new Image("images/tim.png", "Traceability Information Model");		
		
		pageContent.removeAll();
		pageContent.setClassName("layout-style");
		pageContent.add(/*tabs,*/ pages, image);
	}


	private VerticalLayout statePanel() {
		VerticalLayout layout = new VerticalLayout();
		layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		layout.setFlexGrow(0);

		Label pLabel = new Label("Process/Task Sequence for Participant"+seq.getParticipantId());
		layout.add(pLabel);

		Grid<TaskInfo> grid = new Grid<>();
		Grid.Column<TaskInfo> procName = grid.addColumn(ti -> ti.getProcessId()).setHeader("Process/Task").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> procInput = grid.addComponentColumn(ti -> inputToProc(ti)).setHeader("Input").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> repair = grid.addColumn(ti -> ti.getRepairSupport()).setHeader("Config").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> init = grid.addComponentColumn(ti -> taskInfoToEvalButton(ti)).setHeader("Init").setResizable(false).setSortable(false);		
		grid.setDataProvider(dataProvider);	

		layout.add(grid);

		return layout;
	}

	private Component inputToProc(TaskInfo info) {
		return new Paragraph(new Anchor(info.getInputUrl(), info.getInputId()));
	}


	private Component taskInfoToEvalButton(TaskInfo entry) {

		Button button = null;
		String nextAllowedProc = commandGateway.isAllowedAsNextProc(entry.getProcessId(), seq.getParticipantId());
		if (commandGateway.doAllowProcessInstantiation(entry.getInputId()) && nextAllowedProc.equalsIgnoreCase(entry.getProcessId())) {
			button = new Button("Start Process/Task", evt -> {
				String id = entry.getInputId()+entry.getProcessId();
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				Notification.show("Process Instantiation might take some time. UI will be updated automatically upon success.");
				 
					try {
						Map<String, ArtifactIdentifier> inputs = new HashMap<>();
						inputs.put(entry.getInputParam(), new ArtifactIdentifier(entry.getInputId(), entry.getArtifactType(), entry.getIdType()));
						ProcessInstance pi = commandGateway.instantiateProcess(id, inputs, entry.getProcessId());						 
						if (auth != null && auth.getName() != null) {
							pi.getInstance().addOwner(new User(auth.getName()));
						}
						commandGateway.getMonitor().processCreated(pi, auth != null ? auth.getName() : null);
//						this.getUI().get().access(() -> { 
//							Notification.show("Successfully instantiated process"); 
//	//						dataProvider.refreshAll();
//						});
						UI.getCurrent().getPage().open("home", "_self");
					} catch (Exception e) { // importing an issue that is not present in the database will cause this exception (but also other nested exceptions)
						log.error("CommandExecutionException: " + e.getMessage());
						e.printStackTrace();
						this.getUI().get().access(() ->Notification.show("Creation failed! \r\n"+e.getMessage()));
					}					
			});
		} else {
			button = new Button("Start Process/Task");
			button.setEnabled(false);
		}
		return button;
	}



}
