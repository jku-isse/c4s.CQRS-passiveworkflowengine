package at.jku.isse.passiveprocessengine.frontend.ui;

import at.jku.isse.designspace.stagesexport.TransformDeployResult;
import at.jku.isse.designspace.stagesexport.TransformationError;
import at.jku.isse.designspace.stagesexport.rest.TransformDeployResultPersistence;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.security.SecurityService;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppFooter;
import at.jku.isse.passiveprocessengine.frontend.ui.components.AppHeader;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Route("stagesfeedback")
@Push
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Stages Transformation Feedback")
//@ConditionalOnExpression(value = "${stages.enabled:false}")
@UIScope
//@SpringComponent
public class StagesTransformationResultView extends VerticalLayout implements HasUrlParameter<String> {

	protected AtomicInteger counter = new AtomicInteger(0);

	protected RequestDelegate commandGateway;
	protected TransformDeployResultPersistence tResultProvider;

	private ListDataProvider<TransformationError> tdataProvider;
	private ListDataProvider<ProcessDefinitionError> ddataProvider;
	private ListDataProvider<ProcessInstanceError> idataProvider;

	public StagesTransformationResultView(RequestDelegate commandGateway, TransformDeployResultPersistence tResultProvider, SecurityService securityService) {
		this.commandGateway = commandGateway;
		setSizeFull();
		setMargin(false);
		setPadding(false);

		this.tResultProvider = tResultProvider;
				
		AppHeader header = new AppHeader("Stages Transformation Feedback", securityService, commandGateway.getUIConfig());
		AppFooter footer = new AppFooter(commandGateway.getUIConfig()); 

		add(
				header,
				main(),
				footer
				);
	}

	@Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        // example link: http://localhost:8080/home/?key=DEMO-9&value=Task
        Location location = beforeEvent.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        String id = parametersMap.getOrDefault("id", List.of("")).get(0);        
        refreshContent(id);
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
		refreshContent("");
		return pageContent;
	}
		
	public void refreshContent(String id) {		
		TransformDeployResult result;
		if (id.equals("")) {
			result = tResultProvider.getLastResult();
		} else
			result = tResultProvider.getResult(id);		
		tdataProvider = new ListDataProvider<>(result != null ? result.getTransformErrors() : Collections.emptyList());
		if (result != null) {
			tdataProvider = new ListDataProvider<>(result.getTransformErrors());
			ddataProvider = new ListDataProvider<>(result.getDeployResults().stream().flatMap(dr -> dr.getDefinitionErrors().stream()).collect(Collectors.toList()));
			idataProvider = new ListDataProvider<>(result.getDeployResults().stream().flatMap(dr -> dr.getInstanceErrors().stream()).collect(Collectors.toList()));
		} else {
			tdataProvider = new ListDataProvider<>(Collections.emptyList());
			ddataProvider = new ListDataProvider<>(Collections.emptyList());
			idataProvider = new ListDataProvider<>(Collections.emptyList());
		}
		VerticalLayout tList = transformPanel();
		tList.setHeight("100%");
		VerticalLayout dList = deployPanel();
		dList.setHeight("100%");
		VerticalLayout iList = instancePanel();
		iList.setHeight("100%");
		
		Div pages = new Div(tList, dList, iList); //, snap, split
		pages.setHeight("97%");
		pages.setWidthFull();

		pageContent.removeAll();
		pageContent.setClassName("layout-style");
		pageContent.add(/*tabs,*/ pages);
	}


	private VerticalLayout transformPanel() {
				
		VerticalLayout layout = new VerticalLayout();
		layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		layout.setFlexGrow(0);

		Label pLabel = new Label("Transformation Feedback");
		layout.add(pLabel);

		Grid<TransformationError> grid = new Grid<>();
		Grid.Column<TransformationError> sev = grid.addColumn(ti -> ti.getSeverity()).setHeader("Severity").setResizable(true).setSortable(true);
		Grid.Column<TransformationError> type = grid.addColumn(ti -> ti.getErrorType()).setHeader("Type").setResizable(true).setSortable(true);
		Grid.Column<TransformationError> msg = grid.addComponentColumn(ti -> generatePara(ti.getErrorMsg())).setHeader("Message").setResizable(true).setSortable(true);
		Grid.Column<TransformationError> link = grid.addComponentColumn(ti -> generateAnchor(ti)).setHeader("Scope").setResizable(true).setSortable(true);
		grid.setDataProvider(tdataProvider);	

		layout.add(grid);

		return layout;
	}
	
	private Component generatePara(String content) {
		Paragraph p = new Paragraph(content);
		p.getStyle().set("white-space", "pre-line");
		return p;
	}
	
	private Component generateAnchor(TransformationError te) {
		Anchor a = new Anchor(te.getStagesElementURL(), te.getStagesElementName());
		a.setTarget("_blank");
		return a;
	}

	private VerticalLayout deployPanel() {
		
		VerticalLayout layout = new VerticalLayout();
		layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		layout.setFlexGrow(0);

		Label pLabel = new Label("Deployment Feedback");
		layout.add(pLabel);

		Grid<ProcessDefinitionError> grid = new Grid<>();
		Grid.Column<ProcessDefinitionError> sev = grid.addColumn(ti -> "Error").setHeader("Severity").setResizable(true).setSortable(true);
		Grid.Column<ProcessDefinitionError> type = grid.addColumn(ti -> ti.getErrorType()).setHeader("Type").setResizable(true).setSortable(true);
		Grid.Column<ProcessDefinitionError> msg = grid.addComponentColumn(ti -> generatePara(ti.getErrorMsg())).setHeader("Message").setResizable(true).setSortable(true);
		Grid.Column<ProcessDefinitionError> link = grid.addComponentColumn(ti -> ComponentUtils.convertToResourceLinkWithBlankTarget(ti.getErrorScope().getInstance())).setHeader("Scope").setResizable(true).setSortable(true);
		grid.setDataProvider(ddataProvider);	

		layout.add(grid);

		return layout;
	}


	private VerticalLayout instancePanel() {
		
		VerticalLayout layout = new VerticalLayout();
		layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		layout.setFlexGrow(0);

		Label pLabel = new Label("Process Re-Instantiation Feedback");
		layout.add(pLabel);

		Grid<ProcessInstanceError> grid = new Grid<>();
		Grid.Column<ProcessInstanceError> sev = grid.addColumn(ti -> "Error").setHeader("Severity").setResizable(true).setSortable(true);
		Grid.Column<ProcessInstanceError> type = grid.addColumn(ti -> ti.getErrorType()).setHeader("Type").setResizable(true).setSortable(true);
		Grid.Column<ProcessInstanceError> msg = grid.addComponentColumn(ti -> generatePara(ti.getErrorMsg())).setHeader("Message").setResizable(true).setSortable(true);
		Grid.Column<ProcessInstanceError> link = grid.addComponentColumn(ti -> ComponentUtils.convertToResourceLinkWithBlankTarget(ti.getErrorScope().getInstance())).setHeader("Scope").setResizable(true).setSortable(true);
		grid.setDataProvider(idataProvider);	

		layout.add(grid);

		return layout;
	}
	



}
