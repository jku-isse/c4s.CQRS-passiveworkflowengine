package at.jku.isse.passiveprocessengine.frontend.ui;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.AlternativeRepairNode;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.SequenceRepairNode;
import at.jku.isse.designspace.rule.arl.repair.SingleValueRepairAction;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.components.ComponentUtils;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairTreeGrid extends TreeGrid<RepairNode>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int repairCount = 0;
	private RepairTreeFilter rtf;
	RequestDelegate reqDel;
	//private UsageMonitor usageMonitor;
	
	public RepairTreeGrid(UsageMonitor monitor, RepairTreeFilter rtf, RequestDelegate reqDel) {
	//	this.usageMonitor = monitor;
		this.rtf = rtf;		
		this.reqDel = reqDel;
		this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
	}
	
	public void initTreeGrid() {
        this.addComponentHierarchyColumn(o -> {
           if (o instanceof DummyRepairNode) {
        	   Span span = new Span("Insufficient Input or Output.");
        	   return span;
           }else if (o instanceof ConstraintWrapper) {
                ConstraintWrapper rebc = (ConstraintWrapper) o;
                Span span = new Span(rebc.getQaSpec().getHumanReadableDescription());
                span.getElement().setProperty("title", rebc.getName());
                return span;
            } else if (o instanceof RepairNode) {
            	RepairNode rn = (RepairNode) o;  	
            	HorizontalLayout hl = new HorizontalLayout();
            	//Span span = new Span();
            	nodeToDescription(rn).stream().forEach(comp -> hl.add(comp));
                hl.getElement().setProperty("title", rn.toString());
                return hl;
            } else {
                return new Paragraph(o.getClass().getSimpleName() );
            }
        })
        .setAutoWidth(true)
        .setHeader("Execute any one of the following actions to fulfill constraint:"); //.setWidth("100%");
    }
	
	public Collection<Component> nodeToDescription(RepairNode rn) {
		switch(rn.getNodeType()) {
		case ALTERNATIVE:
			return Set.of(new Paragraph("Do one of:"));
		case SEQUENCE:
			return Set.of(new Paragraph("Do all of:"));
		case MULTIVALUE: //fallthrough
		case VALUE:
			AbstractRepairAction ra = (AbstractRepairAction)rn;
			RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
			if (rootNode != null) {
				try {
					String restriction = rootNode.printNodeTree(false);
					return generateRestrictedRepair(ra, restriction);
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
					return generatePlainRepairs(ra);
				}
			} else {
				return generatePlainRepairs(ra);
			}	
		default:
			return List.of(new Paragraph(rn.toString()));
		}
	}
	
	private Component getReloadIcon(Instance inst) {
		if (inst == null && reqDel.getUIConfig().doGenerateRefetchButtonsPerArtifact()) return new Paragraph("");
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
	
	private Collection<Component> generateRestrictedRepair(AbstractRepairAction ra, String restriction) {
		
		Component target = ra.getElement() != null ? new Paragraph(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Paragraph("");	
		Component reload = getReloadIcon((Instance)ra.getElement());
		switch(ra.getOperator()) {
		case ADD:
			List<Component> list = new ArrayList<>();			 
			list.add(new Paragraph(String.format("Add to %s of ", ra.getProperty())));
			list.add(target);		
			list.add(reload);
			list.add(new Paragraph(restriction));
			return list;
		case MOD_EQ:
		case MOD_GT:
		case MOD_LT:
		case MOD_NEQ:
			List<Component> list3 = new ArrayList<>();
			list3.add(new Paragraph(String.format("Set the %s of ", ra.getProperty())));
			list3.add(target);		
			list3.add(reload);
			list3.add(new Paragraph(" to"));
			list3.add(new Paragraph(restriction));
			return list3;
		case REMOVE:
			List<Component> list2 = new ArrayList<>();			
			list2.add(new Paragraph(String.format("Remove from %s of ", ra.getProperty())));
			list2.add(target);
			list2.add(reload);
			list2.add(new Paragraph(restriction));
			return list2;
		default:
			break;		
		}
		return Collections.emptyList();
	}
	
	
	private Collection<Component> generatePlainRepairs(AbstractRepairAction ra) {
		Component target = ra.getElement() != null ? new Paragraph(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Paragraph("");
		Component reload = getReloadIcon((Instance)ra.getElement());
		Collection<Component> change = object2String(ra);
		switch(ra.getOperator()) {
		case ADD:
			List<Component> list = new ArrayList<>();
			list.add(new Paragraph("Add "));
			change.stream().forEach(comp -> list.add(comp)); 
			list.add(new Paragraph(String.format(" to %s of ", ra.getProperty())));
			list.add(target);					
			list.add(reload);
			return list;
		case MOD_EQ:
		case MOD_GT:
		case MOD_LT:
		case MOD_NEQ:
			List<Component> list3 = new ArrayList<>();
			list3.add(new Paragraph(String.format("Change %s of ", ra.getProperty())));
			list3.add(target);
			list3.add(reload);
			if (isSimpleRepairValue(ra)) {
				list3.add(new Paragraph(String.format(" to %s ", ra.getOperator().toString())));
				change.stream().forEach(comp -> list3.add(comp));
			}
			return list3;
			//return String.format("Change %s of %s to %s %s", ra.getProperty(), target, ra.getOperator().toString(), change);
		case REMOVE:
			List<Component> list2 = new ArrayList<>();
			if (ra.getProperty() != null)
			{
				list2.add(new Paragraph("Remove "));
				change.stream().forEach(comp -> list2.add(comp)); 			
				list2.add(new Paragraph(String.format(" from %s of ", ra.getProperty())));
				list2.add(target);
			} else {
				list2.add(new Paragraph("Delete "));
				change.stream().forEach(comp -> list2.add(comp));				
			}
			list2.add(reload);
//			Entry<String,String> hint2 = generateHintForInOrOutProperty(ra.getProperty());
//			if (hint2 != null) {
//				Paragraph pHint = new Paragraph(hint2.getKey());
//				pHint.setTitle(hint2.getValue());
//				list2.add(pHint);
//			}
			return list2;
			//return String.format("Remove %s from %s of %s",change, ra.getProperty(), target);
		default:
			break;		
		}
		return Collections.emptyList();
	}
		
	public static Collection<Component> object2String(AbstractRepairAction ra) {
		if (ra.getValue() instanceof Instance) {
			return Set.of(new Paragraph(( ComponentUtils.generateDisplayNameForInstance((Instance) ra.getValue()))));
		} else if (ra.getValue() instanceof InstanceType) {
			return List.of(new Paragraph("a type of "), ComponentUtils.convertToResourceLinkWithBlankTarget((InstanceType) ra.getValue()));
		} else {
			if (ra.getValue()!=null)
				return List.of(new Paragraph(ra.getValue().toString()));
			else {
				Instance subject = (Instance) ra.getElement();
				if (subject.hasProperty(ra.getProperty())) {
					PropertyType propT = subject.getProperty(ra.getProperty()).propertyType();
					String propType = propT.referencedInstanceType().name();
					return List.of(new Paragraph("some "+propType));
				} else {
					return List.of(new Paragraph("something"));
				}
			}
		}
	}
	
	public static boolean isSimpleRepairValue(AbstractRepairAction ra) {
		if (ra.getValue()!=null && !(ra.getValue() instanceof Instance) && !(ra.getValue() instanceof InstanceType))
			return true;
		else
			return false;
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
	
	public void updateConditionTreeGrid(RepairNode rootNode) {
		rtf.filterRepairTree(rootNode);
		repairCount = rootNode.getRepairActions().size();
		if (repairCount == 0) {
			rootNode.getChildren().add(new DummyRepairNode(null));
		} 
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
	
	
	private static class DummyRepairNode extends SequenceRepairNode {

		public DummyRepairNode(RepairNode parent) {
			super(parent);
		}
		
	}
}
