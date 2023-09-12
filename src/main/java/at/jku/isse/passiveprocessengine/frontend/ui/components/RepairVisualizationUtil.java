package at.jku.isse.passiveprocessengine.frontend.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.ui.RepairTreeGrid;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairVisualizationUtil {

	private RequestDelegate reqDel;
	private ReloadIconProvider iconProvider;
	
	public RepairVisualizationUtil(RequestDelegate reqDel, ReloadIconProvider iconProvider) {
		this.reqDel = reqDel;
		this.iconProvider = iconProvider;
	}
	
	
	public Collection<Component> nodeToDescription(RepairNode rn, ProcessInstance scope) {
		switch(rn.getNodeType()) {
		case ALTERNATIVE:
			return Set.of(new Paragraph("Do one of:"));
		case SEQUENCE:
			return Set.of(new Paragraph("Do all of:"));
		case MULTIVALUE: //fallthrough
		case VALUE:
			AbstractRepairAction ra = (AbstractRepairAction)rn;
			RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
			if (rootNode != null && reqDel.doShowRestrictions(scope)) {
				try {
					String restriction = rootNode.printTree(false,40);
					String restriction1=rootNode.toTreeString(40);
					restriction=restriction.replaceAll("(?m)^[ \t]*\r?\n", "");
					//System.out.println(restriction);
					//System.out.println(restriction1);
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
	
	
	
	private Collection<Component> generateRestrictedRepair(AbstractRepairAction ra, String restriction) {
		
		Component target = ra.getElement() != null ? new Paragraph(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Paragraph("");	
		Component reload = iconProvider.getReloadIcon((Instance)ra.getElement());
		switch(ra.getOperator()) {
		case ADD:
			List<Component> list = new ArrayList<>();			 
			list.add(new Paragraph(String.format("Add to %s of ", ra.getProperty())));
			list.add(target);		
			list.add(reload);
			Paragraph pAdd = new Paragraph(restriction);
			pAdd.getStyle().set("white-space", "pre-line");
			list.add(pAdd);
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
			Paragraph pSet = new Paragraph(restriction);
			pSet.getStyle().set("white-space", "pre-line");
			list3.add(pSet);
			return list3;
		case REMOVE:
			List<Component> list2 = new ArrayList<>();			
			list2.add(new Paragraph(String.format("Remove from %s of ", ra.getProperty())));
			list2.add(target);
			list2.add(reload);
			Paragraph pDel = new Paragraph(restriction);
			pDel.getStyle().set("white-space", "pre-line");
			list2.add(pDel);

			return list2;
		default:
			break;		
		}
		return Collections.emptyList();
	}
	
	
	private Collection<Component> generatePlainRepairs(AbstractRepairAction ra) {
		Component target = ra.getElement() != null ? new Paragraph(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Paragraph("");
		Component reload = iconProvider.getReloadIcon((Instance)ra.getElement());
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
			if (ra.getValue()!=null) {
				if (ra.getValue()==UnknownRepairValue.UNKNOWN) {
					Instance subject = (Instance) ra.getElement();
					if (subject.hasProperty(ra.getProperty())) {
						PropertyType propT = subject.getProperty(ra.getProperty()).propertyType();
						String propType = propT.referencedInstanceType().name();
						return List.of(new Paragraph("some suitable "+propType));
					} else {
						return List.of(new Paragraph("something suitable"));
					}
				} else
					return List.of(new Paragraph(ra.getValue().toString()));
			} else {
				return List.of(new Paragraph("null"));
			}
		}
	}
	
	public static boolean isSimpleRepairValue(AbstractRepairAction ra) {
		if (ra.getValue()!=null && !(ra.getValue() instanceof Instance) && !(ra.getValue() instanceof InstanceType))
			return true;
		else
			return false;
	}	
	
	
	public static interface ReloadIconProvider {
		public Component getReloadIcon(Instance inst);
	}
}
