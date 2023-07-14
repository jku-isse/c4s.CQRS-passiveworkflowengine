package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.evaluator.RotationNode;
import at.jku.isse.designspace.rule.arl.expressions.IteratorExpression;
import at.jku.isse.designspace.rule.arl.expressions.LiteralExpression;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil.ReloadIconProvider;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;

@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
public class ConstraintTreeGrid extends TreeGrid<at.jku.isse.passiveprocessengine.frontend.ui.ConstraintTreeGrid.RotationOrRepair> implements ReloadIconProvider{
	

	RequestDelegate reqDel;
	ProcessInstance scope;
	private RepairVisualizationUtil repairViz;
	
	public ConstraintTreeGrid(RequestDelegate reqDel) {	
		this.reqDel = reqDel;
		this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		repairViz = new RepairVisualizationUtil(reqDel, this);
		initGrid();
	}
	
	private void initGrid() {
		this.addComponentHierarchyColumn(ror -> {
			return getExpression(ror);
		}).setHeader("Expression").setKey("expression");
		
		this.addColumn( ror -> {
			String exp = getExplanation(ror);
			return exp;
		}).setHeader("Explanation");
		
		this.getColumnByKey("expression").setClassNameGenerator(node -> {
			if (node.hasRepair())
				return "repair";
			else if (node.getRotationNode().isNodeOnRepairPath())
				return "warn";
			else 
				return null;
		});
	}

	private Component getExpression(RotationOrRepair ror) {
		if (ror.hasRotation()) {
			RotationNode rNode = ror.getRotationNode();
			RotationNode parent = rNode.getParent();
			String constraint = rNode.getNode().expression.getOriginalARL();
			String parentConstr = parent != null ? parent.getNode().expression.getOriginalARL() : "";
			String shortConstr = constraint;		
			if (constraint.startsWith(parentConstr)) 
				shortConstr = shortConstr.substring(parentConstr.length());
			Span span= new Span(shortConstr);
			span.setTitle(rNode.getNode().expression.getClass().getSimpleName());
			return span;
		} else {
			AbstractRepairAction rn = ror.getRepairAction();
			HorizontalLayout hl = new HorizontalLayout();
        	repairViz.nodeToDescription(rn, scope).stream().forEach(comp -> hl.add(comp));
            hl.getElement().setProperty("title", rn.toString());
            return hl;
		}
	}
	
	private String getExplanation(RotationOrRepair ror) {
		if (ror.hasRotation()) {
			RotationNode rNode = ror.getRotationNode();
			return rNode.getNode() != null ? Objects.toString(rNode.getNode().expression.explain(rNode.getNode())) : "";
		} else {
			return "";
		}
	}
	
	public void updateGrid(EvaluationNode node, ProcessInstance scope) {	
		this.scope = scope;
		RotationNode root = new RotationNode().buildInitialTree(node, RotationNode.ROOTROTATION);
		RotationNode newRoot = root.rotateLeftSideClockwise(RotationNode.ROOTROTATION);
		// lets move the original root repair (removing the context instance) to the new root.
		newRoot.getNode().getRepairs().addAll(root.getNode().getRepairs());
		root.getNode().getRepairs().clear();
		newRoot.isNodeOnRepairPath(); // ensure that any variable expressions rotated towards the root are correctly reflecting their repairpath participation
		RotationOrRepair ror = new RotationOrRepair(newRoot);
		this.setItems(Stream.of(ror), ror1 -> {
			return getChildElements(ror1);
		});
		this.getDataProvider().refreshAll();  
	}
	
	private Stream<RotationOrRepair> getChildElements(RotationOrRepair ror) {
		if (ror.hasRepair() )
			return Stream.empty(); //repairs dont have children
		else {
			RotationNode rn = ror.getRotationNode();
			if (rn.getNode() == null) return Stream.empty();
			return Stream.concat(
					Stream.concat(rn.getNode().getRepairs().stream()
							.map(rep -> new RotationOrRepair(rep)),
						rn.getLhs().stream()
							.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
							.filter(node -> node.getNode() != null) 
							.map(node -> new RotationOrRepair(node))),
			rn.getRhs().stream()
				.filter(node -> node.getNode() != null) 
				.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
				.map(node -> new RotationOrRepair(node)) );
					
		}
	}
	
	
	public Component getReloadIcon(Instance inst) {
		if (inst == null || !reqDel.getUIConfig().doGenerateRefetchButtonsPerArtifact()) return new Paragraph("");
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
	
	public static class RotationOrRepair {
		RotationNode rNode = null;
		AbstractRepairAction repair = null;
		
		RotationOrRepair(RotationNode rNode) {
			this.rNode = rNode;
			this.repair = null;
		}
		
		RotationOrRepair(AbstractRepairAction repair) {
			this.rNode = null;
			this.repair = repair;
		}
		
		boolean hasRotation() {
			return this.rNode != null;
		}
		
		boolean hasRepair() {
			return this.repair != null;
		}
		
		public RotationNode getRotationNode() {
			return rNode;
		}
		
		public AbstractRepairAction getRepairAction() {
			return repair;
		}
	}
	
}
