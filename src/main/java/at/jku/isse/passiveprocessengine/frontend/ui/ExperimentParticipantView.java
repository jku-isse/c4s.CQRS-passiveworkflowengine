package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequence.TaskInfo;
import at.jku.isse.passiveprocessengine.frontend.experiment.ExperimentSequenceProvider;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value="exp", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Experiment Overview")
//@ConditionalOnExpression(value = "${enableExperimentMode:false}")
@UIScope
//@SpringComponent
public class ExperimentParticipantView extends VerticalLayout  {

	protected AtomicInteger counter = new AtomicInteger(0);

	protected RequestDelegate commandGateway;
	protected ExperimentSequenceProvider sequenceProvider;
	protected ExperimentSequence seq;
	protected String participantId;
	private ListDataProvider<ExperimentSequence.TaskInfo> dataProvider;

	public ExperimentParticipantView(RequestDelegate commandGateway, ExperimentSequenceProvider sequenceProvider, SecurityService securityService) {
		this.commandGateway = commandGateway;
	//	setSizeFull();
		setMargin(false);
		setPadding(false);

		participantId = securityService.getAuthenticatedUser().getUsername();
		seq = sequenceProvider.getSequenceForParticipant(participantId);
		if (seq == null) {
			seq = new ExperimentSequence(participantId+" - no sequence configured for this user");
		}
		dataProvider = new ListDataProvider<>(seq.getSequence());
		Image image = new Image("images/tim.png", "Traceability Information Model");	
		add(statePanel(), image);
		
	}

	private VerticalLayout statePanel() {
		VerticalLayout layout = new VerticalLayout();
//		layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		layout.setFlexGrow(0);

		Label pLabel = new Label("Process/Task sequence for participant "+seq.getParticipantId());
		layout.add(pLabel);

		Grid<TaskInfo> grid = new Grid<>();
		Grid.Column<TaskInfo> procName = grid.addColumn(ti -> ti.getProcessId()).setHeader("Process/Task").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> procInput = grid.addComponentColumn(ti -> inputToProc(ti)).setHeader("Input").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> repair = grid.addColumn(ti -> ti.getRepairSupport()).setHeader("Config").setResizable(true).setSortable(false);
		Grid.Column<TaskInfo> init = grid.addComponentColumn(ti -> taskInfoToEvalButton(ti)).setHeader("Start Task/Process").setResizable(false).setSortable(false);	
		Grid.Column<TaskInfo> stop = grid.addComponentColumn(ti -> taskInfoToCompletionButton(ti)).setHeader("Finish Task/Process").setResizable(false).setSortable(false);	
		grid.setDataProvider(dataProvider);	
		grid.setAllRowsVisible(true);
		layout.add(grid);

		return layout;
	}

	private Component inputToProc(TaskInfo info) {
		Anchor a = new Anchor(info.getInputUrl(), info.getInputId());
		a.setTarget("_blank");
		return new Paragraph(a);
	}


	private Component taskInfoToEvalButton(TaskInfo entry) {

		Button button = null;
		String nextAllowedProc = commandGateway.isAllowedAsNextProc(entry.getProcessId(), seq.getParticipantId());
		if (commandGateway.doAllowProcessInstantiation(entry.getInputId()) && nextAllowedProc.equalsIgnoreCase(entry.getProcessId())) {
			button = new Button("Start", evt -> {
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
			button = new Button("Start");
			button.setEnabled(false);
		}
		return button;
	}

	private Component taskInfoToCompletionButton(TaskInfo entry) {
		Button delButton = null;
		Optional<Instance> optInst = commandGateway.findAnyProcessInstanceByDefinitionAndOwner(entry.getProcessId(), participantId);
		if (optInst.isPresent()) {
			Instance procInst = optInst.get();
			if (!procInst.isDeleted) {
				ProcessInstance wfi = WrapperCache.getWrappedInstance(ProcessInstance.class, procInst);
				delButton = new Button("Finish", evt ->  {
					commandGateway.getMonitor().processDeleted(wfi, participantId);
					commandGateway.deleteProcessInstance(wfi.getName());
					dataProvider.refreshAll();
				});
				return delButton;
			}
		} 
		delButton = new Button("Finish");
		delButton.setEnabled(false);
		return delButton;
	}

	

}
