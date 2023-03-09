package at.jku.isse.passiveprocessengine.toolbridges.jiraToJama;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import at.jku.isse.designspace.artifactconnector.core.IService;
import at.jku.isse.designspace.core.events.ElementCreate;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyCreate;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ReservedNames;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.core.service.ServiceRegistry;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.jama.service.IJamaService;
import at.jku.isse.designspace.jama.service.IJamaService.JamaIdentifiers;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.designspace.jira.service.IJiraService.JiraIdentifier;

@Service
@DependsOn({"controleventengine"})
@ConditionalOnExpression(value = "${jama.enabled:false} and ${jira.enabled:false}") 
public class JiraJamaItemAugmentor implements WorkspaceListener, IService {

	@Autowired
	IJamaService jamaService;
	
	@Autowired
	IJiraService jiraService;
	
//	@Autowired 
//	WorkspaceService workspaceService;
	
	public static final String JIRA2JAMALINKPROPERTYNAME = "jamaItem";
	public static final String JAMA2JIRALINKPROPERTYNAME = "jiraIssue";
	public static final String JIRA2JAMAIDPROPERTYNAME = "jamaId";
	public static final String JAMA2JIRAIDPROPERTYNAME = "jiraKey";
	
	private Workspace ws;
	
	private InstanceType jiraBaseType; 
	private InstanceType jamaBaseType; 
	
	private boolean isSchemaUpdated = false;	
	
	public JiraJamaItemAugmentor() {		
		ServiceRegistry.registerService("JiraJamaBridge", "1.0.0", 90, () -> this.initialize(), true);		
	}
		
	public void initialize() {
		this.ws = WorkspaceService.PUBLIC_WORKSPACE;
		ws.workspaceListeners.add(this);
		// after a reboot, the types with augmentation might already exist,
		// upon new boot, then wont exist. 
		InstanceType jamaBaseType = ws.debugInstanceTypeFindByName("jama_item");
		if (jamaBaseType == null) return;

		InstanceType parentType = ws.debugInstanceTypeFindByName("jira_core_artifact");
		if (parentType != null && !parentType.subTypes().isEmpty()) {
			InstanceType jiraConcreteType = parentType.subTypes().iterator().next();
			// check if has property
			if (jiraConcreteType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JIRA2JAMALINKPROPERTYNAME)) {		
				isSchemaUpdated = true;
			}
		}
	}
	
	@Override
	public void handleUpdated(List<Operation> arg0) {
	
		arg0.stream().forEach(op -> {
			if (op instanceof ElementCreate && !isSchemaUpdated) {
				handleElementCreate((ElementCreate) op);
			} else if (op instanceof PropertyUpdateSet && isSchemaUpdated) {
				handlePropertyUpdateSet((PropertyUpdateSet) op);
			}
		});				
	}

	private void handleElementCreate(ElementCreate op) {
		// we need to update the schema with additional property or type JamaItem, 
		// everything hardcoded for now
  	    // also this is done only at the beginning when hopefully no jira instance yet exist (should be save to assume a this code is called only upon property creation)		
		if (op.instanceTypeId().value()==2l) {
			InstanceType instType = (InstanceType)ws.findElement(op.elementId());
			// simpler approach
			if (instType.name().equalsIgnoreCase("jama_item")) {
				jamaBaseType = instType;
			} else if(instType.name().equalsIgnoreCase("jira_core_artifact")) {
				jiraBaseType = instType;
			} 
			if (jiraBaseType != null && jamaBaseType != null) {
				if (!jiraBaseType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JIRA2JAMALINKPROPERTYNAME)) {				
					// create cross link property
					WorkspaceService.createOpposablePropertyType(ws, jiraBaseType, JIRA2JAMALINKPROPERTYNAME, Cardinality.SINGLE, jamaBaseType, JAMA2JIRALINKPROPERTYNAME, Cardinality.SINGLE);		
					Workspace.logger.info("Jira2Jama Bridge: augmented Jira and Jama Subclasses ");
					ws.concludeTransaction();
				} 
				// and set flag to no longer check for property creations
				isSchemaUpdated = true;
				return;
			}
				
			
//			// look for property created events, within an instance type that has JiraBase as the parent
//			if (isSubclassOfJira(instType)) {
//				// obtain jama schema																				
//				InstanceType jamaBaseType = ws.debugInstanceTypeFindByName("jama_item");
//				if (jamaBaseType == null) {
//					Workspace.logger.info("Jira2Jama Bridge: could not augment Jira Subclass "+instType.name()+" because unable to resolve jama base type: jama_item yet");
//				} else {
//					// check if has cross link property, if not
//					if (!instType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JIRA2JAMALINKPROPERTYNAME)) {				
//						// create cross link property
//						WorkspaceService.createOpposablePropertyType(ws, instType, JIRA2JAMALINKPROPERTYNAME, Cardinality.SINGLE, jamaBaseType, JAMA2JIRALINKPROPERTYNAME, Cardinality.SINGLE);		
//						Workspace.logger.info("Jira2Jama Bridge: augmented Jira Subclass "+instType.name());
//						ws.concludeTransaction();
//					} 
//					// and set flag to no longer check for property creations
//					isSchemaUpdated = true;
//					return;
//				}
//			} else if (isSubclassOfJama(instType)) { // we also try inverse if only jira exists but not jama yet
//				// obtain jama schema																				
//				InstanceType jiraBaseType = ws.debugInstanceTypeFindByName("jira_core_artifact");
//				if (jiraBaseType == null) {
//					Workspace.logger.info("Jira2Jama Bridge: could not augment Jama Subclass "+instType.name()+" because unable to resolve jira base type: jira_core_artifact yet");
//				} else {
//					// check if has cross link property, if not
//					if (!instType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JAMA2JIRALINKPROPERTYNAME)) {				
//						// create cross link property
//						WorkspaceService.createOpposablePropertyType(ws, instType, JAMA2JIRALINKPROPERTYNAME, Cardinality.SINGLE, jiraBaseType, JIRA2JAMALINKPROPERTYNAME, Cardinality.SINGLE);		
//						Workspace.logger.info("Jira2Jama Bridge: augmented Jama Subclass "+instType.name());
//						ws.concludeTransaction();
//					} 
//					// and set flag to no longer check for property creations
//					isSchemaUpdated = true;
//					return;
//				}
//			}
		}	
	}	
	
	private boolean isSubclassOfJira(InstanceType instType) {
		return instType.getAllSuperTypes().stream()
		.map(superT -> superT.name())
		.anyMatch(superName -> superName.equalsIgnoreCase("jira_core_artifact"));
	}
	
	private boolean isSubclassOfJama(InstanceType instType) {
		return instType.getAllSuperTypes().stream()
		.map(superT -> superT.name())
		.anyMatch(superName -> superName.equalsIgnoreCase("jama_item"));
	}	
	
	private void handlePropertyUpdateSet(PropertyUpdateSet op) { // we only set from the jira side
		if (op.name().equalsIgnoreCase(JIRA2JAMAIDPROPERTYNAME)) {
			Instance inst = ws.findElement(op.elementId());
			if (inst.hasProperty(JIRA2JAMALINKPROPERTYNAME)) {
				setJiraToJamaCrossLink(inst, op.value());
			}
		} else if (op.name().equalsIgnoreCase(JAMA2JIRAIDPROPERTYNAME)) {
			Instance inst = ws.findElement(op.elementId());
			if (inst.hasProperty(JAMA2JIRALINKPROPERTYNAME)) {
				setJamaToJiraCrossLink(inst, op.value());
			}
		}
	}
	
	private void setJiraToJamaCrossLink(Instance jiraItem, Object jamaKey) {
		if (jamaKey == null || jamaKey.toString().length() == 0) {
			// remove
			jiraItem.getProperty(JIRA2JAMALINKPROPERTYNAME).set(null);
		} else {			
			// or resolve it to a jama item
			Optional<Instance> jamaInstOpt = jamaService.getJamaItem(jamaKey.toString(), JamaIdentifiers.JamaItemDocKey);
			if (jamaInstOpt.isPresent()) {
			// and set it to the link property
			try {
				jiraItem.getProperty(JIRA2JAMALINKPROPERTYNAME).set(jamaInstOpt.get());
			} catch (IllegalArgumentException ie) {
                Workspace.logger.debug("Jira2Jama Bridge: " + jamaKey + " could not be assigned to crosslink "+ie.getMessage());
            }
			} else {
				Workspace.logger.debug("Jira2Jama Bridge: " + jamaKey + " could not be resolved to a jama item");
			}
		}
	}
	
	private void setJamaToJiraCrossLink(Instance jamaItem, Object jiraKey) {
		if (jiraKey == null || jiraKey.toString().length() == 0) {
			// remove
			jamaItem.getProperty(JAMA2JIRALINKPROPERTYNAME).set(null);
		} else {			
			try {
				// or resolve it to a jira item
				Optional<Instance> jiraInstOpt = jiraService.getArtifact(jiraKey.toString(), JiraIdentifier.JiraIssueKey, false);
				if (jiraInstOpt.isPresent()) {
					// and set it to the link property
					try {
						jamaItem.getProperty(JAMA2JIRALINKPROPERTYNAME).set(jiraInstOpt.get());
					} catch (IllegalArgumentException ie) {
						Workspace.logger.debug("Jira2Jama Bridge: " + jiraKey + " could not be assigned to crosslink "+ie.getMessage());
					}
				} else {
					Workspace.logger.debug("Jira2Jama Bridge: " + jiraKey + " could not be resolved to a jira item");
				} 
			} catch (Exception e) {
				Workspace.logger.debug("Jira2Jama Bridge: " + jiraKey + " could not be resolved with error "+e.getMessage());
			}
		}
	}
}
