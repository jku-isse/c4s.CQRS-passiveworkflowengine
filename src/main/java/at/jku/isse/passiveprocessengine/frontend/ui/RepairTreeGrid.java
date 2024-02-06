package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
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
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.SequenceRepairNode;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil;
import at.jku.isse.passiveprocessengine.frontend.ui.components.RepairVisualizationUtil.ReloadIconProvider;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairTreeGrid  extends TreeGrid<at.jku.isse.passiveprocessengine.frontend.ui.RepairTreeGrid.WrappedRepairNode> implements ReloadIconProvider{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int repairCount = 0;
	private RepairTreeFilter rtf;
	RequestDelegate reqDel;
	ProcessInstance scope;
	private RepairVisualizationUtil repairViz;
	//private UsageMonitor usageMonitor;
	
	public RepairTreeGrid(UsageMonitor monitor, RepairTreeFilter rtf, RequestDelegate reqDel) {
	//	this.usageMonitor = monitor;
		this.rtf = rtf;		
		this.reqDel = reqDel;
		this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		repairViz = new RepairVisualizationUtil(reqDel, this);		
	}
	
	public void initTreeGrid() {
        this.addComponentHierarchyColumn(o -> {
           if (o.getRepairNode() instanceof DummyRepairNode) {
        	   Span span = new Span("Insufficient Input.");
        	   return span;
           }else if (o.getRepairNode() instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o.getRepairNode();
                Span span = new Span(rebc.getSpec().getHumanReadableDescription());
                span.getElement().setProperty("title", rebc.getName());
                return span;
            } else if (o.getRepairNode() instanceof RepairNode) {
            	RepairNode rn = (RepairNode) o.getRepairNode();  	
            	HorizontalLayout hl = new HorizontalLayout();
            	//Span span = new Span();
            	repairViz.nodeToDescription(rn, scope).stream().forEach(comp -> hl.add(comp));
                hl.getElement().setProperty("title", rn.toString());
                return hl;
            } else {
                return new Paragraph(o.getClass().getSimpleName() );
            }
        })
        .setAutoWidth(true)        
        .setHeader("Execute any one of the following actions to fulfill constraint:"); //.setWidth("100%");
    }
	
	
	public Component getReloadIcon(Instance inst) {
		if (inst == null || !reqDel.getUIConfig().isGenerateRefetchButtonsPerArtifactEnabled()) return new Paragraph("");
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
	
//	public void updateQAConstraintTreeGrid(RepairNode rootNode) {
//		rtf.filterRepairTree(rootNode);
//		repairCount = rootNode.getRepairActions().size();
//		if (repairCount == 0) {
//			rootNode.getChildren().add(new DummyRepairNode(null));
//		} 
//		this.setItems(rootNode.getChildren().stream()
//				.map(x->x),
//				o -> {
//					if (o instanceof DummyRepairNode) {
//						return Stream.empty();
//					} else if (o instanceof RepairNode) { 
//						RepairNode rn = (RepairNode) o;
//						return rn.getChildren().stream().map(x -> (RepairNode)x);
//					} else {
//						log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
//						return Stream.empty();
//					}
//				});		
//		this.getDataProvider().refreshAll();
//	}
	
	public void updateConditionTreeGrid(RepairNode rootNode, ProcessInstance scope) {
		this.scope = scope;
		rtf.filterRepairTree(rootNode);
		repairCount = rootNode.getRepairActions().size();
		if (repairCount == 0 && rootNode.getChildren().stream().noneMatch(rn -> rn instanceof DummyRepairNode)) {
			rootNode.getChildren().add(new DummyRepairNode(null));
		} 
		//alphabetical Sort
		Comparator<RepairNode> comp = Comparator.comparing(RepairNode::toString,
				(sc1, sc2) -> sc2.compareTo(sc1)).reversed();
		List<RepairNode> childCollection = rootNode.getChildren().stream().map(x -> (RepairNode) x).sorted(comp)
				.collect(Collectors.toList());//till here
		this.setItems(childCollection.stream()
                .map(x->new WrappedRepairNode(x)),
        o -> {
        	if (o instanceof WrappedRepairNode) { 
            	WrappedRepairNode rn = (WrappedRepairNode) o; 
            	List<RepairNode> cc = rn.getRepairNode().getChildren().stream().map(x -> (RepairNode) x).sorted(comp)
        				.collect(Collectors.toList());
            	return cc.stream()
            			.map(x -> new WrappedRepairNode(x));
            } else {
                log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
                return Stream.empty();
            }
        });
		this.getDataProvider().refreshAll();		
	}
	
	private static java.util.Random random = new java.util.Random();
	
	private static class DummyRepairNode extends SequenceRepairNode {
		long rand;		
		public DummyRepairNode(RepairNode parent) {
			super(parent);
			rand = random.nextLong(); // to avoid duplicate grid entry error
		}
	}
	
	public static class WrappedRepairNode {
		RepairNode wrapped = null;
		public WrappedRepairNode(RepairNode wrapped) {
			assert (wrapped != null);
			this.wrapped = wrapped;
		}
		public RepairNode getRepairNode() {
			return wrapped;
		}
	}
}
