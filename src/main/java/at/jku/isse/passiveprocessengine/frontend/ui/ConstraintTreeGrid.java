package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.evaluator.RotationNode;
import at.jku.isse.designspace.rule.arl.expressions.AsTypeExpression;
import at.jku.isse.designspace.rule.arl.expressions.ExistsExpression;
import at.jku.isse.designspace.rule.arl.expressions.Expression;
import at.jku.isse.designspace.rule.arl.expressions.ForAllExpression;
import at.jku.isse.designspace.rule.arl.expressions.LiteralExpression;
import at.jku.isse.designspace.rule.arl.expressions.OperationCallExpression;
import at.jku.isse.designspace.rule.arl.expressions.TypeExpression;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil.ReloadIconProvider;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CssImport(value="./styles/grid-styles.css", themeFor="vaadin-grid")
public class ConstraintTreeGrid extends TreeGrid<RotationNode> implements ReloadIconProvider{
	

	RequestDelegate reqDel;
	ProcessInstance scope;
	private RepairVisualizationUtil repairViz;
	private boolean doShowRepairs;
//	private Element parentToNotify;
	
	public ConstraintTreeGrid(RequestDelegate reqDel, boolean doShowRepairs /*, Element parentToNotify */) {	
		this.reqDel = reqDel;
		this.doShowRepairs = doShowRepairs;
//		this.parentToNotify = parentToNotify;
		this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		repairViz = new RepairVisualizationUtil(reqDel, this);
		initGrid();
	}		
	
	private void initGrid() {
		this.addComponentHierarchyColumn(ror -> {
			return getExpression(ror);
		}).setHeader("Expression").setKey("expression").setResizable(true).setFlexGrow(3);
		this.getColumnByKey("expression").setClassNameGenerator(node -> {
			if (node.isNodeOnRepairPath()) {
				if (getVisibleRepairs(node).isEmpty())
					return "warn";
				else 
					return "repairable";
			} else 
				return null;
		});
		
		this.addColumn(createDefaultValueRenderer()).setHeader("Details").setResizable(true).setFlexGrow(1);		
		
		this.setItemDetailsRenderer(createDetailedValueRenderer());
		
		//with split view no longer an issue
		// needed due to bug: https://github.com/vaadin/flow-components/issues/1492
		// to recalculate scollbar on outer grid when this dynamically changes its size
//		this.addExpandListener(evt -> {
//			this.parentToNotify.executeJs("this.notifyResize()");
//		});
//		this.addCollapseListener(evt -> {
//			this.parentToNotify.executeJs("this.notifyResize()");
//		});
	}
	
	public void updateGrid(EvaluationNode node, ProcessInstance scope) {	
		this.scope = scope;
		RotationNode root = new RotationNode(node, RotationNode.ROOTROTATION);
		RotationNode newRoot = root.rotateLeftSideClockwise(RotationNode.ROOTROTATION);
		// lets move the original root repair (i.e. removing the context instance) to the new root.
		newRoot.getNode().getRepairs().addAll(root.getNode().getRepairs());
		root.getNode().getRepairs().clear();
		newRoot.isNodeOnRepairPath(); // ensure that any variable expressions rotated towards the root are correctly reflecting their repairpath participation
		
		List<RotationNode> topNodes = new LinkedList<>(); 
		collectFlattenedNodes(topNodes, newRoot);			
		this.setItems( topNodes.stream()
				.filter(rNode -> doShowNode(rNode))
				.map(rNode -> rNode) , rNode -> {
			return getSemiflatChildElements(rNode);
		});
		this.getDataProvider().refreshAll();  
	}
	
	private List<AbstractRepairAction> getVisibleRepairs(RotationNode node) {
		if (node.getNode().getRepairs().isEmpty()) {
			return Collections.emptyList();
		} else {
			return node.getNode().getRepairs();
		} 
	}

	private Component getExpression(RotationNode ror) {

			RotationNode rNode = ror;
			RotationNode parent = rNode.getParent();
			String constraint = rNode.getNode().expression.getOriginalARL(0, false);
			String parentConstr = (parent != null && parent != RotationNode.ROOTROTATION) ? parent.getNode().expression.getOriginalARL(0, false) : "";
			String shortConstr = constraint;		
			if (constraint.startsWith(parentConstr)) 
				shortConstr = shortConstr.substring(parentConstr.length());
			//Span para = new Span(shortConstr.trim());
			Span para = new Span(rNode.getNode().expression.getLocalARL());
			para.setTitle(shortConstr.trim());
			para.getStyle().set("white-space", "pre");
			return para;

	}
	
	private ComponentRenderer<Span, RotationNode> createDetailedValueRenderer() {
    	return new ComponentRenderer<>(Span::new, detailsBiConsumer);
    }
    
	private final SerializableBiConsumer<Span, RotationNode> detailsBiConsumer = (span, rNode) -> {
		Expression expr = rNode.getNode().expression;
		if (rNode.isCollectionOrCombinationNode() 
				&& !rNode.isCombinationNode() 
				&& !(expr instanceof ForAllExpression) 
				&& !(expr instanceof ExistsExpression)) {
			Object coll = rNode.getNode().resultValue;
			span.add(collectionValueToComponent((Collection) coll));
		} 
		else {
				Object expl = rNode.getNode().expression.explain(rNode.getNode());					
				if (expl instanceof Map) {				
					span.add(InstanceView.mapValueToComponent((Map)expl));
				}     	
				else  if (expl instanceof Collection) {
					span.add(collectionValueToComponent((Collection) expl));
				} 
				else if (rNode.getNode().getRepairs().isEmpty()) {
						span.add(singleValueToComponent(expl));
				}	
		}
		if (doShowRepairs) {
			augmentWithRepairComponent(rNode.getNode(), span);
		}
	}; 
    
	private void augmentWithRepairComponent(EvaluationNode node, Span span) {
		for (AbstractRepairAction rn : node.getRepairs()) {
			HorizontalLayout hl = new HorizontalLayout();
			repairViz.nodeToDescription(rn, scope).stream().forEach(comp -> hl.add(comp));
			hl.getElement().setProperty("title", rn.toString());
			hl.setClassName("repair");
			span.add(hl);
		}
	}
    
	private static ComponentRenderer<Span, RotationNode> createDefaultValueRenderer() {
    	return new ComponentRenderer<>(Span::new, defaultBiConsumer);
    }
    
	private static final SerializableBiConsumer<Span, RotationNode> defaultBiConsumer = (span, rNode) -> {
			Object expl = rNode.getNode().expression.explain(rNode.getNode());					
			if (expl instanceof Map) {				
				span.add(String.format("having %s entries", ((Map)expl).size()));
			}     	
			else  if (expl instanceof Collection) {
				span.add(String.format("having %s entries", ((Collection)expl).size()));
			} 
			else span.add(singleValueToComponent(expl));
		
    }; 
    
    private static Component singleValueToComponent(Object value) {
    	if (value instanceof Instance) {
    		Instance inst = (Instance)value;
    		return new Span(ComponentUtils.convertToResourceLinkWithBlankTarget(inst));
    	} else if (value instanceof InstanceType) {
        		InstanceType inst = (InstanceType)value;
        		return new Span(ComponentUtils.convertToResourceLinkWithBlankTarget(inst));
        } else
    	return new Span(Objects.toString(value));
    }
    
    private static Component collectionValueToComponent(Collection value) {
    	if (value == null || value.size() == 0)	
    		return new Span( "[ ]");
    	else if (value.size() == 1)
    		return singleValueToComponent(value.iterator().next());
    	else {    		
    		VerticalLayout vLayout = new VerticalLayout();
    		vLayout.setPadding(false);
    		vLayout.setMargin(false);
    		//UnorderedList list = new UnorderedList();    		
    		//list.setClassName("no-padding");
    		value.stream().forEach(val -> vLayout.add(singleValueToComponent(val)));
    		//if (value instanceof Set) // we sort the entries in the set for easier readability, for lists, we maintain the list order
    		//	list.
    		//vLayout.add(list);
    		return vLayout;
    	}
    }
		
	private Stream<RotationNode> getSemiflatChildElements(RotationNode rn) {
		// we indent when the parent item is a quantifier, or an combinator (AND, OR, XOR)
		// to avoid indentation, we need to fetch also child elements (which can only be one then)
			if (rn.getNode() == null) return Stream.empty();
			if (rn.isCollectionOrCombinationNode())	 {
				// then we have to provide children			, the default behavior
				// unless
				Stream<RotationNode> rhsStream = null;
				if (!rn.isCombinationNode()) { //thus a collection node,  hence we wont continue on rhs
					rhsStream = Stream.empty();
				} else {
					rhsStream = rn.getRhs().stream()
					.filter(node -> node.getNode() != null) 
					.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
					//.map(node -> new RotationOrRepair(node))
					;
				}
				return Stream.concat(
						rn.getLhs().stream()
						.filter(node -> node.getNode() != null)
						.filter(node -> !(node.getNode().expression instanceof LiteralExpression))	
						,
						rhsStream );
			}
			else if (
					(!rn.getParent().equals(RotationNode.ROOTROTATION) 
					&& rn.getParent().isCombinationNode() ) // either is a combination node, then do both
					|| 
					( !rn.getParent().equals(RotationNode.ROOTROTATION) // or a collection node, then only if left hand side
							&& rn.getParent().isCollectionOrCombinationNode()
							&& !rn.getParent().isCombinationNode()
							&& rn.getParent().getLhs().contains(rn) )
					) {
				// we have to provide flattened children, but only if this is in lefthand side of parent
				return Stream.concat(
						rn.getLhs().stream()
								.filter(node -> node.getNode() != null)	
								.filter(node -> !(node.getNode().expression instanceof LiteralExpression))														
								.flatMap(node -> {
									List<RotationNode> flattenedNodes = new LinkedList<>(); 
									collectFlattenedNodes(flattenedNodes, node);									
									return flattenedNodes.stream().filter(rNode -> doShowNode(rNode));
								})
								,
						rn.getRhs().stream()
						.filter(node -> node.getNode() != null) 
						.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
						.flatMap(node -> {
							List<RotationNode> flattenedNodes = new LinkedList<>(); 
							collectFlattenedNodes(flattenedNodes, node);							
							return flattenedNodes.stream().filter(rNode -> doShowNode(rNode));
						})
						 );
			}
			else { // just return repairs
				return Stream.empty();
			}
	}
	
	private void collectFlattenedNodes(List<RotationNode> flattened, RotationNode currentNode) {		
		flattened.add(currentNode);
		if (currentNode.isCollectionOrCombinationNode() ) {
			if ( !currentNode.isCombinationNode()) {
			 //thus a collection node,  hence we continue only on rhs which should only be one child
				currentNode.getRhs().stream()
				.filter(node -> node.getNode() != null)
				.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
				.findFirst()			
				.ifPresent(childNode -> collectFlattenedNodes(flattened, childNode));
			} // else: a combination node that needs indentation hence children not added here
		} else { // we can only have a single child in lhs or rhs (but never both), we dont know which one
			currentNode.getLhs().stream()
			.filter(node -> node.getNode() != null)
			.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
			.findFirst()
			.ifPresent(childNode -> collectFlattenedNodes(flattened, childNode));
			
			currentNode.getRhs().stream()
			.filter(node -> node.getNode() != null)
			.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
			.findFirst()			
			.ifPresent(childNode -> collectFlattenedNodes(flattened, childNode));
		}
	}
	
//	private Stream<RotationNode> getChildElements(RotationNode rn) {
//		return Stream.concat(
//				rn.getLhs().stream()
//				.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
//				.filter(node -> node.getNode() != null) 
//				,
//				rn.getRhs().stream()
//				.filter(node -> node.getNode() != null) 
//				.filter(node -> !(node.getNode().expression instanceof LiteralExpression))
//
//				);
//	}
	
	
	public Component getReloadIcon(Instance inst) {
		if (inst == null || !reqDel.getUIConfig().isGenerateRefetchButtonsPerArtifactEnabled()) return new Span("");
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
	
	private boolean doShowNode(RotationNode node) {
		// we dont want to show: asType, toSet, toList, ...
		Expression expr = node.getNode().expression;
		if (expr instanceof AsTypeExpression || expr instanceof TypeExpression)
			return false;
		if (expr instanceof OperationCallExpression) {
			String op = ((OperationCallExpression) expr).getOperation();
			switch(op.toLowerCase()) {
			case("asset"):
			case("aslist"):
			case("tointeger"):
			case("toreal"):
			case("toboolean"):
				return false;
			default:
				return true;
			}
		} else
			return true;
	}
	
//	public static class RotationOrRepair {
//		RotationNode rNode = null;
//		AbstractRepairAction repair = null;
//		
//		RotationOrRepair(RotationNode rNode) {
//			this.rNode = rNode;
//			this.repair = null;
//		}
//		
//		RotationOrRepair(AbstractRepairAction repair) {
//			this.rNode = null;
//			this.repair = repair;
//		}
//		
//		boolean hasRotation() {
//			return this.rNode != null;
//		}
//		
//		boolean hasRepair() {
//			return this.repair != null;
//		}
//		
//		public RotationNode getRotationNode() {
//			return rNode;
//		}
//		
//		public AbstractRepairAction getRepairAction() {
//			return repair;
//		}
//		
//		public boolean shouldIndent() {
//			if (this.hasRepair()) return true;
//			if (this.rNode.getParent() == null || this.rNode.getParent().equals(RotationNode.ROOTROTATION)) { 
//				return false;
//			}
//			if (rNode.isCollectionOrCombinationNode() || this.rNode.getParent().isCollectionOrCombinationNode()) {
//				return true;
//			}
//			return false;
//		}				
//	}
	
}
