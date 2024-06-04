package at.jku.isse.passiveprocessengine.rest;

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.gson.Gson;
import at.jku.isse.designspace.artifactconnector.core.artifactapi.ArtifactIdentifier;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.frontend.RequestDelegate;
import at.jku.isse.passiveprocessengine.frontend.artifacts.ArtifactResolver;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*") // to allow incoming calls from other ports
public class RepairsEndpoint {
//	static final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

	@Autowired
	RequestDelegate service;

	@Autowired
	RequestDelegate reqDelegate;

	@Autowired
	ArtifactResolver artRes;

//	@SuppressWarnings("unchecked")
	@GetMapping(value = "/get_repairs/{issue_key}", produces = "application/json")
	public ResponseEntity<String> getRepairTree(@PathVariable("issue_key") String issueKey) throws ProcessException {
		// get issue internal ID
		ArtifactIdentifier artID = new ArtifactIdentifier(issueKey, "jira_core_artifact",
				IJiraService.JiraIdentifier.JiraIssueKey.toString());
		Instance issue = artRes.get(artID);
		// server baseUrl
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

		// instantiate the result RepairsResponseObject to return repairs
		RepairsResponseObject rro = new RepairsResponseObject();
		rro.issueID = issue.id().toString();
		rro.issueKey = artID.getId();
		rro.issueLink = baseUrl + "/instance/" + rro.issueID;

		// get rules and filter out the ones that are not satisfied and add the rest to the response object
		issue.getProperties().stream().filter(property -> property.getName().contains("ruleScopes"))
				.forEach(property -> {
					((Collection<ConsistencyRule>) property.getValue()).forEach(x -> {
						if (x.getPropertyAsValue("result").toString() != "true")
							rro.rules.add(new RuleResponseObject(x, baseUrl));

					});
				});

		// convert the response object to JSON and return it
		Gson gson = new Gson();
		String json = gson.toJson(rro);
		return ResponseEntity.status(HttpStatus.OK).body(json);
	}
}

class SimpleJiraInstance {
	long id;
	String name;
	String key;

	public SimpleJiraInstance(Instance instance) {
		id = instance.id().value();
		name = instance.name();
		key = instance.getProperty("key").getValue().toString();
	}

}

// class to hold the response object. Hierachy is as follows:
// RepairsResponseObject -> RuleResponseObject[] -> RepairResponseObject
class RepairsResponseObject {
	String issueID;
	String issueKey;
	String issueLink;
	public ArrayList<RuleResponseObject> rules;

	public RepairsResponseObject() {
		rules = new ArrayList<RuleResponseObject>();
	}

}

// class to hold the repair response object. Hierachy is as follows:
// RepairResponseObject -> NonAtomicRepairResponseObject -> RepairResponseObject[]
// OR
// RepairResponseObject -> AtomicRepairResponseObject -> SimpleJiraInstance
class RepairResponseObject {
	String type; // direct , +, *,

	public static RepairResponseObject createRepairResponseObject(RepairNode repairNode) {
		// if the repair node is an atomic repair, return it right away
		if (repairNode instanceof AbstractRepairAction) {
			return new AtomicRepairResponseObject((AbstractRepairAction) repairNode);
		} else {
			// if the repair node has only one child, return the child right away
			if(repairNode.getChildren().size()==1)
				return createRepairResponseObject(repairNode.getChildren().getFirst());
			// if the repair node is a non atomic repair, return a NonAtomicRepairResponseObject
			return new NonAtomicRepairResponseObject(repairNode);
		}
	}

}

// class to hold the atomic repair response object
class AtomicRepairResponseObject extends RepairResponseObject {
	SimpleJiraInstance element; // the element to apply the repair on
	String property;
	String operator;
	Object value;
	boolean isConcrete;

	protected AtomicRepairResponseObject(AbstractRepairAction repairAction) {
		super();
		type = "atomic";
		element = new SimpleJiraInstance((Instance) repairAction.getElement());
		operator = repairAction.getOperator().name();
		property = repairAction.getProperty();

		// check if a restriction (= non concrete) repair
		RestrictionNode restrictionNode = repairAction.getValue() == UnknownRepairValue.UNKNOWN
				&& repairAction.getRepairValueOption().getRestriction() != null
						? repairAction.getRepairValueOption().getRestriction().getRootNode()
						: null;

		isConcrete = restrictionNode == null;
		value = !isConcrete ? restrictionNode.printNodeTree(false, 40) 		// if the repair is not concrete, return the restriction tree
				: (repairAction.getValue().getClass() == Instance.class)    // if the repair is concrete, return the value
						? new SimpleJiraInstance((Instance) repairAction.getValue()) 		// if the repair is an instance, return the instance
						: repairAction.getValue().toString();
	}
}

// class to hold the non atomic repair response object
class NonAtomicRepairResponseObject extends RepairResponseObject {
	ArrayList<RepairResponseObject> repairOptions;

	protected NonAtomicRepairResponseObject(RepairNode repairNode) {
		super();
		type = repairNode.toString(); // + or *
		repairOptions = new ArrayList<RepairResponseObject>();
		for (RepairNode child : repairNode.getChildren()) {
			repairOptions.add(RepairResponseObject.createRepairResponseObject(child));
		}
	}
}

// class to hold the rule response object
class RuleResponseObject {
	String ruleID;
	String ruleName;
	String ruleLink;
	String humanReadableDescription;
	RepairResponseObject repair;
	String contextStepName;
	String contextStepLink;
	String dashboardViewLink;

	public RuleResponseObject(ConsistencyRule consistencyRule, String baseUrl) {
		ruleID = consistencyRule.id().toString();
		ruleName = consistencyRule.name();
		ruleLink = baseUrl + "/instance/" + consistencyRule.id();
		Instance contextIntstance = consistencyRule.getPropertyAsInstance("contextInstance");
		ProcessStep processStepInstance = WrapperCache.getWrappedInstance(ProcessStep.class, contextIntstance);
		contextStepName = processStepInstance.getDefinition().getName();
		contextStepLink = baseUrl + "/instance/" + contextIntstance.id();
		
		String constraintType = ruleName.split("_")[1].toLowerCase();
		constraintType = constraintType.contains("precondition")? "preconditions" : constraintType.contains("postcondition")? "postconditions":"qaState";
		Instance constraintWrapper = (Instance)contextIntstance.getPropertyAsMap(constraintType).get(ruleName);
		humanReadableDescription = constraintWrapper.getPropertyAsInstance("qaSpec").getPropertyAsValue("humanReadableDescription").toString();
		// if the constraint is a qaState, the dashboard view link should be the qaState itself otherwise it should be the context instance
		if (constraintType=="qaState")
			dashboardViewLink = baseUrl + "/?focus=" + constraintWrapper.id();
		else
			dashboardViewLink = baseUrl + "/?focus=" + contextIntstance.id();
		
		// start building the repair tree
		RepairNode rnode = RuleService.repairTree(consistencyRule);
		repair = RepairResponseObject.createRepairResponseObject(rnode);
	}
}
