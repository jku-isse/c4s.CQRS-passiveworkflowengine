package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.stream.Stream;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;

import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.AlternativeRepairNode;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.SequenceRepairNode;
import at.jku.isse.designspace.rule.arl.repair.SingleValueRepairAction;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairTreeGrid extends TreeGrid<RepairNode>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public void initTreeGrid() {

        // Column "Workflow Instance"
        this.addComponentHierarchyColumn(o -> {
            if (o instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o;
                Span span = new Span(rebc.getQaSpec().getHumanReadableDescription());
                span.getElement().setProperty("title", rebc.getName());
                return span;
            } else if (o instanceof RepairNode) {
            	RepairNode rn = (RepairNode) o;
            	Span span = new Span(nodeToDescription(rn));
                span.getElement().setProperty("title", rn.toString());
                return span;
            } else {
                return new Paragraph(o.getClass().getSimpleName() );
            }
        }).setHeader("Repair Tree: execute any of the following").setWidth("100%");
    }
	
	private String nodeToDescription(RepairNode rn) {
		switch(rn.getNodeType()) {
		case ALTERNATIVE:
			return "Execute one of:";
		case SEQUENCE:
			return "Execute all of:";
		case MULTIVALUE: //fallthrough
		case VALUE:
			AbstractRepairAction ra = (AbstractRepairAction)rn;
			String target = ra.getElement() != null ? ((Instance)ra.getElement()).name() : "";
			String change = object2String(ra);
			switch(ra.getOperator()) {
			case ADD:
				return String.format("Add %s to %s of %s",change, ra.getProperty(), target);
			case MOD_EQ:
			case MOD_GT:
			case MOD_LT:
			case MOD_NEQ:
				return String.format("Change %s of %s to %s %s", ra.getProperty(), target, ra.getOperator().toString(), change);
			case REMOVE:
				return String.format("Remove %s from %s of %s",change, ra.getProperty(), target);
			default:
				break;
			}
			break;
		default:
			return rn.toString();
		}
		return "";
	}
		
	private String object2String(AbstractRepairAction ra) {
		if (ra.getValue() instanceof Instance) {
			return ((Instance) ra.getValue()).name();
		} else if (ra.getValue() instanceof InstanceType) {
			return "a type of "+((InstanceType) ra.getValue()).name();
		} else
			return ra.getValue()!=null ? ra.getValue().toString() : "unknown";
	}
	
	public void updateTreeGrid(RepairNode rootNode) {
		rtf.filterRepairTree(rootNode);
		this.setItems(rootNode.getChildren().stream()
                .map(x->x),
        o -> {
            if (o instanceof RepairNode) { 
            	RepairNode rn = (RepairNode) o;
            	return rn.getChildren().stream().map(x -> (RepairNode)x);
            } else {
                log.error("TreeGridPanel got unexpected artifact: " + o.getClass().getSimpleName());
                return Stream.empty();
            }
        });
		this.getDataProvider().refreshAll();
	}
	
	private static RepairTreeFilter rtf = new ReducedRepairTreeFilter();
	
	private static class ReducedRepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction ra) {
			return ra.getProperty() != null 
					&& !ra.getProperty().startsWith("out_") // no change to input or output
					&& !ra.getProperty().startsWith("in_")
					&& !ra.getProperty().equalsIgnoreCase("name"); // typically used to describe key or id outside of designspace
		
		}
		
	}

}
