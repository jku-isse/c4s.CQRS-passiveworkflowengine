package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import impactassessment.model.WorkflowInstanceWrapper;
import impactassessment.model.definition.QACheckDocument;
import impactassessment.model.definition.RuleEngineBasedConstraint;
import impactassessment.model.workflowmodel.IdentifiableObject;
import impactassessment.model.workflowmodel.WorkflowInstance;
import impactassessment.model.workflowmodel.WorkflowTask;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class WorkflowTreeGrid extends TreeGrid<IdentifiableObject> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    public void initTreeGrid() {

        this.addHierarchyColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getConstraintType();
            } else {
                return o.getClass().getSimpleName() + " - " + o.getId();
            }
        }).setHeader("Workflow Instance").setWidth("40%");


        this.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                try {
                    return formatter.format(rebc.getLastEvaluated());
                } catch (DateTimeException e) {
                    return "invalid time";
                }
            } else {
                return "";
            }
        }).setHeader("Last Evaluated");


        this.addColumn(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                try {
                    return formatter.format(rebc.getLastChanged());
                } catch (DateTimeException e) {
                    return "invalid time";
                }
            } else {
                return "";
            }
        }).setHeader("Last Changed");


        this.addColumn(new ComponentRenderer<Component, IdentifiableObject>(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                Div div= new Div();
                rebc.getFulfilledForReadOnly().stream()
                        .map(rl -> new Anchor(rl.getHref(), rl.getTitle()))
                        .forEach(anchor -> {
                            anchor.setTarget("_blank");
                            div.addComponentAsFirst(anchor);
                            div.addComponentAsFirst(new Label(" "));
                        });
                return div;
            } else {
                return new Label("");
            }
        })).setHeader("Fulfilled").setClassNameGenerator(item -> {
            if (item instanceof RuleEngineBasedConstraint && !((RuleEngineBasedConstraint)item).getFulfilledForReadOnly().isEmpty()) {
                return "success";
            }
            return "";
        });


        this.addColumn(new ComponentRenderer<Component, IdentifiableObject>(o -> {
            if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                Div div= new Div();
                rebc.getUnsatisfiedForReadOnly().stream()
                        .map(rl -> new Anchor(rl.getHref(), rl.getTitle()))
                        .forEach(anchor -> {
                            anchor.setTarget("_blank");
                            div.addComponentAsFirst(anchor);
                            div.addComponentAsFirst(new Label(" "));
                        });
                return div;
            } else {
                return new Label("");
            }
        })).setHeader("Unsatisfied").setClassNameGenerator(item -> {
            if (item instanceof RuleEngineBasedConstraint && !((RuleEngineBasedConstraint)item).getUnsatisfiedForReadOnly().isEmpty()) {
                return "error";
            }
            return "";
        });
    }

    public void updateTreeGrid(List<WorkflowInstanceWrapper> content) {
        if (content != null) {
            this.setItems(content.stream().map(WorkflowInstanceWrapper::getWorkflowInstance), o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    return wfi.getWorkflowTasksReadonly().stream().map(wft -> (IdentifiableObject) wft);
                } else if (o instanceof WorkflowTask) {
                    WorkflowTask wft = (WorkflowTask) o;
                    return wft.getOutput().stream().filter(ao -> ao.getArtifact() instanceof QACheckDocument).map(x -> (IdentifiableObject) x.getArtifact());
                } else if (o instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) o;
                    return qacd.getConstraintsReadonly().stream().map(x -> (IdentifiableObject) x);
                } else if (o instanceof RuleEngineBasedConstraint) {
                    return Stream.empty();
                } else {
                    log.error("TreeGridPanel got unknown artifact: " + o.getClass().getSimpleName());
                    return Stream.empty();
                }
            });
            this.getDataProvider().refreshAll();
        }
    }
}
