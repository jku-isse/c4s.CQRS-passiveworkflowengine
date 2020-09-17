package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import impactassessment.api.CheckAllConstraintsCmd;
import impactassessment.api.CheckConstraintCmd;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import impactassessment.passiveprocessengine.instance.*;
import impactassessment.passiveprocessengine.definition.AbstractIdentifiableObject;
import impactassessment.passiveprocessengine.definition.NoOpTaskDefinition;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@CssImport(value="./styles/grid-styles.css")
public class WorkflowTreeGrid extends TreeGrid<AbstractIdentifiableObject> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private Function<Object, Object> f;
    private boolean evalMode;


    public WorkflowTreeGrid(Function<Object, Object> f, boolean evalMode) {
        this.f = f;
        this.evalMode = evalMode;
    }

    public void initTreeGrid() {

        this.addHierarchyColumn(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                return wfi.getEntry(WorkflowInstanceWrapper.PROP_ISSUE_TYPE) + ": " + wfi.getId();
            } else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                return wft.getType().getId();
            } else if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                return rebc.getDescription();
            } else {
                return o.getClass().getSimpleName() + ": " + o.getId();
            }
        }).setHeader("Workflow Instance").setWidth("35%");

        if (evalMode) {
            this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    Icon icon = new Icon(VaadinIcon.ROTATE_LEFT);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckAllConstraintsCmd(wfi.getId()));
                        Notification.show("Evaluation of "+wfi.getId()+" requested");
                    });
                    return icon;
                } else if (o instanceof RuleEngineBasedConstraint) {
                    RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                    Icon icon = new Icon(VaadinIcon.ROTATE_LEFT);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckConstraintCmd(rebc.getWorkflow().getId(), rebc.getId()));
                        Notification.show("Evaluation of "+rebc.getId()+" requested");
                    });
                    return icon;
                } else {
                    return new Label("");
                }
            })).setWidth("5%").setFlexGrow(0);
        }

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


        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
                icon.setColor("green");
                boolean fulfilled = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                    .anyMatch(QACheckDocument.QAConstraint::isFulfilled))
                        );
                return fulfilled ? icon : new Label("");
            }
            else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
                icon.setColor("green");
                boolean fulfilled = wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(QACheckDocument.QAConstraint::isFulfilled));
                return fulfilled ? icon : new Label("");
            }
            else if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                Dialog dialog = new Dialog();
                VerticalLayout l = new VerticalLayout();
                l.add(new H3(rebc.getWorkflow().getId()));
                l.add(new Label("Constraint: "+rebc.getDescription()+" was fulfilled by:"));
                Button btn = new Button("Resource Link(s)", e -> dialog.open());
                btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                AtomicBoolean linkPresent = new AtomicBoolean(false);
                rebc.getFulfilledForReadOnly().stream()
                        .map(rl -> new Anchor(rl.getHref(), rl.getTitle()))
                        .forEach(anchor -> {
                            anchor.setTarget("_blank");
                            l.add(anchor);
                            linkPresent.set(true);
                        });

                dialog.add(l);
                return linkPresent.get() ? btn : new Label("");
            } else {
                return new Label("");
            }
        })).setHeader("Fulfilled").setClassNameGenerator(x -> "column-center");


        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                Icon icon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
                icon.setColor("red");
                boolean unsatisfied = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()))
                        );
                return unsatisfied ? icon : new Label("");
            } else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                Icon icon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
                icon.setColor("red");
                boolean unsatisfied = wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()));
                return unsatisfied ? icon : new Label("");
            } else if (o instanceof RuleEngineBasedConstraint) {
                RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                Dialog dialog = new Dialog();
                VerticalLayout l = new VerticalLayout();
                l.add(new H3(rebc.getWorkflow().getId()));
                l.add(new Label("Constraint: "+rebc.getDescription()+" was unsatisfied by:"));
                Button btn = new Button("Resource Link(s)", e -> dialog.open());
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                AtomicBoolean linkPresent = new AtomicBoolean(false);
                rebc.getUnsatisfiedForReadOnly().stream()
                        .map(rl -> new Anchor(rl.getHref(), rl.getTitle()))
                        .forEach(anchor -> {
                            anchor.setTarget("_blank");
                            l.add(anchor);
                            linkPresent.set(true);
                        });
                dialog.add(l);
                return linkPresent.get() ? btn : new Label("");
            } else {
                return new Label("");
            }
        })).setHeader("Unsatisfied").setClassNameGenerator(x -> "column-center");
    }

    public void updateTreeGrid(List<WorkflowInstanceWrapper> content) {
        if (content != null) {
            this.setItems(content.stream().map(WorkflowInstanceWrapper::getWorkflowInstance), o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    return wfi.getWorkflowTasksReadonly().stream()
                            .filter(wft -> !(wft.getType() instanceof NoOpTaskDefinition))
                            .map(wft -> (AbstractIdentifiableObject) wft);
                } else if (o instanceof WorkflowTask) {
                    WorkflowTask wft = (WorkflowTask) o;
                    Optional<QACheckDocument> qacd =  wft.getOutput().stream()
                            .map(ao -> ao.getArtifact())
                            .filter(io -> io instanceof QACheckDocument)
                            .map(io -> (QACheckDocument) io)
                            .findFirst();
                    if (qacd.isPresent()) {
                        return qacd.get().getConstraintsReadonly().stream().map(x -> (AbstractIdentifiableObject) x);
                    } else {
                        return Stream.empty();
                    }
                }/* else if (o instanceof QACheckDocument) {
                    QACheckDocument qacd = (QACheckDocument) o;
                    return qacd.getConstraintsReadonly().stream().map(x -> (IdentifiableObject) x);
                }*/ else if (o instanceof RuleEngineBasedConstraint) {
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
