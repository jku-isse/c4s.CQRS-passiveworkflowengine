package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.registry.DeployResultPersistence;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;

@Route(value="deployfeedback", layout = AppView.class)
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
@CssImport(value="./styles/theme.css")
@PageTitle("Web Editor Deployment Feedback")
@UIScope
//@SpringComponent
public class DeployResultView extends VerticalLayout implements HasUrlParameter<String> {

	protected AtomicInteger counter = new AtomicInteger(0);

	protected RequestDelegate commandGateway;
	protected DeployResultPersistence tResultProvider;
	
	private ListDataProvider<ProcessDefinitionError> ddataProvider;
	private ListDataProvider<ProcessInstanceError> idataProvider;

	public DeployResultView(RequestDelegate commandGateway, DeployResultPersistence tResultProvider) {
		this.commandGateway = commandGateway;
	//	setSizeFull();
		setMargin(false);
		setPadding(false);
		this.tResultProvider = tResultProvider;
	}

	@Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String param) {       
		Location location = beforeEvent.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();
    	Map<String, List<String>> parametersMap = queryParameters.getParameters();
        String processName = parametersMap.getOrDefault("processName", List.of("")).get(0);
		if (processName == null)
        	setContent("");
        else 
        	setContent(processName);
    }
	
		
	public void setContent(String id) {		
		ProcessDeployResult result;
		if (id.equals("")) {
			result = tResultProvider.getLastResult();
		} else
			result = tResultProvider.getResult(id);		
		if (result != null) {
			ddataProvider = new ListDataProvider<>(result.getDefinitionErrors());
			idataProvider = new ListDataProvider<>(result.getInstanceErrors());
		} else {
			ddataProvider = new ListDataProvider<>(Collections.emptyList());
			idataProvider = new ListDataProvider<>(Collections.emptyList());
		}		
		VerticalLayout dList = deployPanel();
		dList.setHeight("100%");
		VerticalLayout iList = instancePanel();
		iList.setHeight("100%");
		
		this.add(dList, iList);
	}
	
	private Component generatePara(String content) {
		Span p = new Span(content);
		p.getStyle().set("white-space", "pre-line");
		return p;
	}
	
	private VerticalLayout deployPanel() {
		
		VerticalLayout layout = new VerticalLayout();
		//layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		//layout.setFlexGrow(0);

		Label pLabel = new Label("Deployment Feedback");
		layout.add(pLabel);

		Grid<ProcessDefinitionError> grid = new Grid<>();
		Grid.Column<ProcessDefinitionError> sev = grid.addColumn(ti -> "Error").setHeader("Severity").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(0);
		Grid.Column<ProcessDefinitionError> type = grid.addColumn(ti -> ti.getErrorType()).setHeader("Type").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(0);
		Grid.Column<ProcessDefinitionError> msg = grid.addComponentColumn(ti -> generatePara(ti.getErrorMsg())).setHeader("Message").setResizable(true).setSortable(true).setWidth("55%").setFlexGrow(4);
		Grid.Column<ProcessDefinitionError> link = grid.addComponentColumn(ti -> ComponentUtils.convertToResourceLinkWithBlankTarget(ti.getErrorScope().getInstance())).setHeader("Scope").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(1);
		grid.setDataProvider(ddataProvider);	
		grid.setAllRowsVisible(true);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		
		layout.add(grid);
		return layout;
	}


	private VerticalLayout instancePanel() {
		
		VerticalLayout layout = new VerticalLayout();
		//layout.setClassName("big-text");
		layout.setMargin(false);
		layout.setHeight("50%");
		layout.setWidthFull();
		//layout.setFlexGrow(0);

		Label pLabel = new Label("Process Re-Instantiation Feedback");
		layout.add(pLabel);

		Grid<ProcessInstanceError> grid = new Grid<>();
		Grid.Column<ProcessInstanceError> sev = grid.addColumn(ti -> "Error").setHeader("Severity").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(0);
		Grid.Column<ProcessInstanceError> type = grid.addColumn(ti -> ti.getErrorType()).setHeader("Type").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(0);
		Grid.Column<ProcessInstanceError> msg = grid.addComponentColumn(ti -> generatePara(ti.getErrorMsg())).setHeader("Message").setResizable(true).setSortable(true).setWidth("55%").setFlexGrow(4);
		Grid.Column<ProcessInstanceError> link = grid.addComponentColumn(ti -> ComponentUtils.convertToResourceLinkWithBlankTarget(ti.getErrorScope().getInstance())).setHeader("Scope").setResizable(true).setSortable(true).setWidth("15%").setFlexGrow(1);
		grid.setDataProvider(idataProvider);	
		grid.setAllRowsVisible(true);
		grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		
		layout.add(grid);
		return layout;
	}
	



}
