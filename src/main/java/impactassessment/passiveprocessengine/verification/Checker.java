package impactassessment.passiveprocessengine.verification;

import impactassessment.passiveprocessengine.workflowmodel.AbstractWorkflowDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class Checker {

    private Report report;
    private AbstractWorkflowDefinition workflow;

    public Report check(AbstractWorkflowDefinition workflow) {
        report = new Report();
        this.workflow = workflow;

        // check different aspects
        checkMapping();
        checkDecisionNodeOutBranch();

        return report;
    }

    private void checkMapping() {
        // TODO implement
    }

    private void checkDecisionNodeOutBranch() {
        List<String> unconnectedDndIds = workflow.getDecisionNodeDefinitions().stream()
                .filter(dnd -> dnd.getOutBranches().size() == 0)
                .map(dnd -> dnd.getId())
                .collect(Collectors.toList());
        for (String id : unconnectedDndIds) {
            report.addWarning(new Warning("DecisionNodeDefinition should have at least oneout-branch!", id));
        }
    }
}
