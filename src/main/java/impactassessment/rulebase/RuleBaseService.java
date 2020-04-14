package impactassessment.rulebase;

import impactassessment.model.workflowmodel.WorkflowInstance;
import impactassessment.model.definition.ConstraintTrigger;
import impactassessment.model.definition.QACheckDocument;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.kie.api.runtime.KieSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class RuleBaseService {

    private final CommandGateway commandGateway;
    private KieSession kieSession;

    public RuleBaseService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        kieSession = new RuleBaseFactory().getKieSession();
        kieSession.setGlobal("commandGateway", this.commandGateway);
    }

    public void insertAndFire(WorkflowInstance wfi) {
        kieSession.insert(wfi);
        kieSession.fireAllRules();
    }

    public void insertAndFire(ConstraintTrigger ct) {
        kieSession.insert(ct);
        kieSession.fireAllRules();
    }

    public void insertAndFire(QACheckDocument qacd) {
        kieSession.insert(qacd);
        kieSession.fireAllRules();
    }

    public void insertAndFire(QACheckDocument.QAConstraint qacd) {
        kieSession.insert(qacd);
        kieSession.fireAllRules();
    }

}
