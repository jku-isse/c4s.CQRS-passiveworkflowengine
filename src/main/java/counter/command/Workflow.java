package counter.command;

import counter.api.*;
import counter.workflowmodel.AbstractWorkflowInstanceObject;
import counter.workflowmodel.TaskStateTransitionEvent;
import counter.workflowmodel.TaskStateTransitionEventPublisher;
import counter.workflowmodel.WorkflowInstance;
import counter.workflowmodel.definition.WPManagementWorkflow;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import counter.rulebase.RuleEvaluation;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Profile("command")
@XSlf4j
public class Workflow {

    @AggregateIdentifier
    private String id;
    private WorkflowInstance wfi;
    private List<AbstractWorkflowInstanceObject> awos;

    public Workflow() {
        log.debug("empty constructor invoked");
    }


    // Command Handlers

    @CommandHandler
    public Workflow(CreateWorkflowCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CreatedWorkflowEvt(cmd.getId()));
    }

    @CommandHandler
    public void handle(EnableCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new EnabledEvt(cmd.getId()));
    }


    // Event Handlers

    @EventSourcingHandler
    public void on(CreatedWorkflowEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getId();
        WPManagementWorkflow workflow = new WPManagementWorkflow();
        workflow.initWorkflowSpecification();
        workflow.setTaskStateTransitionEventPublisher(event -> {/*No Op*/});
        wfi = workflow.createInstance(id);
    }

    @EventSourcingHandler
    public void on(EnabledEvt evt) {
        log.debug("applying {}", evt);
        awos = wfi.enableWorkflowTasksAndDecisionNodes();
    }

}
