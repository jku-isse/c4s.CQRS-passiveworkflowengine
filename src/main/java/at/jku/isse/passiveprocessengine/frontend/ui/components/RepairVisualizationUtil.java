package at.jku.isse.passiveprocessengine.frontend.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
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
			return Set.of(new Span("Do one of:"));
		case SEQUENCE:
			return Set.of(new Span("Do all of:"));
		case MULTIVALUE: //fallthrough
		case VALUE:
			AbstractRepairAction ra = (AbstractRepairAction)rn;
			RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
			if (rootNode != null && reqDel.doShowRestrictions(scope)) {
				try {
					String restriction = rootNode.printNodeTree(false,2);
//					String restriction1=rootNode.toTreeString(10);
					restriction=restriction.replaceAll("(?m)^[ \t]*\r?\n", "");
					restriction=restriction.replaceAll("~", " ");
//					System.out.println(restriction);
//					System.out.println(restriction1);
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
			return List.of(new Span(rn.toString()));
		}
	}
	
	
	
	private Collection<Component> generateRestrictedRepair(AbstractRepairAction ra, String restriction) {
		
		Component target = ra.getElement() != null ? new Span(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Span("");	
		Component reload = iconProvider.getReloadIcon((Instance)ra.getElement());
		switch(ra.getOperator()) {
		case ADD:
			List<Component> list = new ArrayList<>();			 
			list.add(new Html(String.format("<span>Add to <b>%s</b> of </span>", ra.getProperty())));
			list.add(target);		
			list.add(reload);
			Span pAdd = new Span(restriction);
			pAdd.getStyle().set("white-space", "pre");
			list.add(pAdd);
			return list;
		case MOD_EQ:
		case MOD_GT:
		case MOD_LT:
		case MOD_NEQ:
			List<Component> list3 = new ArrayList<>();
			list3.add(new Html(String.format("<span>Set the <b>%s</b> of </span>", ra.getProperty())));
			list3.add(target);		
			list3.add(reload);
			list3.add(new Span(" to"));
			Span pSet = new Span(restriction);
			pSet.getStyle().set("white-space", "pre");
			list3.add(pSet);
			return list3;
		case REMOVE:
			List<Component> list2 = new ArrayList<>();			
			list2.add(new Html(String.format("<span>Remove from <b>%s</b> of </span>", ra.getProperty())));
			list2.add(target);
			list2.add(reload);
			Span pDel = new Span(restriction);
			pDel.getStyle().set("white-space", "pre");
			list2.add(pDel);

			return list2;
		default:
			break;		
		}
		return Collections.emptyList();
	}
	
	
	private Collection<Component> generatePlainRepairs(AbstractRepairAction ra) {
		Component target = ra.getElement() != null ? new Span(ComponentUtils.convertToResourceLinkWithBlankTarget((Instance)ra.getElement())) : new Span("");
		Component reload = iconProvider.getReloadIcon((Instance)ra.getElement());
		Collection<Component> change = object2String(ra);
		switch(ra.getOperator()) {
		case ADD:
			List<Component> list = new ArrayList<>();
			list.add(new Span("Add "));
			change.stream().forEach(comp -> list.add(comp)); 
			Html html=new Html(String.format("<span> to <b>%s</b> of </span>", ra.getProperty()));
			list.add(html);
			list.add(target);					
			list.add(reload);
			return list;
		case MOD_EQ:
		case MOD_GT:
		case MOD_LT:
		case MOD_NEQ:
			List<Component> list3 = new ArrayList<>();
			//list3.add(new Span(String.format("Change %s of ", ra.getProperty())));
			list3.add(new Html(String.format("<span>Change <b>%s</b> of </span>", ra.getProperty())));
			list3.add(target);
			list3.add(reload);
			if (isSimpleRepairValue(ra)) {
				list3.add(new Span(String.format(" to %s ", ra.getOperator().toString())));
				change.stream().forEach(comp -> list3.add(comp));
			}
			return list3;
			//return String.format("Change %s of %s to %s %s", ra.getProperty(), target, ra.getOperator().toString(), change);
		case REMOVE:
			List<Component> list2 = new ArrayList<>();
			if (ra.getProperty() != null)
			{
				list2.add(new Span("Remove "));
				change.stream().forEach(comp -> list2.add(comp)); 			
				list2.add(new Html(String.format("<span> from <b>%s</b> of </span>", ra.getProperty())));
				list2.add(target);
			} else {
				list2.add(new Span("Delete "));
				change.stream().forEach(comp -> list2.add(comp));				
			}
			list2.add(reload);
//			Entry<String,String> hint2 = generateHintForInOrOutProperty(ra.getProperty());
//			if (hint2 != null) {
//				Span pHint = new Span(hint2.getKey());
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
			return Set.of(new Span(( ComponentUtils.generateDisplayNameForInstance((Instance) ra.getValue()))));
		} else if (ra.getValue() instanceof InstanceType) {
			return List.of(new Span("a type of "), ComponentUtils.convertToResourceLinkWithBlankTarget((InstanceType) ra.getValue()));
		} else {
			if (ra.getValue()!=null) {
				if (ra.getValue()==UnknownRepairValue.UNKNOWN) {
					Instance subject = (Instance) ra.getElement();
					if (subject.hasProperty(ra.getProperty())) {
						PropertyType propT = subject.getProperty(ra.getProperty()).propertyType();
						String propType = propT.referencedInstanceType().name();
						return List.of(new Span("some suitable "+propType));
					} else {
						return List.of(new Span("something suitable"));
					}
				} else
					return List.of(new Span(ra.getValue().toString()));
			} else {
				return List.of(new Span("null"));
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
