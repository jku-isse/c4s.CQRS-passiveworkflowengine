package impactassessment.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import impactassessment.api.Commands.*;
import impactassessment.passiveprocessengine.WorkflowInstanceWrapper;
import lombok.extern.slf4j.Slf4j;
import passiveprocessengine.definition.AbstractIdentifiableObject;
import passiveprocessengine.definition.ArtifactType;
import passiveprocessengine.definition.NoOpTaskDefinition;
import passiveprocessengine.instance.ArtifactIO;
import passiveprocessengine.instance.ArtifactInput;
import passiveprocessengine.instance.ArtifactOutput;
import passiveprocessengine.instance.QACheckDocument;
import passiveprocessengine.instance.ResourceLink;
import passiveprocessengine.instance.RuleEngineBasedConstraint;
import passiveprocessengine.instance.WorkflowInstance;
import passiveprocessengine.instance.WorkflowTask;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // Column "Workflow Instance"
        this.addHierarchyColumn(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                int i = wfi.getId().indexOf("WF-");
                String id = i < 0 ? wfi.getId() : wfi.getId().substring(0, i+2).concat("...").concat(wfi.getId().substring(wfi.getId().length()-5));
                return wfi.getType().getId() + " (" + id + ")";
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

        // Column "Info"

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                return infoDialog((WorkflowInstance)o);
            } else if (o instanceof WorkflowTask) {
                return infoDialog((WorkflowTask)o);
            } else {
                return new Label("");
            }
        })).setWidth("5%").setFlexGrow(0);

        // Column "Last Evaluated"

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

        // Column "Last Changed"

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

        // Column delete

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                Icon icon = new Icon(VaadinIcon.TRASH);
                icon.setColor("red");
                icon.getStyle().set("cursor", "pointer");
                icon.addClickListener(e -> {
                    f.apply(new DeleteCmd(wfi.getId()));
                });
                icon.getElement().setProperty("title", "Remove this workflow");
                return icon;
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);

        // Column "Reevaluate"

        if (evalMode) {
            this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
                if (o instanceof WorkflowInstance) {
                    WorkflowInstance wfi = (WorkflowInstance) o;
                    Icon icon = new Icon(VaadinIcon.REPLY_ALL);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckAllConstraintsCmd(wfi.getId()));
                        Notification.show("Evaluation of "+wfi.getId()+" requested");
                    });
                    icon.getElement().setProperty("title", "Request a explicit re-evaluation of all rules for this artifact..");
                    return icon;
                } else if (o instanceof RuleEngineBasedConstraint) {
                    RuleEngineBasedConstraint rebc = (RuleEngineBasedConstraint) o;
                    Icon icon = new Icon(VaadinIcon.REPLY);
                    icon.setColor("#1565C0");
                    icon.getStyle().set("cursor", "pointer");
                    icon.addClickListener(e -> {
                        f.apply(new CheckConstraintCmd(rebc.getWorkflow().getId(), rebc.getId()));
                        Notification.show("Evaluation of "+rebc.getId()+" requested");
                    });
                    icon.getElement().setProperty("title", "Request a explicit re-evaluation of this rule for this artifact..");
                    return icon;
                } else {
                    return new Label("");
                }
            })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);
        }

        // Column "Unsatisfied" or "Fulfilled"

        this.addColumn(new ComponentRenderer<Component, AbstractIdentifiableObject>(o -> {
            if (o instanceof WorkflowInstance) {
                WorkflowInstance wfi = (WorkflowInstance) o;
                boolean unsatisfied = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof passiveprocessengine.instance.QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()))
                        );
                boolean fulfilled = wfi.getWorkflowTasksReadonly().stream()
                        .anyMatch(wft -> wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(QACheckDocument.QAConstraint::isFulfilled))
                        );
                return getIcon(unsatisfied, fulfilled);
            } else if (o instanceof WorkflowTask) {
                WorkflowTask wft = (WorkflowTask) o;
                boolean unsatisfied = wft.getOutput().stream()
                                .map(ArtifactIO::getArtifact)
                                .filter(a -> a instanceof QACheckDocument)
                                .map(a -> (QACheckDocument) a)
                                .map(QACheckDocument::getConstraintsReadonly)
                                .anyMatch(a -> a.stream()
                                        .anyMatch(c -> !c.isFulfilled()));
                boolean fulfilled = wft.getOutput().stream()
                        .map(ArtifactIO::getArtifact)
                        .filter(a -> a instanceof QACheckDocument)
                        .map(a -> (QACheckDocument) a)
                        .map(QACheckDocument::getConstraintsReadonly)
                        .anyMatch(a -> a.stream()
                                .anyMatch(QACheckDocument.QAConstraint::isFulfilled));
                return getIcon(unsatisfied, fulfilled);
            } else if (o instanceof RuleEngineBasedConstraint) {
                return infoDialog((RuleEngineBasedConstraint)o);
            } else {
                return new Label("");
            }
        })).setClassNameGenerator(x -> "column-center").setWidth("5%").setFlexGrow(0);
    }

    private Icon getIcon(boolean unsatisfied, boolean fulfilled) {
        Icon icon;
        if (unsatisfied && fulfilled) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
            icon.getElement().setProperty("title", "This contains unsatisfied and fulfilled constraints");
        } else if (unsatisfied) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
            icon.getElement().setProperty("title", "This contains unsatisfied constraints");
        } else if (fulfilled){
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
            icon.getElement().setProperty("title", "This contains fulfilled constraints");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            icon.getElement().setProperty("title", "Constraints not evaluated");
        }
        return icon;
    }

    private Component infoDialog(WorkflowInstance wfi) {
        VerticalLayout l = new VerticalLayout();
        l.add(new H3(wfi.getId()));
        l.add(new H4("Properties"));
        for (Map.Entry<String, String> e : wfi.getPropertiesReadOnly()) {
            l.add(new Paragraph(e.getKey() + ": " + e.getValue()));
        }
        infoDialogInputOutput(l, wfi.getInput(), wfi.getOutput(), wfi.getType().getExpectedInput(), wfi.getType().getExpectedOutput());
        Dialog dialog = new Dialog();

        Icon icon = new Icon(VaadinIcon.INFO_CIRCLE);
        icon.setColor("#1565C0");
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this workflow instance");

        dialog.add(l);

        return icon;
    }

    private Component infoDialog(WorkflowTask wft) {
        VerticalLayout l = new VerticalLayout();
        l.add(new H3(wft.getId()));
        l.add(new H4("Properties"));
        if (wft.getLifecycleState() != null)
            l.add(new Paragraph(wft.getLifecycleState().name()));
        infoDialogInputOutput(l, wft.getInput(), wft.getOutput(), wft.getType().getExpectedInput(), wft.getType().getExpectedOutput());
        Dialog dialog = new Dialog();

        Icon icon = new Icon(VaadinIcon.INFO_CIRCLE_O);
        icon.setColor("#1565C0");
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show more information about this workflow task");

        dialog.add(l);

        return icon;
    }

    private void infoDialogInputOutput(VerticalLayout l, List<ArtifactInput> inputs, List<ArtifactOutput> outputs, Map<String, ArtifactType> expectedInput, Map<String, ArtifactType> expectedOutput) {
        l.add(new H4("Inputs"));
        VerticalLayout inLayout = new VerticalLayout();
        inLayout.setClassName("card-border");
        inLayout.add(new H5("Expected"));
        for (Map.Entry<String, ArtifactType> entry : expectedInput.entrySet()) {
            inLayout.add(new Paragraph(entry.getKey() + " (" + entry.getValue().getArtifactType() + ")"));
        }
        inLayout.add(new H5("Present"));
        for (ArtifactInput ai : inputs) {
            inLayout.add(new Paragraph(ai.getRole() + " (" + ai.getArtifactType().getArtifactType() + "): " + ai.getArtifact().getId()));
        }
        l.add(inLayout);

        l.add(new H4("Outputs"));
        VerticalLayout outLayout = new VerticalLayout();
        outLayout.setClassName("card-border");
        outLayout.add(new H5("Expected"));
        for (Map.Entry<String, ArtifactType> entry : expectedOutput.entrySet()) {
            outLayout.add(new Paragraph(entry.getKey() + " (" + entry.getValue().getArtifactType() + ")"));
        }
        outLayout.add(new H5("Present"));
        for (ArtifactOutput ao : outputs) {
            outLayout.add(new Paragraph(ao.getRole() + " (" + ao.getArtifactType().getArtifactType() + "): " + ao.getArtifact().getId()));
        }
        l.add(outLayout);
    }

    private Component infoDialog(RuleEngineBasedConstraint rebc) {
        VerticalLayout l = new VerticalLayout();
        l.add(new H3(rebc.getWorkflow().getId()));
        l.add(new H4(rebc.getDescription()));
        // Unsatisfied resources
        List<Anchor> unsatisfiedLinks = new ArrayList<>();
        for (ResourceLink rl : rebc.getUnsatisfiedForReadOnly()) {
            Anchor a = new Anchor(rl.getHref(), rl.getTitle());
            a.setTarget("_blank");
            unsatisfiedLinks.add(a);
        }
        if (unsatisfiedLinks.size() > 0) {
            l.add(new H5("Unsatisfied by:"));
            for (Anchor a : unsatisfiedLinks) {
                HorizontalLayout h = new HorizontalLayout();
                h.setWidthFull();
                h.setMargin(false);
                h.setPadding(false);
                Icon icon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
                icon.setColor("red");
                h.add(icon, a);
                l.add(h);
            }
        }
        // Fulfilled resources
        List<Anchor> fulfilledLinks = new ArrayList<>();
        for (ResourceLink rl : rebc.getFulfilledForReadOnly()) {
            Anchor a = new Anchor(rl.getHref(), rl.getTitle());
            a.setTarget("_blank");
            fulfilledLinks.add(a);
        }
        if (fulfilledLinks.size() > 0) {
            l.add(new H5("Fulfilled by:"));
            for (Anchor a : fulfilledLinks) {
                HorizontalLayout h = new HorizontalLayout();
                h.setWidthFull();
                h.setMargin(false);
                h.setPadding(false);
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
                icon.setColor("green");
                h.add(icon, a);
                l.add(h);
            }
        }

        Dialog dialog = new Dialog();
        dialog.add(l);

        Icon icon;
        if (fulfilledLinks.size() > 0 && unsatisfiedLinks.size() > 0) {
            icon = new Icon(VaadinIcon.WARNING);
            icon.setColor("#E24C00");
        } else if (fulfilledLinks.size() > 0) {
            icon = new Icon(VaadinIcon.CHECK_CIRCLE);
            icon.setColor("green");
        } else if (unsatisfiedLinks.size() > 0) {
            icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
            icon.setColor("red");
        } else {
            icon = new Icon(VaadinIcon.QUESTION_CIRCLE);
            icon.setColor("#1565C0");
            return icon;
        }
        icon.getStyle().set("cursor", "pointer");
        icon.addClickListener(e -> dialog.open());
        icon.getElement().setProperty("title", "Show all resources of this rule");

        return icon;
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
                            .map(ArtifactIO::getArtifact)
                            .filter(io -> io instanceof QACheckDocument)
                            .map(io -> (QACheckDocument) io)
                            .findFirst();
                    return qacd.map(qaCheckDocument -> qaCheckDocument.getConstraintsReadonly().stream()
                            .map(x -> (AbstractIdentifiableObject) x))
                            .orElseGet(Stream::empty);
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
